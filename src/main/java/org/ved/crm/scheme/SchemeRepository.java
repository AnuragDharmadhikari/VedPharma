package org.ved.crm.scheme;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SchemeRepository extends JpaRepository<Scheme, UUID> {

    // ─── Queries for REST endpoints ───────────────────────────────────────────

    // All active schemes for a specific chemist.
    // LEFT JOIN FETCH on both product and chemist because mapper accesses them.
    // stockist is null for chemist schemes so we don't fetch it.
    @Query("""
            SELECT s FROM Scheme s
            LEFT JOIN FETCH s.product
            LEFT JOIN FETCH s.chemist
            WHERE s.chemist.id = :chemistId
            AND s.isActive = true
            """)
    List<Scheme> findActiveByChemistId(@Param("chemistId") UUID chemistId);

    // All active schemes for a specific stockist.
    @Query("""
            SELECT s FROM Scheme s
            LEFT JOIN FETCH s.product
            LEFT JOIN FETCH s.stockist
            WHERE s.stockist.id = :stockistId
            AND s.isActive = true
            """)
    List<Scheme> findActiveByStockistId(@Param("stockistId") UUID stockistId);

    // Get by ID with all relationships fetched — used after save to return
    // correct updatedAt. Both stockist and chemist are LEFT JOIN FETCH
    // because exactly one will be null depending on buyer type.
    @Query("""
            SELECT s FROM Scheme s
            LEFT JOIN FETCH s.product
            LEFT JOIN FETCH s.stockist
            LEFT JOIN FETCH s.chemist
            WHERE s.id = :id
            """)
    Optional<Scheme> findByIdWithDetails(@Param("id") UUID id);

    // ─── Auto-apply queries called internally by OrderService ─────────────────

    // Find applicable scheme for a DIRECT order (chemist is the buyer).
    // Conditions:
    //   1. Same product as the order item
    //   2. Assigned to this specific chemist
    //   3. Today is within the validity window (validFrom <= today <= validTo)
    //   4. Scheme is active
    // Returns Optional — no scheme found means no benefit applied, order proceeds normally.
    @Query("""
            SELECT s FROM Scheme s
            LEFT JOIN FETCH s.product
            WHERE s.product.id = :productId
            AND s.chemist.id = :chemistId
            AND s.validFrom <= :today
            AND s.validTo >= :today
            AND s.isActive = true
            """)
    Optional<Scheme> findApplicableSchemeForChemist(
            @Param("productId") UUID productId,
            @Param("chemistId") UUID chemistId,
            @Param("today") LocalDate today
    );

    // Find applicable scheme for a VIA_STOCKIST order (stockist is the buyer).
    // Same logic, but buyer is stockist instead of chemist.
    @Query("""
            SELECT s FROM Scheme s
            LEFT JOIN FETCH s.product
            WHERE s.product.id = :productId
            AND s.stockist.id = :stockistId
            AND s.validFrom <= :today
            AND s.validTo >= :today
            AND s.isActive = true
            """)
    Optional<Scheme> findApplicableSchemeForStockist(
            @Param("productId") UUID productId,
            @Param("stockistId") UUID stockistId,
            @Param("today") LocalDate today
    );

    // Duplicate check before creating a new scheme.
    // A buyer should not have two active schemes of the same type
    // on the same product with overlapping validity periods.
    // Called in SchemeService.createScheme before saving.
    @Query("""
            SELECT COUNT(s) > 0 FROM Scheme s
            WHERE s.product.id = :productId
            AND s.chemist.id = :chemistId
            AND s.schemeType = :schemeType
            AND s.isActive = true
            AND s.validFrom <= :validTo
            AND s.validTo >= :validFrom
            """)
    boolean existsActiveConflictForChemist(
            @Param("productId") UUID productId,
            @Param("chemistId") UUID chemistId,
            @Param("schemeType") SchemeType schemeType,
            @Param("validFrom") LocalDate validFrom,
            @Param("validTo") LocalDate validTo
    );

    // Same conflict check for stockist buyer.
    @Query("""
            SELECT COUNT(s) > 0 FROM Scheme s
            WHERE s.product.id = :productId
            AND s.stockist.id = :stockistId
            AND s.schemeType = :schemeType
            AND s.isActive = true
            AND s.validFrom <= :validTo
            AND s.validTo >= :validFrom
            """)
    boolean existsActiveConflictForStockist(
            @Param("productId") UUID productId,
            @Param("stockistId") UUID stockistId,
            @Param("schemeType") SchemeType schemeType,
            @Param("validFrom") LocalDate validFrom,
            @Param("validTo") LocalDate validTo
    );
}