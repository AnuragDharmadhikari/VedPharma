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

        // Step 4 — Determine who is billed and which state to use for tax
        // VIA_STOCKIST → invoice goes to Stockist → tax based on stockist state
        // DIRECT → invoice goes to Chemist → tax based on chemist state
        Chemist chemist = order.getChemist();
        Stockist stockist = order.getStockist();

        BilledTo billedTo;
        String billingState;

        if (order.getFulfillmentType() == FulfillmentType.VIA_STOCKIST) {
            billedTo = BilledTo.STOCKIST;
            // Stockist is guaranteed non-null for VIA_STOCKIST orders
            billingState = stockist.getState();
        } else {
            billedTo = BilledTo.CHEMIST;
            // Direct sale — bill to chemist, use chemist state for tax
            billingState = chemist.getState();
        }

        // Step 5 — Determine CGST+SGST or IGST
        TaxType taxType = companyState.equalsIgnoreCase(billingState)
                ? TaxType.CGST_SGST
                : TaxType.IGST;

        // Step 6 — Generate sequential invoice number
        String invoiceNumber = generateInvoiceNumber();

        // Step 7 — Process each order item and calculate taxes
        List<InvoiceLineItem> lineItems = new ArrayList<>();
        BigDecimal totalSubtotal = BigDecimal.ZERO;
        BigDecimal totalDiscount = BigDecimal.ZERO;
        BigDecimal totalCgst = BigDecimal.ZERO;
        BigDecimal totalSgst = BigDecimal.ZERO;
        BigDecimal totalIgst = BigDecimal.ZERO;
        BigDecimal grandTotal = BigDecimal.ZERO;

        for (OrderItem orderItem : order.getOrderItems()) {

            // Gross = unitPrice × quantity
            BigDecimal grossAmount = orderItem.getUnitPrice()
                    .multiply(BigDecimal.valueOf(orderItem.getQuantity()))
                    .setScale(2, RoundingMode.HALF_UP);

            // Total effective discount = base discount + scheme discount.
            // schemeDiscountPct defaults to ZERO when no scheme applied — always safe.
            // PERCENTAGE_DISCOUNT schemes stack on top of base discount.
            BigDecimal totalDiscountPct = orderItem.getDiscountPct()
                    .add(orderItem.getSchemeDiscountPct());

            BigDecimal discountAmount = grossAmount
                    .multiply(totalDiscountPct)
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

            // Taxable amount — GST calculated on this
            BigDecimal taxableAmount = grossAmount
                    .subtract(discountAmount)
                    .setScale(2, RoundingMode.HALF_UP);

            // Numeric GST rate from enum e.g. GST_12 → 12
            BigDecimal gstRate = BigDecimal.valueOf(
                    orderItem.getProduct().getGstRate().getRate());

            BigDecimal cgstAmt = BigDecimal.ZERO;
            BigDecimal sgstAmt = BigDecimal.ZERO;
            BigDecimal igstAmt = BigDecimal.ZERO;

            if (taxType == TaxType.CGST_SGST) {
                // Intra-state: GST split equally — divide by 200
                cgstAmt = taxableAmount
                        .multiply(gstRate)
                        .divide(BigDecimal.valueOf(200), 2, RoundingMode.HALF_UP);
                sgstAmt = cgstAmt;
            } else {
                // Inter-state: full rate as IGST — divide by 100
                igstAmt = taxableAmount
                        .multiply(gstRate)
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            }

            // Line total = taxable + all taxes
            BigDecimal lineTotal = taxableAmount
                    .add(cgstAmt)
                    .add(sgstAmt)
                    .add(igstAmt)
                    .setScale(2, RoundingMode.HALF_UP);

            // Build line item — invoice linked after parent invoice is built
            InvoiceLineItem lineItem = InvoiceLineItem.builder()
                    .product(orderItem.getProduct())
                    .hsnCode(orderItem.getProduct().getHsnCode())
                    .quantity(orderItem.getQuantity())
                    .unitPrice(orderItem.getUnitPrice())
                    .discountPct(totalDiscountPct)
                    .taxableAmount(taxableAmount)
                    .cgstAmt(cgstAmt)
                    .sgstAmt(sgstAmt)
                    .igstAmt(igstAmt)
                    .lineTotal(lineTotal)
                    // Carry free units from order item so InventoryService
                    // deducts quantity + freeQuantity from stock.
                    // Zero when no QUANTITY_FREE scheme applied.
                    .freeQuantity(orderItem.getFreeQuantity())
                    .build();

            lineItems.add(lineItem);

            // Accumulate invoice totals
            totalSubtotal = totalSubtotal.add(taxableAmount);
            totalDiscount = totalDiscount.add(discountAmount);
            totalCgst = totalCgst.add(cgstAmt);
            totalSgst = totalSgst.add(sgstAmt);
            totalIgst = totalIgst.add(igstAmt);
            grandTotal = grandTotal.add(lineTotal);
        }

        // Step 8 — Build Invoice entity
        Invoice invoice = Invoice.builder()
                .order(order)
                .rep(order.getRep())
                .chemist(chemist)
                .stockist(stockist)
                .billedTo(billedTo)
                .invoiceNumber(invoiceNumber)
                .invoiceDate(LocalDate.now())
                .taxType(taxType)
                .subtotal(totalSubtotal.setScale(2, RoundingMode.HALF_UP))
                .totalDiscount(totalDiscount.setScale(2, RoundingMode.HALF_UP))
                .totalCgst(totalCgst.setScale(2, RoundingMode.HALF_UP))
                .totalSgst(totalSgst.setScale(2, RoundingMode.HALF_UP))
                .totalIgst(totalIgst.setScale(2, RoundingMode.HALF_UP))
                .grandTotal(grandTotal.setScale(2, RoundingMode.HALF_UP))
                .build();

        // Step 9 — Link line items to invoice
        lineItems.forEach(item -> item.setInvoice(invoice));
        invoice.getLineItems().addAll(lineItems);

        // Step 10 — Save and re-fetch for complete response
        Invoice saved = invoiceRepository.save(invoice);

        // Step 11 — Deduct stock for all line items in this invoice
        // FIFO by expiry — oldest expiring batch deducted first
        // If insufficient stock, this throws IllegalArgumentException
        // and the entire transaction rolls back — invoice is NOT saved
        inventoryService.deductStockForInvoice(saved);

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

    private String generateInvoiceNumber() {
        Long nextVal = invoiceRepository.getNextSequenceValue();
        return String.format("VED-%d-%06d", LocalDate.now().getYear(), nextVal);
    }
}