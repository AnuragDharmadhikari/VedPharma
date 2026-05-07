package org.ved.crm.Payment;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.ved.crm.audit.Audited;
import org.ved.crm.billing.Invoice;
import org.ved.crm.billing.InvoiceRepository;
import org.ved.crm.billing.InvoiceStatus;
import org.ved.crm.chemist.Chemist;
import org.ved.crm.chemist.ChemistRepository;
import org.ved.crm.common.exception.ResourceNotFoundException;
import org.ved.crm.returns.CreditNoteRepository;
import org.ved.crm.stockist.Stockist;
import org.ved.crm.stockist.StockistRepository;
import org.springframework.security.access.prepost.PreAuthorize;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentAllocationRepository paymentAllocationRepository;
    private final InvoiceRepository invoiceRepository;
    private final StockistRepository stockistRepository;
    private final ChemistRepository chemistRepository;
    private final PaymentMapper paymentMapper;
    private final CreditNoteRepository creditNoteRepository;

    // GET all payments
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER')")
    public List<PaymentDto> getAllPayments(){
        return paymentRepository.findAllWithDetails()
                .stream()
                .map(paymentMapper::toDto)
                .toList();
    }

    // GET payment by ID
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER')")
    public PaymentDto getPaymentById(UUID id){
        Payment payment = paymentRepository.findByIdWithDetails(id)
                .orElseThrow(()->new ResourceNotFoundException("Payment","id",id));
        return paymentMapper.toDto(payment);
    }

    // GET payments by stockist
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER')")
    public List<PaymentDto> getPaymentsByStockist(UUID stockistId){
        return paymentRepository.findByStockistId(stockistId)
                .stream()
                .map(paymentMapper::toDto)
                .toList();
    }

    // GET payments by chemist
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER')")
    public List<PaymentDto> getPaymentsByChemist(UUID chemistId) {
        return paymentRepository.findByChemistId(chemistId)
                .stream()
                .map(paymentMapper::toDto)
                .toList();
    }

    // CREATE payment — the most complex method
    @Audited(action = "PAYMENT_RECORDED", entityType = "Payment")
    @PreAuthorize("hasRole('OWNER')")
    @Transactional
    public PaymentDto createPayment(CreatePaymentRequest request){

        // Step 1 — Validate payer XOR
        if(request.stockistId() == null && request.chemistId() == null){
            throw new IllegalArgumentException("Either stockistId or chemistId must be provided");
        }
        if(request.stockistId() != null && request.chemistId() != null){
            throw new IllegalArgumentException("Only one of stockistId or chemistId can be provided");
        }

        // Step 2 — Load payer entity
        Stockist stockist = null;
        Chemist chemist = null;

        if(request.stockistId() != null){
            stockist = stockistRepository.findByIdWithDetails(request.stockistId())
                    .orElseThrow(()->new ResourceNotFoundException(
                            "Stockist","id",request.stockistId()));
        } else {
            chemist = chemistRepository.findByIdWithDetails(request.chemistId())
                    .orElseThrow(()->new ResourceNotFoundException(
                            "Chemist","id",request.chemistId()));
        }

        // Step 3 — Validate total allocation equals payment amount
        BigDecimal totalAllocated = request.allocations().stream()
                .map(PaymentAllocationRequest::allocatedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if(totalAllocated.compareTo(request.amount()) != 0){
            throw new IllegalArgumentException(
                    "Total allocated amount (" + totalAllocated
                            + ") must equal payment amount (" + request.amount() + ")");
        }

        // Step 4 — Validate each invoice and build map for reuse in Steps 7 and 9.
        // Fetching once here and reusing eliminates redundant DB hits per allocation.
        Map<UUID, Invoice> invoiceMap = new HashMap<>();

        for(PaymentAllocationRequest allocationReq : request.allocations()){

            Invoice invoice = invoiceRepository.findByIdWithDetails(allocationReq.invoiceId())
                    .orElseThrow(()->new ResourceNotFoundException(
                            "Invoice","id",allocationReq.invoiceId()));

            // Store in map for reuse — keyed by invoice ID
            invoiceMap.put(allocationReq.invoiceId(), invoice);

            if(invoice.getStatus() == InvoiceStatus.DRAFT){
                throw new IllegalArgumentException(
                        "Cannot allocate payment to a DRAFT invoice: "
                                + invoice.getInvoiceNumber());
            }
            if(invoice.getStatus() == InvoiceStatus.PAID){
                throw new IllegalArgumentException(
                        "Invoice is already fully paid: "
                                + invoice.getInvoiceNumber());
            }

            BigDecimal alreadyAllocated = paymentAllocationRepository
                    .getTotalAllocatedForInvoice(allocationReq.invoiceId());

            BigDecimal alreadyCredited = creditNoteRepository
                    .getTotalCreditAppliedForInvoice(allocationReq.invoiceId());

            BigDecimal outstanding = invoice.getGrandTotal()
                    .subtract(alreadyAllocated)
                    .subtract(alreadyCredited);

            if(allocationReq.allocatedAmount().compareTo(outstanding) > 0){
                throw new IllegalArgumentException(
                        "Allocation amount (" + allocationReq.allocatedAmount()
                                + ") exceeds outstanding balance ("
                                + outstanding + ") for invoice: "
                                + invoice.getInvoiceNumber());
            }
        }

        // Step 5 — Generate sequential payment number
        String paymentNumber = generatePaymentNumber();

        // Step 6 — Build Payment entity
        Payment payment = Payment.builder()
                .stockist(stockist)
                .chemist(chemist)
                .paymentNumber(paymentNumber)
                .paymentDate(request.paymentDate())
                .amount(request.amount())
                .paymentMode(request.paymentMode())
                .referenceNumber(request.referenceNumber())
                .notes(request.notes())
                .build();

        // Step 7 — Build allocations using invoices from map — no extra DB fetch
        for(PaymentAllocationRequest allocationReq : request.allocations()){
            Invoice invoice = invoiceMap.get(allocationReq.invoiceId());

            PaymentAllocation allocation = PaymentAllocation.builder()
                    .payment(payment)
                    .invoice(invoice)
                    .allocatedAmount(allocationReq.allocatedAmount())
                    .build();

            payment.getAllocations().add(allocation);
        }

        // Step 8 — Save payment — cascade saves all allocations automatically
        Payment saved = paymentRepository.save(payment);

        // Step 9 — Update invoice statuses using invoices from map — no extra DB fetch.
        // Outstanding accounts for BOTH payments AND credit notes.
        for(PaymentAllocationRequest allocationReq : request.allocations()){
            Invoice invoice = invoiceMap.get(allocationReq.invoiceId());

            BigDecimal totalPaid = paymentAllocationRepository
                    .getTotalAllocatedForInvoice(allocationReq.invoiceId());

            BigDecimal totalCredited = creditNoteRepository
                    .getTotalCreditAppliedForInvoice(allocationReq.invoiceId());

            BigDecimal outstanding = invoice.getGrandTotal()
                    .subtract(totalPaid)
                    .subtract(totalCredited);

            if(outstanding.compareTo(BigDecimal.ZERO) <= 0){
                invoice.setStatus(InvoiceStatus.PAID);
            } else {
                invoice.setStatus(InvoiceStatus.PARTIALLY_PAID);
            }

            invoiceRepository.save(invoice);
        }

        // Step 10 — Re-fetch for complete response
        return paymentMapper.toDto(
                paymentRepository.findByIdWithDetails(saved.getId()).orElseThrow()
        );
    }

    // Generate sequential payment number
    private String generatePaymentNumber() {
        Long nextVal = paymentRepository.getNextSequenceValue();
        return String.format("PAY-%d-%06d", LocalDate.now().getYear(), nextVal);
    }

}
