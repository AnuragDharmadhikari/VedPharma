package org.ved.crm.billing;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.ved.crm.common.exception.ResourceNotFoundException;
import org.ved.crm.order.Order;
import org.ved.crm.order.OrderItem;
import org.ved.crm.order.OrderRepository;
import org.ved.crm.order.OrderStatus;

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

    @Value("${vedpharm.company.state}")
    private String companyState;

    public List<InvoiceDto> getAllInvoices(){
        return invoiceRepository.findAllWithDetails()
                .stream()
                .map(invoiceMapper::toDto)
                .toList();
    }

    public InvoiceDto getInvoiceById(UUID id){
       return invoiceRepository.findByIdWithDetails(id)
                .map(invoiceMapper::toDto)
                .orElseThrow(()->new ResourceNotFoundException("Invoice","id",id));
    }

    @Transactional
    public InvoiceDto generateInvoice(UUID orderId){

        // Step 1 — Load order with all items and relationships
        Order order = orderRepository.findByIdWithDetails(orderId)
                .orElseThrow(()->new ResourceNotFoundException("Order","id",orderId));

        // Step 2 — Only CONFIRMED orders can be invoiced
        if(order.getStatus()!= OrderStatus.CONFIRMED){
            throw new IllegalArgumentException("Only CONFIRMED orders can be invoiced");
        }

        // Step 3 — One order can only ever produce one invoice
        if(invoiceRepository.existsByOrderId(orderId)){
            throw new IllegalArgumentException("Invoice already exists for this order");
        }

        // Step 4 — Compare states to determine tax type
        // Same state → CGST+SGST, Different state → IGST

        String stockistState = order.getStockist().getState();
        TaxType taxType = companyState.equalsIgnoreCase(stockistState)
                ?TaxType.CGST_SGST
                :TaxType.IGST;

        // Step 5 — Get next sequential invoice number from PostgreSQL SEQUENCE
        String invoiceNumber = generateInvoiceNumber();

        // Step 6 — Process each order item, calculate taxes, build line items
        List<InvoiceLineItem> lineItems = new ArrayList<>();
        BigDecimal totalSubtotal = BigDecimal.ZERO;
        BigDecimal totalDiscount = BigDecimal.ZERO;
        BigDecimal totalCgst = BigDecimal.ZERO;
        BigDecimal totalSgst = BigDecimal.ZERO;
        BigDecimal totalIgst = BigDecimal.ZERO;
        BigDecimal grandTotal = BigDecimal.ZERO;

        for (OrderItem orderItem : order.getOrderItems()){

            // Gross = unitPrice × quantity
            BigDecimal grossAmount = orderItem.getUnitPrice()
                    .multiply(BigDecimal.valueOf(orderItem.getQuantity()))
                    .setScale(2, RoundingMode.HALF_UP);

            // Discount in rupees = gross × discountPct / 100
            BigDecimal discountAmount = grossAmount
                    .multiply(orderItem.getDiscountPct())
                    .divide(BigDecimal.valueOf(100),2,RoundingMode.HALF_UP);

            // Taxable amount = gross - discount — GST is calculated on this
            BigDecimal taxableAmount = grossAmount
                    .subtract(discountAmount)
                    .setScale(2,RoundingMode.HALF_UP);

            // Get numeric GST rate from the enum e.g. GST_12 → 12
            BigDecimal gstRate = BigDecimal.valueOf(
                    orderItem.getProduct().getGstRate().getRate()
            );

            BigDecimal cgstAmt = BigDecimal.ZERO;
            BigDecimal sgstAmt = BigDecimal.ZERO;
            BigDecimal igstAmt = BigDecimal.ZERO;

            if (taxType == TaxType.CGST_SGST){
                // Intra-state: divide GST rate by 2 for each component
                // GST 12% → CGST 6% + SGST 6%
                // We divide by 200 instead of dividing rate by 2 then by 100
                cgstAmt = taxableAmount
                        .multiply(gstRate)
                        .divide(BigDecimal.valueOf(200),2,RoundingMode.HALF_UP);
                sgstAmt=cgstAmt;
            }else{
                // Inter-state: full rate as IGST
                // GST 12% → IGST 12%
                igstAmt = taxableAmount
                        .multiply(gstRate)
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            }

            // Line total = taxable + all taxes
            BigDecimal lineTotal = taxableAmount
                    .add(cgstAmt)
                    .add(sgstAmt)
                    .add(igstAmt)
                    .setScale(2,RoundingMode.HALF_UP);

            // Build line item — invoice field set to null for now
            // We link it to the parent invoice in Step 8 below
            InvoiceLineItem lineItem = InvoiceLineItem.builder()
                    .product(orderItem.getProduct())
                    .hsnCode(orderItem.getProduct().getHsnCode())
                    .quantity(orderItem.getQuantity())
                    .unitPrice(orderItem.getUnitPrice())
                    .discountPct(orderItem.getDiscountPct())
                    .taxableAmount(taxableAmount)
                    .cgstAmt(cgstAmt)
                    .sgstAmt(sgstAmt)
                    .igstAmt(igstAmt)
                    .lineTotal(lineTotal)
                    .build();

            lineItems.add(lineItem);

            totalSubtotal = totalSubtotal.add(taxableAmount);
            totalDiscount = totalDiscount.add(discountAmount);
            totalCgst = totalCgst.add(cgstAmt);
            totalSgst = totalSgst.add(sgstAmt);
            totalIgst = totalIgst.add(igstAmt);
            grandTotal = grandTotal.add(lineTotal);

        }
        // Step 7 — Build the Invoice entity with all computed totals
        Invoice invoice = Invoice.builder()
                .order(order)
                .rep(order.getRep())
                .invoiceNumber(invoiceNumber)
                .invoiceDate(LocalDate.now())
                .taxType(taxType)
                .subtotal(totalSubtotal.setScale(2,RoundingMode.HALF_UP))
                .totalDiscount(totalDiscount.setScale(2, RoundingMode.HALF_UP))
                .totalCgst(totalCgst.setScale(2, RoundingMode.HALF_UP))
                .totalSgst(totalSgst.setScale(2,RoundingMode.HALF_UP))
                .totalIgst(totalIgst.setScale(2, RoundingMode.HALF_UP))
                .grandTotal(grandTotal.setScale(2, RoundingMode.HALF_UP))
                .build();

        // Step 8 — Link each line item back to the parent invoice
        // This must happen after invoice is built to avoid circular reference

        lineItems.forEach(item->item.setInvoice(invoice));
        invoice.getLineItems().addAll(lineItems);

        // Step 9 — Save invoice, cascade saves all line items automatically
        // Re-fetch with JOIN FETCH so response has all relationships populated

        Invoice saved = invoiceRepository.save(invoice);
        return invoiceMapper.toDto(
                invoiceRepository.findByIdWithDetails(saved.getId()).orElseThrow()
        );


    }

    @Transactional
    public InvoiceDto updateInvoiceStatus(UUID id, InvoiceStatus newStatus){
        Invoice invoice = invoiceRepository.findByIdWithDetails(id)
                .orElseThrow(()->new ResourceNotFoundException("Invoice","id",id));
        invoice.setStatus(newStatus);
        invoiceRepository.save(invoice);
        return invoiceMapper.toDto(invoiceRepository.findByIdWithDetails(id).orElseThrow());
    }


    // Calls PostgreSQL SEQUENCE — atomic, gapless, legally compliant
    // Format: VED-2026-000001
    private String generateInvoiceNumber() {
        Long nextVal = invoiceRepository.getNextSequenceValue();
        return String.format("VED-%d-%06d", LocalDate.now().getYear(), nextVal);
    }
}
