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

import java.time.LocalDate;
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

    // Called by InvoiceService after saving an invoice
    // Deducts stock for every line item in the invoice
    // Uses FIFO by expiry — oldest expiring batch deducted first
    @Transactional
    public void deductStockForInvoice(Invoice invoice){
        // Loop through every line item in the invoice
        // Each line item is one product with a quantity
        for(InvoiceLineItem lineItem : invoice.getLineItems()){

            UUID productId = lineItem.getProduct().getId();
            // How many units of this product need to be deducted
            // Deduct ordered quantity PLUS free units from QUANTITY_FREE schemes.
            // Free units are physical goods leaving the warehouse even though
            // no revenue is recorded for them. freeQuantity defaults to 0
            // when no scheme applied so this is always safe.
            int remainingToDeduct = lineItem.getQuantity() + lineItem.getFreeQuantity();

            // Get all available batches for this product
            // ordered by expiry date ASC — oldest expiring first (FIFO)
            // Only batches with currentQuantity > 0 are returned
            List<Batch> availableBatches = batchRepository.findAvailableBatchesByProduct(productId);

            // Step 1 — Filter out expired batches
            // We never sell from expired batches
            List<Batch> validBatches = availableBatches.stream()
                    .filter(b->!b.isExpired())
                    .toList();

            // Step 2 — Calculate total available stock across all valid batches
            int totalAvailable = validBatches.stream()
                    .mapToInt(Batch::getCurrentQuantity)
                    .sum();

            // Step 3 — Check if we have enough stock
            // If not, throw exception — transaction will rollback
            // Invoice will NOT be saved
            if(totalAvailable < remainingToDeduct){
                throw new IllegalArgumentException(
                        "Insufficient stock for product: "
                                + lineItem.getProduct().getName()
                                + ". Required: " + remainingToDeduct
                                + ", Available: " + totalAvailable);
            }

            // Step 4 — Deduct from batches FIFO by expiry
            // We loop through batches oldest-expiry-first
            // and deduct as much as possible from each

            for(Batch batch : validBatches){

                // If we've deducted everything we need, stop
                if(remainingToDeduct == 0 ) break;

                // How much can we take from this batch?
                // Either the full remaining amount needed
                // or everything in this batch — whichever is smaller
                int deductFromThisBatch = Math.min(remainingToDeduct,batch.getCurrentQuantity());

                batch.setCurrentQuantity(batch.getCurrentQuantity()-deductFromThisBatch);
                batchRepository.save(batch);

                // Record the SALE movement for this batch
                // quantity is negative — stock going OUT
                StockMovement saleMovement = StockMovement.builder()
                        .batch(batch)
                        .movementType(MovementType.SALE)
                        .quantity(-deductFromThisBatch)
                        .referenceId(invoice.getId())
                        .referenceType("INVOICE")
                        .notes("Stock deducted for invoice: "
                                + invoice.getInvoiceNumber())
                        .build();

                stockMovementRepository.save(saleMovement);

                // Reduce the remaining amount we still need to deduct
                remainingToDeduct -= deductFromThisBatch;
            }
            // After this loop, remainingToDeduct will be 0
            // because we verified totalAvailable >= required in Step 3
        }
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
