package org.ved.crm.inventory;


import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.ved.crm.audit.Audited;
import org.ved.crm.billing.Invoice;
import org.ved.crm.billing.InvoiceLineItem;
import org.ved.crm.common.exception.ResourceNotFoundException;
import org.ved.crm.product.Product;
import org.ved.crm.product.ProductRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InventoryService {

    private final BatchRepository batchRepository;
    private final StockMovementRepository stockMovementRepository;
    private final ProductRepository productRepository;
    private final BatchMapper batchMapper;
    private final StockMovementMapper stockMovementMapper;

    // ─────────────────────────────────────────────
    // PUBLIC API METHODS — exposed via controller
    // ─────────────────────────────────────────────

    // GET all batches for a product
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'REP')")
    public List<BatchDto> getBatchesByProduct(UUID productId){
        return batchRepository.findAllBatchesByProduct(productId)
                .stream().map(batchMapper::toDto)
                .toList();
    }

    // GET batch by ID
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'REP')")
    public BatchDto getBatchById(UUID id){
        Batch batch = batchRepository.findByIdWithDetails(id)
                .orElseThrow(()->new ResourceNotFoundException("Batch","id",id));
        return batchMapper.toDto(batch);
    }

    // GET all stock movements for a batch — full audit trail
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'REP')")
    public List<StockMovementDto> getMovementsByBatch(UUID batchId){
        return stockMovementRepository.findByBatchId(batchId)
                .stream()
                .map(stockMovementMapper::toDto)
                .toList();
    }

    // GET near expiry batches — expiring within 90 days
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'REP')")
    public List<BatchDto> getNearExpiryBatches(){
        LocalDate today = LocalDate.now();
        LocalDate warningDate = today.plusDays(90);
        return batchRepository.findNearExpiryBatches(today,warningDate)
                .stream()
                .map(batchMapper::toDto)
                .toList();
    }

    // GET expired batches that still have stock — need writeoff
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'REP')")
    public List<BatchDto> getExpiredBatchesWithStock() {
        return batchRepository.findExpiredBatchesWithStock(LocalDate.now())
                .stream()
                .map(batchMapper::toDto)
                .toList();
    }

    // ADD new batch — stock received from manufacturer
    @PreAuthorize("hasRole('OWNER')")
    @Transactional
    public BatchDto addBatch(AddBatchRequest request){

        // Step 1 — Validate product exists
        Product product = productRepository.findById(request.productId())
                .orElseThrow(()->new ResourceNotFoundException("Product","id",request.productId()));

        if (!product.isActive()) {
            throw new IllegalArgumentException(
                    "Product is deactivated and cannot be added to batch: "
                            + product.getName());
        }

        // Step 2 — Check duplicate batch number for this product
        // Same batch number can exist for different products
        // but not for the same product twice
        if(batchRepository.existsByProductIdAndBatchNumber(request.productId(), request.batchNumber())){
            throw new IllegalArgumentException("Batch number already exists for this product: "+request.batchNumber());
        }

        // Step 3 — Build the batch entity
        // initialQuantity and currentQuantity start equal
        // As stock is sold/sampled, currentQuantity decreases
        // initialQuantity never changes — it's a permanent record
        // of how much arrived
        Batch batch = Batch.builder()
                .product(product)
                .batchNumber(request.batchNumber())
                .mfgDate(request.mfgDate())
                .expiryDate(request.expiryDate())
                .initialQuantity(request.quantity())
                .currentQuantity(request.quantity())
                .build();

        Batch saved = batchRepository.save(batch);

        // Step 4 — Record the INWARD stock movement
        // Every stock addition must have an audit trail entry
        // quantity is positive — stock coming IN
        StockMovement inwardMovement = StockMovement.builder()
                .batch(saved)
                .movementType(MovementType.INWARD)
                .quantity(request.quantity())
                .referenceType("MANUAL")
                .notes("Initial stock inward for batch: " + request.batchNumber())
                .build();

        stockMovementRepository.save(inwardMovement);

        return batchMapper.toDto(batchRepository.findByIdWithDetails(saved.getId()).orElseThrow());

    }

    // MANUAL stock adjustment — owner corrects discrepancies
    @Audited(action = "STOCK_ADJUSTED", entityType = "Batch")
    @PreAuthorize("hasRole('OWNER')")
    @Transactional
    public BatchDto adjustStock(UUID batchId, AdjustStockRequest request){

        // Step 1 — Load the batch
        Batch batch = batchRepository.findByIdWithDetails(batchId)
                .orElseThrow(()->new ResourceNotFoundException("Batch","id",batchId));

        // Step 2 — Calculate new quantity after adjustment
        int newQuantity = batch.getCurrentQuantity() + request.quantity();

        // Step 3 — Quantity can never go below zero
        // You cannot have negative physical stock

        if(newQuantity < 0){
            throw new IllegalArgumentException(
                    "Adjustment would result in negative stock. "
                            + "Current: " + batch.getCurrentQuantity()
                            + ", Adjustment: " + request.quantity());
        }

        // Step 4 — Apply the adjustment
        batch.setCurrentQuantity(newQuantity);
        batchRepository.save(batch);

        // Step 5 — Record the ADJUSTMENT movement for audit trail
        // quantity can be positive or negative depending on direction
        StockMovement adjustment = StockMovement.builder()
                .batch(batch)
                .movementType(MovementType.ADJUSTMENT)
                .quantity(request.quantity())
                .referenceType("MANUAL")
                .notes(request.reason())
                .build();

        stockMovementRepository.save(adjustment);

        return batchMapper.toDto(batchRepository.findByIdWithDetails(batchId).orElseThrow());

    }

    // WRITE OFF expired stock — marks expired batch as zero
    @PreAuthorize("hasRole('OWNER')")
    @Transactional
    public BatchDto writeOffExpiredBatch(UUID batchId){
        Batch batch  = batchRepository.findByIdWithDetails(batchId)
                .orElseThrow(()->new ResourceNotFoundException("Batch","id",batchId));

        // Can only write off actually expired batches
        if(!batch.isExpired()){
            throw new IllegalArgumentException(
                    "Batch is not expired yet. Expiry date: "
                            + batch.getExpiryDate());
        }

        // Can only write off batches that still have stock
        if (batch.getCurrentQuantity() == 0) {
            throw new IllegalArgumentException(
                    "Batch already has zero stock. Nothing to write off.");
        }

        int writeOffQuantity = batch.getCurrentQuantity();

        // Set current quantity to zero — stock is gone
        batch.setCurrentQuantity(0);
        batchRepository.save(batch);

        // Record the writeoff movement — negative quantity = stock going out
        StockMovement writeOff = StockMovement.builder()
                .batch(batch)
                .movementType(MovementType.EXPIRY_WRITEOFF)
                .quantity(-writeOffQuantity)
                .referenceType("MANUAL")
                .notes("Expired batch written off. Expiry date: "
                        + batch.getExpiryDate())
                .build();

        stockMovementRepository.save(writeOff);

        return batchMapper.toDto(
                batchRepository.findByIdWithDetails(batchId).orElseThrow());
    }

    // ─────────────────────────────────────────────
    // INTERNAL METHODS — called by other services
    // NOT exposed via controller
    // ─────────────────────────────────────────────

    // Called by InvoiceService after saving an empty invoice
    // For each OrderItem, deducts stock FIFO by expiry
    // Creates ONE InvoiceLineItem PER BATCH used
    // This gives full batch traceability on the invoice itself
    // Returns all created line items — InvoiceService adds them to invoice
    @Transactional
    public List<InvoiceLineItem> deductStockAndCreateLineItems(
            Invoice invoice,
            List<org.ved.crm.order.OrderItem> orderItems,
            org.ved.crm.billing.TaxType taxType) {

        List<InvoiceLineItem> allLineItems = new ArrayList<>();

        for (org.ved.crm.order.OrderItem orderItem : orderItems) {

            UUID productId = orderItem.getProduct().getId();

            // Total units to deduct = ordered qty + free qty from schemes
            int remainingToDeduct = orderItem.getQuantity()
                    + orderItem.getFreeQuantity();

            // Get available batches FIFO by expiry date
            List<Batch> availableBatches =
                    batchRepository.findAvailableBatchesByProduct(productId);

            // Filter expired batches — never sell expired stock
            List<Batch> validBatches = availableBatches.stream()
                    .filter(b -> !b.isExpired())
                    .toList();

            // Validate total available stock
            int totalAvailable = validBatches.stream()
                    .mapToInt(Batch::getCurrentQuantity)
                    .sum();

            if (totalAvailable < remainingToDeduct) {
                throw new IllegalArgumentException(
                        "Insufficient stock for product: "
                                + orderItem.getProduct().getName()
                                + ". Required: " + remainingToDeduct
                                + ", Available: " + totalAvailable);
            }

            // Track how many free units we still need to assign
            // Free units from QUANTITY_FREE schemes are distributed
            // proportionally across batches
            int remainingFreeToAssign = orderItem.getFreeQuantity();

            // ── Deduct from batches FIFO ──────────────────────
            for (Batch batch : validBatches) {

                if (remainingToDeduct == 0) break;

                // Units to take from this batch
                int deductFromThisBatch = Math.min(
                        remainingToDeduct,
                        batch.getCurrentQuantity());

                // How many of these are free units?
                // Take free units from this batch proportionally
                int freeFromThisBatch = Math.min(
                        remainingFreeToAssign,
                        deductFromThisBatch);

                // Paid units from this batch
                int paidFromThisBatch = deductFromThisBatch - freeFromThisBatch;

                // ── Calculate GST for this batch's quantity ────
                // Only paid units are taxable — free units have no revenue
                BigDecimal grossAmount = orderItem.getUnitPrice()
                        .multiply(BigDecimal.valueOf(paidFromThisBatch))
                        .setScale(2, java.math.RoundingMode.HALF_UP);

                // Total discount = base discount + scheme discount
                BigDecimal totalDiscountPct = orderItem.getDiscountPct()
                        .add(orderItem.getSchemeDiscountPct());

                BigDecimal discountAmount = grossAmount
                        .multiply(totalDiscountPct)
                        .divide(BigDecimal.valueOf(100), 2,
                                java.math.RoundingMode.HALF_UP);

                BigDecimal taxableAmount = grossAmount
                        .subtract(discountAmount)
                        .setScale(2, java.math.RoundingMode.HALF_UP);

                BigDecimal gstRate = BigDecimal.valueOf(
                        orderItem.getProduct().getGstRate().getRate());

                BigDecimal cgstAmt = BigDecimal.ZERO;
                BigDecimal sgstAmt = BigDecimal.ZERO;
                BigDecimal igstAmt = BigDecimal.ZERO;

                if (taxType == org.ved.crm.billing.TaxType.CGST_SGST) {
                    cgstAmt = taxableAmount
                            .multiply(gstRate)
                            .divide(BigDecimal.valueOf(200), 2,
                                    java.math.RoundingMode.HALF_UP);
                    sgstAmt = cgstAmt;
                } else {
                    igstAmt = taxableAmount
                            .multiply(gstRate)
                            .divide(BigDecimal.valueOf(100), 2,
                                    java.math.RoundingMode.HALF_UP);
                }

                BigDecimal lineTotal = taxableAmount
                        .add(cgstAmt)
                        .add(sgstAmt)
                        .add(igstAmt)
                        .setScale(2, java.math.RoundingMode.HALF_UP);

                // ── Create InvoiceLineItem for this batch ──────
                InvoiceLineItem lineItem = InvoiceLineItem.builder()
                        .invoice(invoice)
                        .product(orderItem.getProduct())
                        .hsnCode(orderItem.getProduct().getHsnCode())
                        .quantity(paidFromThisBatch)
                        .freeQuantity(freeFromThisBatch)
                        .unitPrice(orderItem.getUnitPrice())
                        .discountPct(totalDiscountPct)
                        .taxableAmount(taxableAmount)
                        .cgstAmt(cgstAmt)
                        .sgstAmt(sgstAmt)
                        .igstAmt(igstAmt)
                        .lineTotal(lineTotal)
                        // ── Batch info — the key addition ─────
                        .batchNumber(batch.getBatchNumber())
                        .expiryDate(batch.getExpiryDate())
                        .build();

                allLineItems.add(lineItem);

                // ── Deduct stock from batch ────────────────────
                batch.setCurrentQuantity(
                        batch.getCurrentQuantity() - deductFromThisBatch);
                batchRepository.save(batch);

                // ── Record StockMovement ───────────────────────
                StockMovement saleMovement = StockMovement.builder()
                        .batch(batch)
                        .movementType(MovementType.SALE)
                        .quantity(-deductFromThisBatch)
                        .referenceId(invoice.getId())
                        .referenceType("INVOICE")
                        .notes("Stock deducted for invoice: "
                                + invoice.getInvoiceNumber()
                                + " (Batch: " + batch.getBatchNumber() + ")")
                        .build();

                stockMovementRepository.save(saleMovement);

                remainingToDeduct -= deductFromThisBatch;
                remainingFreeToAssign -= freeFromThisBatch;
            }
        }

        return allLineItems;
    }
    // Called by VisitService when rep logs samples given to a doctor
    // Deducts from specified batch directly — rep chooses which batch
    @Transactional
    public void deductStockForSample(UUID batchId, int quantity, UUID visitId){

        Batch batch = batchRepository.findByIdWithDetails(batchId)
                .orElseThrow(()->new ResourceNotFoundException("Batch","id",batchId));

        if(batch.isExpired()){
            throw new IllegalArgumentException(
                    "Cannot distribute samples from expired batch: "
                            + batch.getBatchNumber());
        }

        // Cannot give more samples than available
        if(batch.getCurrentQuantity() < quantity){
            throw new IllegalArgumentException(
                    "Insufficient stock in batch: " + batch.getBatchNumber()
                            + ". Available: " + batch.getCurrentQuantity()
                            + ", Requested: " + quantity);
        }

        batch.setCurrentQuantity(batch.getCurrentQuantity()- quantity);
        batchRepository.save(batch);

        // Record SAMPLE movement — linked to the visit
        StockMovement sampleMovement = StockMovement.builder()
                .batch(batch)
                .movementType(MovementType.SAMPLE)
                .quantity(-quantity)
                .referenceId(visitId)
                .referenceType("VISIT")
                .notes("Physician samples distributed")
                .build();

        stockMovementRepository.save(sampleMovement);
    }





}
