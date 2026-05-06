package org.ved.crm.scheme;


import lombok.RequiredArgsConstructor;
import org.springframework.cglib.core.Local;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.ved.crm.chemist.Chemist;
import org.ved.crm.chemist.ChemistRepository;
import org.ved.crm.common.exception.ResourceNotFoundException;
import org.ved.crm.order.FulfillmentType;
import org.ved.crm.order.Order;
import org.ved.crm.order.OrderItem;
import org.ved.crm.product.Product;
import org.ved.crm.product.ProductRepository;
import org.ved.crm.stockist.Stockist;
import org.ved.crm.stockist.StockistRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SchemeService {

    private final SchemeRepository schemeRepository;
    private final SchemeApplicationRepository schemeApplicationRepository;
    private final ProductRepository productRepository;
    private final ChemistRepository chemistRepository;
    private final StockistRepository stockistRepository;
    private final SchemeMapper schemeMapper;

    // ─── CRUD Methods ─────────────────────────────────────────────────────────

    public List<SchemeDto> getSchemesByChemist(UUID chemistId){
        return schemeRepository.findActiveByChemistId(chemistId)
                .stream()
                .map(schemeMapper::toDto)
                .toList();
    }

    public List<SchemeDto> getSchemesByStockist(UUID stockistId){
        return schemeRepository.findActiveByStockistId(stockistId)
                .stream().map(schemeMapper::toDto)
                .toList();
    }

    public SchemeDto getSchemeById(UUID id){
        Scheme scheme = schemeRepository.findByIdWithDetails(id)
                .orElseThrow(()->new ResourceNotFoundException("Scheme","id",id));
        return schemeMapper.toDto(scheme);
    }

    @Transactional
    public SchemeDto createScheme(CreateSchemeRequest request){

        // Step 1: Validate buyer XOR — exactly one of chemistId or stockistId
        if(request.chemistId() == null && request.stockistId() == null){
            throw new IllegalArgumentException(
                    "Either chemistId or stockistId must be provided");
        }
        if(request.chemistId() != null && request.stockistId() != null){
            throw new IllegalArgumentException(
                    "Only one of chemistId or stockistId can be provided, not both");
        }

        // Step 2: Validate schemeType XOR fields
        // QUANTITY_FREE must have freeQuantity, must not have discountPct
        // PERCENTAGE_DISCOUNT must have discountPct, must not have freeQuantity
        if(request.schemeType() == SchemeType.QUANTITY_FREE){
            if(request.freeQuantity() == null){
                throw new IllegalArgumentException(
                        "freeQuantity is required for QUANTITY_FREE schemes");
            }
            if(request.discountPct() != null){
                throw new IllegalArgumentException(
                        "discountPct must not be provided for QUANTITY_FREE schemes");
            }
        }
        if(request.schemeType() == SchemeType.PERCENTAGE_DISCOUNT){
            if(request.discountPct() == null){
                throw new IllegalArgumentException(
                        "discountPct is required for PERCENTAGE_DISCOUNT schemes");
            }
            if(request.freeQuantity()!= null){
                throw new IllegalArgumentException(
                        "freeQuantity must not be provided for PERCENTAGE_DISCOUNT schemes");
            }
        }

        // Step 3: Validate validity window — validFrom must be before validTo
        if(!request.validFrom().isBefore(request.validTo())){
            throw new IllegalArgumentException(
                    "validFrom must be before validTo");
        }

        // Step 4: Fetch product
        Product product = productRepository.findById(request.productId())
                .orElseThrow(()->new ResourceNotFoundException("Product","id",request.productId()));
        if (!product.isActive()) {
            throw new IllegalStateException(
                    "Cannot create a scheme for a discontinued product: "
                            + product.getName());
        }

        // Step 5: Fetch buyer, check for duplicate/overlapping scheme
        Chemist chemist = null;
        Stockist stockist = null;

        if(request.chemistId() != null){
            chemist = chemistRepository.findById(request.chemistId())
                    .orElseThrow(()->new ResourceNotFoundException("Chemist","id",request.chemistId()));
            if (!chemist.isActive()) {
                throw new IllegalStateException(
                        "Cannot create a scheme for a deactivated chemist: "
                                + chemist.getFirmName());
            }

            // Duplicate check — same product, same chemist, same schemeType,
            // overlapping validity period
            boolean conflict = schemeRepository.existsActiveConflictForChemist(
                    product.getId(),
                    chemist.getId(),
                    request.schemeType(),
                    request.validFrom(),
                    request.validTo()
            );

            if (conflict) {
                throw new IllegalStateException(
                        "An active " + request.schemeType() +
                                " scheme already exists for this chemist and product " +
                                "in the given validity period");
            }

        }

        if (request.stockistId() != null) {
            stockist = stockistRepository.findById(request.stockistId())
                    .orElseThrow(()->new ResourceNotFoundException("Stockist","id",request.stockistId()));
            if (!stockist.isActive()) {
                throw new IllegalStateException(
                        "Cannot create a scheme for a deactivated stockist: "
                                + stockist.getFirmName());
            }

            boolean conflict = schemeRepository.existsActiveConflictForStockist(
                    product.getId(),
                    stockist.getId(),
                    request.schemeType(),
                    request.validFrom(),
                    request.validTo()
            );
            if (conflict) {
                throw new IllegalStateException(
                        "An active " + request.schemeType() +
                                " scheme already exists for this stockist and product " +
                                "in the given validity period");
            }
        }


        // Step 6: Build and save scheme

        Scheme scheme = Scheme.builder()
                .product(product)
                .chemist(chemist)
                .stockist(stockist)
                .schemeType(request.schemeType())
                .minQuantity(request.minQuantity())
                .freeQuantity(request.freeQuantity())
                .discountPct(request.discountPct())
                .validFrom(request.validFrom())
                .validTo(request.validTo())
                .build();
        schemeRepository.save(scheme);

        // Step 7: Re-fetch with details for correct updatedAt and relationships
        return schemeMapper.toDto(
                schemeRepository.findByIdWithDetails(scheme.getId()).orElseThrow()
        );
    }

    @Transactional
    public SchemeDto updateScheme(UUID id, UpdateSchemeRequest request) {

        // Fetch existing scheme
        Scheme scheme = schemeRepository.findByIdWithDetails(id)
                .orElseThrow(()->new ResourceNotFoundException("Scheme","id",id));

        // Update only fields that are present in the request
        if (request.minQuantity() != null) {
            scheme.setMinQuantity(request.minQuantity());
        }

        // Validate schemeType consistency before updating benefit fields.
        // We don't allow changing schemeType on update — so we check against
        // the existing type on the scheme entity.
        if (request.freeQuantity() != null) {
            if (scheme.getSchemeType() != SchemeType.QUANTITY_FREE) {
                throw new IllegalArgumentException(
                        "freeQuantity can only be updated on QUANTITY_FREE schemes");
            }
            scheme.setFreeQuantity(request.freeQuantity());
        }

        if (request.discountPct() != null) {
            if (scheme.getSchemeType() != SchemeType.PERCENTAGE_DISCOUNT) {
                throw new IllegalArgumentException(
                        "discountPct can only be updated on PERCENTAGE_DISCOUNT schemes");
            }
            scheme.setDiscountPct(request.discountPct());
        }

        if (request.validTo() != null) {
            // New validTo must still be after the scheme's existing validFrom
            if (!request.validTo().isAfter(scheme.getValidFrom())) {
                throw new IllegalArgumentException(
                        "validTo must be after the scheme's validFrom: "
                                + scheme.getValidFrom());
            }
            scheme.setValidTo(request.validTo());
        }
        if (request.isActive() != null) {
            scheme.setActive(request.isActive());
        }

        schemeRepository.save(scheme);

        return schemeMapper.toDto(
                schemeRepository.findByIdWithDetails(scheme.getId()).orElseThrow()
        );

    }

    // ─── Internal Engine — Called by OrderService ──────────────────────────────

    // This method is the heart of the schemes engine.
    // Called once per order item during order creation.
    // Modifies the order item in-place if a scheme applies,
    // saves a SchemeApplication snapshot, and returns the modified item.
    // If no scheme applies or minQuantity not met, item returned unchanged.

    @Transactional
    public void applySchemes(OrderItem orderItem, Order order) {

        LocalDate today = LocalDate.now();
        Optional<Scheme> applicableScheme;

        // Step 1: Find applicable scheme based on fulfillment type.
        // VIA_STOCKIST → stockist is the buyer getting the scheme benefit
        // DIRECT → chemist is the buyer getting the scheme benefit
        if (order.getFulfillmentType() == FulfillmentType.VIA_STOCKIST) {
            applicableScheme = schemeRepository.findApplicableSchemeForStockist(
                    orderItem.getProduct().getId(),
                    order.getStockist().getId(),
                    today
            );
        } else {
            applicableScheme = schemeRepository.findApplicableSchemeForChemist(
                    orderItem.getProduct().getId(),
                    order.getChemist().getId(),
                    today
            );
        }

        // Step 2: No scheme found — return early, item completely unchanged.
        // This is the normal case for most order items.
        if (applicableScheme.isEmpty()) {
            return;
        }

        Scheme scheme = applicableScheme.get();

        // Step 3: Check minimum quantity threshold.
        // Scheme exists but buyer didn't order enough to trigger it.
        // Example: scheme requires 10 units, buyer ordered 8 — no benefit.
        if (orderItem.getQuantity() < scheme.getMinQuantity()) {
            return;
        }

        // Step 4: Apply benefit based on scheme type
        String benefitDescription;

        if (scheme.getSchemeType() == SchemeType.QUANTITY_FREE) {

            // Set free quantity on the order item.
            // InvoiceService will deduct qty + freeQuantity from stock.
            // No revenue is recorded for free units — they're physical goods only.
            orderItem.setFreeQuantity(scheme.getFreeQuantity());

            benefitDescription = scheme.getFreeQuantity() + " units free on order of "
                    + orderItem.getQuantity() + " units of "
                    + orderItem.getProduct().getName();

        } else {

            // Add scheme discount on top of any existing per-item discount.
            // Example: item has 5% discount, scheme gives 3% more → total 8%
            // We store them separately so the audit trail shows both.
            orderItem.setSchemeDiscountPct(
                    scheme.getDiscountPct().setScale(2, RoundingMode.HALF_UP));

            benefitDescription = scheme.getDiscountPct().setScale(2, RoundingMode.HALF_UP)
                    + "% discount applied on order of "
                    + orderItem.getQuantity() + " units of "
                    + orderItem.getProduct().getName();
        }

        // Step 5: Recalculate lineTotal with scheme benefit included.
        // Formula: qty × unitPrice × (1 - (discountPct + schemeDiscountPct) / 100)
        // For QUANTITY_FREE: discountPct unchanged, freeQuantity recorded separately
        // For PERCENTAGE_DISCOUNT: total discount = discountPct + schemeDiscountPct
        BigDecimal totalDiscountPct = orderItem.getDiscountPct()
                .add(orderItem.getSchemeDiscountPct());

        BigDecimal discountMultiplier = BigDecimal.ONE
                .subtract(totalDiscountPct.divide(
                        new BigDecimal("100"), 10, RoundingMode.HALF_UP));

        BigDecimal newLineTotal = orderItem.getUnitPrice()
                .multiply(new BigDecimal(orderItem.getQuantity()))
                .multiply(discountMultiplier)
                .setScale(2, RoundingMode.HALF_UP);

        orderItem.setLineTotal(newLineTotal);

        // Step 6: Save SchemeApplication — immutable snapshot of what fired
        SchemeApplication schemeApplication = SchemeApplication.builder()
                .orderItem(orderItem)
                .scheme(scheme)
                .schemeType(scheme.getSchemeType())
                .benefitDescription(benefitDescription)
                .freeQuantity(scheme.getSchemeType() == SchemeType.QUANTITY_FREE
                        ? scheme.getFreeQuantity() : null)
                .discountApplied(scheme.getSchemeType() == SchemeType.PERCENTAGE_DISCOUNT
                        ? scheme.getDiscountPct() : null)
                .build();

        schemeApplicationRepository.save(schemeApplication);
    }

}
