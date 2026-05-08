package org.ved.crm.returns;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.ved.crm.Payment.PaymentAllocationRepository;
import org.ved.crm.audit.Audited;
import org.ved.crm.billing.Invoice;
import org.ved.crm.billing.InvoiceRepository;
import org.ved.crm.billing.InvoiceStatus;
import org.ved.crm.chemist.Chemist;
import org.ved.crm.chemist.ChemistRepository;
import org.ved.crm.common.exception.ResourceNotFoundException;
import org.ved.crm.inventory.Batch;
import org.ved.crm.inventory.BatchRepository;
import org.ved.crm.inventory.MovementType;
import org.ved.crm.inventory.StockMovement;
import org.ved.crm.inventory.StockMovementRepository;
import org.ved.crm.product.Product;
import org.ved.crm.stockist.Stockist;
import org.ved.crm.stockist.StockistRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReturnService {

    private final ReturnRepository returnRepository;
    private final ReturnItemRepository returnItemRepository;
    private final CreditNoteRepository creditNoteRepository;
    private final BatchRepository batchRepository;
    private final StockMovementRepository stockMovementRepository;
    private final ChemistRepository chemistRepository;
    private final StockistRepository stockistRepository;
    private final InvoiceRepository invoiceRepository;
    private final PaymentAllocationRepository paymentAllocationRepository;
    private final ReturnMapper returnMapper;
    private final CreditNoteMapper creditNoteMapper;

    // ─────────────────────────────────────────────
    // GET METHODS
    // ─────────────────────────────────────────────

    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER')")
    public List<ReturnDto> getAllReturns() {
        return returnRepository.findAllWithDetails()
                .stream()
                // No credit note context in list queries — pass null
                .map(r -> returnMapper.toDto(r, null))
                .toList();
    }

    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'REP')")
    public ReturnDto getReturnById(UUID id) {
        Return returnDoc = returnRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Return", "id", id));
        // No credit note context in single fetch — pass null
        return returnMapper.toDto(returnDoc, null);
    }

    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'REP')")
    public List<ReturnDto> getReturnsByChemist(UUID chemistId) {
        return returnRepository.findByChemistId(chemistId)
                .stream()
                // No credit note context in list queries — pass null
                .map(r -> returnMapper.toDto(r, null))
                .toList();
    }

    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'REP')")
    public List<ReturnDto> getReturnsByStockist(UUID stockistId) {
        return returnRepository.findByStockistId(stockistId)
                .stream()
                // No credit note context in list queries — pass null
                .map(r -> returnMapper.toDto(r, null))
                .toList();
    }

    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'REP')")
    public List<CreditNoteDto> getOpenCreditNotesByChemist(UUID chemistId) {
        return creditNoteRepository.findOpenCreditNotesByChemist(chemistId)
                .stream()
                .map(creditNoteMapper::toDto)
                .toList();
    }

    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'REP')")
    public List<CreditNoteDto> getOpenCreditNotesByStockist(UUID stockistId) {
        return creditNoteRepository.findOpenCreditNotesByStockist(stockistId)
                .stream()
                .map(creditNoteMapper::toDto)
                .toList();
    }

    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'REP')")
    public CreditNoteDto getCreditNoteById(UUID id) {
        CreditNote creditNote = creditNoteRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "CreditNote", "id", id));
        return creditNoteMapper.toDto(creditNote);
    }

    // ─────────────────────────────────────────────
    // CREATE RETURN
    // Logs return document with status PENDING
    // No stock or financial changes at this stage
    // ─────────────────────────────────────────────

    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'REP')")
    @Transactional
    public ReturnDto createReturn(CreateReturnRequest request) {

        // Step 1 — Validate payer — exactly one must be provided
        if (request.chemistId() == null && request.stockistId() == null) {
            throw new IllegalArgumentException(
                    "Either chemistId or stockistId must be provided");
        }
        if (request.chemistId() != null && request.stockistId() != null) {
            throw new IllegalArgumentException(
                    "Only one of chemistId or stockistId can be provided");
        }

        // Step 2 — Load the payer
        Chemist chemist = null;
        Stockist stockist = null;

        if (request.chemistId() != null) {
            chemist = chemistRepository.findByIdWithDetails(request.chemistId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Chemist", "id", request.chemistId()));
        } else {
            stockist = stockistRepository.findByIdWithDetails(request.stockistId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Stockist", "id", request.stockistId()));
        }

        // Step 3 — Generate return number from PostgreSQL SEQUENCE
        String returnNumber = generateReturnNumber();

        // Step 4 — Build Return entity
        // Status defaults to PENDING via @Builder.Default
        Return returnDoc = Return.builder()
                .chemist(chemist)
                .stockist(stockist)
                .returnNumber(returnNumber)
                .returnDate(request.returnDate())
                .reason(request.reason())
                .build();

        // Step 5 — Build ReturnItem entities
        for (ReturnItemRequest itemReq : request.returnItems()) {

            // Load batch — drives product, price, and stock location
            Batch batch = batchRepository.findByIdWithDetails(itemReq.batchId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Batch", "id", itemReq.batchId()));

            // Product is derived from batch — one batch = one product always
            Product product = batch.getProduct();

            // Unit price = dealer price — base return value
            BigDecimal unitPrice = product.getDealerPrice();
            BigDecimal lineTotal = unitPrice
                    .multiply(BigDecimal.valueOf(itemReq.quantity()))
                    .setScale(2, RoundingMode.HALF_UP);

            ReturnItem returnItem = ReturnItem.builder()
                    .returnDoc(returnDoc)
                    .batch(batch)
                    .product(product)
                    .quantity(itemReq.quantity())
                    .condition(itemReq.condition())
                    .unitPrice(unitPrice)
                    .lineTotal(lineTotal)
                    .build();

            returnDoc.getReturnItems().add(returnItem);
        }

        // Step 6 — Save return — cascade saves all items automatically
        // No credit note at creation time — return is still PENDING
        Return saved = returnRepository.save(returnDoc);
        return returnMapper.toDto(
                returnRepository.findByIdWithDetails(saved.getId()).orElseThrow(),
                null);
    }

    // ─────────────────────────────────────────────
    // PROCESS RETURN
    // Adjusts stock based on condition
    // Raises credit note for full return value
    // ─────────────────────────────────────────────

    @Audited(action = "RETURN_PROCESSED", entityType = "Return")
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER')")
    @Transactional
    public ReturnDto processReturn(UUID returnId) {

        // Step 1 — Load the return
        Return returnDoc = returnRepository.findByIdWithDetails(returnId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Return", "id", returnId));

        // Step 2 — Only PENDING returns can be processed
        if (returnDoc.getStatus() != ReturnStatus.PENDING) {
            throw new IllegalArgumentException(
                    "Only PENDING returns can be processed. Current status: "
                            + returnDoc.getStatus());
        }

        // Step 3 — Process each return item
        BigDecimal totalCreditAmount = BigDecimal.ZERO;

        for (ReturnItem item : returnDoc.getReturnItems()) {

            Batch batch = item.getBatch();

            if (item.getCondition() == ReturnItemCondition.SALEABLE) {

                // SALEABLE — restore stock to batch
                // Chemist returned good stock that can be resold
                batch.setCurrentQuantity(
                        batch.getCurrentQuantity() + item.getQuantity());
                batchRepository.save(batch);

                // Record RETURN movement — positive quantity = stock coming IN
                StockMovement returnMovement = StockMovement.builder()
                        .batch(batch)
                        .movementType(MovementType.RETURN)
                        .quantity(item.getQuantity())
                        .referenceId(returnDoc.getId())
                        .referenceType("RETURN")
                        .notes("Stock returned — saleable. Return: "
                                + returnDoc.getReturnNumber())
                        .build();
                stockMovementRepository.save(returnMovement);

            } else {

                // DAMAGED or EXPIRED — write off the stock
                // Do NOT restore to batch — goods are unusable
                // But DO reduce currentQuantity — these units are gone
                batch.setCurrentQuantity(
                        Math.max(0, batch.getCurrentQuantity() - item.getQuantity()));
                batchRepository.save(batch);

                // Record EXPIRY_WRITEOFF movement — negative = stock going OUT
                StockMovement writeOffMovement = StockMovement.builder()
                        .batch(batch)
                        .movementType(MovementType.EXPIRY_WRITEOFF)
                        .quantity(-item.getQuantity())
                        .referenceId(returnDoc.getId())
                        .referenceType("RETURN")
                        .notes("Stock written off — " + item.getCondition()
                                + ". Return: " + returnDoc.getReturnNumber())
                        .build();
                stockMovementRepository.save(writeOffMovement);
            }

            // Credit accumulates for ALL returned items regardless of condition
            // Stockist/chemist paid for these goods — they get credit back either way
            totalCreditAmount = totalCreditAmount.add(item.getLineTotal());
        }

        // Step 4 — Mark return as PROCESSED
        returnDoc.setStatus(ReturnStatus.PROCESSED);
        returnRepository.save(returnDoc);

        // Step 5 — Generate Credit Note
        // One credit note per return — always raised on processing
        String creditNoteNumber = generateCreditNoteNumber();

        CreditNote creditNote = CreditNote.builder()
                .returnDoc(returnDoc)
                .chemist(returnDoc.getChemist())
                .stockist(returnDoc.getStockist())
                .creditNoteNumber(creditNoteNumber)
                .amount(totalCreditAmount.setScale(2, RoundingMode.HALF_UP))
                .build();

        CreditNote savedCreditNote = creditNoteRepository.save(creditNote);

        // Step 6 — Re-fetch both return and credit note with all relationships
        // Re-fetch is required to get correct updatedAt and all lazy relationships
        // loaded for the mapper — same pattern used across all domains
        CreditNote fetchedCreditNote = creditNoteRepository
                .findByIdWithDetails(savedCreditNote.getId()).orElseThrow();

        return returnMapper.toDto(
                returnRepository.findByIdWithDetails(returnId).orElseThrow(),
                fetchedCreditNote);
    }

    // ─────────────────────────────────────────────
    // REJECT RETURN
    // Marks return as rejected — no stock or financial changes
    // ─────────────────────────────────────────────

    @Audited(action = "RETURN_REJECTED", entityType = "Return")
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER')")
    @Transactional
    public ReturnDto rejectReturn(UUID returnId) {

        Return returnDoc = returnRepository.findByIdWithDetails(returnId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Return", "id", returnId));

        if (returnDoc.getStatus() != ReturnStatus.PENDING) {
            throw new IllegalArgumentException(
                    "Only PENDING returns can be rejected. Current status: "
                            + returnDoc.getStatus());
        }

        returnDoc.setStatus(ReturnStatus.REJECTED);
        returnRepository.save(returnDoc);

        // No credit note on rejection — pass null
        return returnMapper.toDto(
                returnRepository.findByIdWithDetails(returnId).orElseThrow(),
                null);
    }

    // ─────────────────────────────────────────────
    // APPLY CREDIT NOTE
    // Applies an OPEN credit note to an ISSUED or PARTIALLY_PAID invoice
    // Updates invoice status based on true outstanding balance
    // ─────────────────────────────────────────────

    @Audited(action = "CREDIT_NOTE_APPLIED", entityType = "CreditNote")
    @PreAuthorize("hasRole('OWNER')")
    @Transactional
    public CreditNoteDto applyCreditNote(UUID creditNoteId, UUID invoiceId) {

        // Step 1 — Load credit note
        CreditNote creditNote = creditNoteRepository.findByIdWithDetails(creditNoteId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "CreditNote", "id", creditNoteId));

        // Step 2 — Only OPEN credit notes can be applied
        if (creditNote.getStatus() != CreditNoteStatus.OPEN) {
            throw new IllegalArgumentException(
                    "Only OPEN credit notes can be applied. Current status: "
                            + creditNote.getStatus());
        }

        // Step 3 — Load the invoice
        Invoice invoice = invoiceRepository.findByIdWithDetails(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Invoice", "id", invoiceId));

        // Step 4 — Invoice must be ISSUED or PARTIALLY_PAID
        if (invoice.getStatus() == InvoiceStatus.DRAFT ||
                invoice.getStatus() == InvoiceStatus.PAID) {
            throw new IllegalArgumentException(
                    "Cannot apply credit note to a "
                            + invoice.getStatus() + " invoice");
        }

        // Step 5 — Validate payer match
        // Credit note must belong to the same party as the invoice billing
        // Prevents applying Sharma's credit to Patel's invoice
        boolean payerMatches = false;

        if (creditNote.getChemist() != null && invoice.getChemist() != null) {
            payerMatches = creditNote.getChemist().getId()
                    .equals(invoice.getChemist().getId());
        } else if (creditNote.getStockist() != null && invoice.getStockist() != null) {
            payerMatches = creditNote.getStockist().getId()
                    .equals(invoice.getStockist().getId());
        }

        if (!payerMatches) {
            throw new IllegalArgumentException(
                    "Credit note payer does not match invoice billing party");
        }

        // Step 6 — Apply credit note to invoice
        creditNote.setStatus(CreditNoteStatus.APPLIED);
        creditNote.setAppliedToInvoice(invoice);
        creditNoteRepository.save(creditNote);

        // Step 7 — Calculate true outstanding balance
        // Accounts for ALL payments AND ALL credit notes applied to this invoice
        // including the one we just saved in Step 6
        BigDecimal totalAlreadyPaid = paymentAllocationRepository
                .getTotalAllocatedForInvoice(invoiceId);

        BigDecimal totalAlreadyCredited = creditNoteRepository
                .getTotalCreditAppliedForInvoice(invoiceId);

        BigDecimal outstanding = invoice.getGrandTotal()
                .subtract(totalAlreadyPaid)
                .subtract(totalAlreadyCredited);

        if (outstanding.compareTo(BigDecimal.ZERO) <= 0) {
            invoice.setStatus(InvoiceStatus.PAID);
        } else {
            invoice.setStatus(InvoiceStatus.PARTIALLY_PAID);
        }
        invoiceRepository.save(invoice);

        return creditNoteMapper.toDto(
                creditNoteRepository.findByIdWithDetails(creditNoteId).orElseThrow());
    }

    // ─────────────────────────────────────────────
    // SEQUENCE GENERATORS
    // ─────────────────────────────────────────────

    private String generateReturnNumber() {
        Long nextVal = returnRepository.getNextSequenceValue();
        return String.format("RET-%d-%06d", LocalDate.now().getYear(), nextVal);
    }

    private String generateCreditNoteNumber() {
        Long nextVal = creditNoteRepository.getNextSequenceValue();
        return String.format("CN-%d-%06d", LocalDate.now().getYear(), nextVal);
    }
}