package org.ved.crm.billing;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.ved.crm.audit.Audited;
import org.ved.crm.chemist.Chemist;
import org.ved.crm.common.exception.ResourceNotFoundException;
import org.ved.crm.inventory.InventoryService;
import org.ved.crm.order.Order;
import org.ved.crm.order.OrderItem;
import org.ved.crm.order.OrderRepository;
import org.ved.crm.order.OrderStatus;
import org.ved.crm.order.FulfillmentType;
import org.ved.crm.stockist.Stockist;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final OrderRepository orderRepository;
    private final InvoiceMapper invoiceMapper;
    private final InventoryService inventoryService;

    @Value("${vedpharm.company.state}")
    private String companyState;

    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER')")
    public List<InvoiceDto> getAllInvoices() {
        return invoiceRepository.findAllWithDetails()
                .stream()
                .map(invoiceMapper::toDto)
                .toList();
    }

    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'REP')")
    public InvoiceDto getInvoiceById(UUID id) {
        Invoice invoice = invoiceRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Invoice", "id", id));
        return invoiceMapper.toDto(invoice);
    }

    @Audited(action = "INVOICE_GENERATED", entityType = "Invoice")
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER')")
    @Transactional
    public InvoiceDto generateInvoice(UUID orderId) {

        // Step 1 — Load order with all relationships
        Order order = orderRepository.findByIdWithDetails(orderId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Order", "id", orderId));

        // Step 2 — Only CONFIRMED orders can be invoiced
        if (order.getStatus() != OrderStatus.CONFIRMED) {
            throw new IllegalArgumentException(
                    "Only CONFIRMED orders can be invoiced");
        }

        // Step 3 — One order can only produce one invoice
        if (invoiceRepository.existsByOrderId(orderId)) {
            throw new IllegalArgumentException(
                    "Invoice already exists for this order");
        }

        // Step 4 — Determine who is billed and billing state
        Chemist chemist = order.getChemist();
        Stockist stockist = order.getStockist();

        BilledTo billedTo;
        String billingState;

        if (order.getFulfillmentType() == FulfillmentType.VIA_STOCKIST) {
            billedTo = BilledTo.STOCKIST;
            billingState = stockist.getState();
        } else {
            billedTo = BilledTo.CHEMIST;
            billingState = chemist.getState();
        }

        // Step 5 — Determine CGST+SGST or IGST
        TaxType taxType = companyState.equalsIgnoreCase(billingState)
                ? TaxType.CGST_SGST
                : TaxType.IGST;

        // Step 6 — Generate sequential invoice number
        String invoiceNumber = generateInvoiceNumber();

        // Step 7 — Build Invoice with ZERO totals and empty line items
        // Totals will be recalculated after InventoryService creates
        // the batch-split line items
        Invoice invoice = Invoice.builder()
                .order(order)
                .rep(order.getRep())
                .chemist(chemist)
                .stockist(stockist)
                .billedTo(billedTo)
                .invoiceNumber(invoiceNumber)
                .invoiceDate(LocalDate.now())
                .taxType(taxType)
                .subtotal(BigDecimal.ZERO)
                .totalDiscount(BigDecimal.ZERO)
                .totalCgst(BigDecimal.ZERO)
                .totalSgst(BigDecimal.ZERO)
                .totalIgst(BigDecimal.ZERO)
                .grandTotal(BigDecimal.ZERO)
                .build();

        // Step 8 — Save invoice first to get an ID
        // InventoryService needs the invoice ID for StockMovement referenceId
        Invoice saved = invoiceRepository.save(invoice);

        // Step 9 — Deduct stock FIFO and create batch-split line items
        // Each batch used gets its own InvoiceLineItem with full GST calculation
        // This is the core of our batch traceability feature
        List<InvoiceLineItem> lineItems =
                inventoryService.deductStockAndCreateLineItems(
                        saved,
                        order.getOrderItems(),
                        taxType);

        // Step 10 — Link line items to invoice
        lineItems.forEach(item -> item.setInvoice(saved));
        saved.getLineItems().addAll(lineItems);

        // Step 11 — Recalculate invoice totals from actual line items
        // We do this here instead of in InventoryService to keep
        // financial calculations in the billing domain
        BigDecimal totalSubtotal  = BigDecimal.ZERO;
        BigDecimal totalDiscount  = BigDecimal.ZERO;
        BigDecimal totalCgst      = BigDecimal.ZERO;
        BigDecimal totalSgst      = BigDecimal.ZERO;
        BigDecimal totalIgst      = BigDecimal.ZERO;
        BigDecimal grandTotal     = BigDecimal.ZERO;

        for (InvoiceLineItem item : lineItems) {
            totalSubtotal = totalSubtotal.add(item.getTaxableAmount());
            totalCgst     = totalCgst.add(item.getCgstAmt());
            totalSgst     = totalSgst.add(item.getSgstAmt());
            totalIgst     = totalIgst.add(item.getIgstAmt());
            grandTotal    = grandTotal.add(item.getLineTotal());
        }

        // Recalculate total discount from order items
        // (discount was applied per order item, not per batch line item)
        for (org.ved.crm.order.OrderItem orderItem : order.getOrderItems()) {
            BigDecimal grossAmount = orderItem.getUnitPrice()
                    .multiply(BigDecimal.valueOf(orderItem.getQuantity()))
                    .setScale(2, RoundingMode.HALF_UP);
            BigDecimal totalDiscountPct = orderItem.getDiscountPct()
                    .add(orderItem.getSchemeDiscountPct());
            BigDecimal discountAmount = grossAmount
                    .multiply(totalDiscountPct)
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            totalDiscount = totalDiscount.add(discountAmount);
        }

        // Step 12 — Update invoice with calculated totals
        saved.setSubtotal(totalSubtotal.setScale(2, RoundingMode.HALF_UP));
        saved.setTotalDiscount(totalDiscount.setScale(2, RoundingMode.HALF_UP));
        saved.setTotalCgst(totalCgst.setScale(2, RoundingMode.HALF_UP));
        saved.setTotalSgst(totalSgst.setScale(2, RoundingMode.HALF_UP));
        saved.setTotalIgst(totalIgst.setScale(2, RoundingMode.HALF_UP));
        saved.setGrandTotal(grandTotal.setScale(2, RoundingMode.HALF_UP));

        // Step 13 — Save everything and re-fetch for complete response
        invoiceRepository.save(saved);

        return invoiceMapper.toDto(
                invoiceRepository.findByIdWithDetails(saved.getId()).orElseThrow()
        );
    }

    @PreAuthorize("hasRole('OWNER')")
    @Transactional
    public InvoiceDto updateInvoiceStatus(UUID id, InvoiceStatus newStatus) {
        Invoice invoice = invoiceRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Invoice", "id", id));
        invoice.setStatus(newStatus);
        invoiceRepository.save(invoice);
        return invoiceMapper.toDto(
                invoiceRepository.findByIdWithDetails(id).orElseThrow()
        );
    }

    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER')")
    public List<OutstandingInvoiceDto> getOutstandingInvoices() {
        return invoiceRepository.findOutstandingInvoices()
                .stream()
                .map(p -> new OutstandingInvoiceDto(
                        p.getInvoiceId(),
                        p.getInvoiceNumber(),
                        p.getBilledToName(),
                        p.getGrandTotal(),
                        p.getTotalPaid(),
                        p.getOutstandingAmount(),
                        p.getStatus(),
                        p.getChemistId(),
                        p.getStockistId()
                ))
                .toList();
    }

    private String generateInvoiceNumber() {
        Long nextVal = invoiceRepository.getNextSequenceValue();
        return String.format("VED-%d-%06d", LocalDate.now().getYear(), nextVal);
    }
}