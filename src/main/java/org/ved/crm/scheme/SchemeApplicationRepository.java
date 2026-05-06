package org.ved.crm.scheme;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SchemeApplicationRepository extends JpaRepository<SchemeApplication, UUID> {

    // All scheme applications for a specific order item.
    // JOIN FETCH scheme so the mapper can access scheme details
    // without triggering a lazy load outside the session.
    @Query("""
            SELECT sa FROM SchemeApplication sa
            JOIN FETCH sa.scheme
            WHERE sa.orderItem.id = :orderItemId
            """)
    List<SchemeApplication> findByOrderItemId(@Param("orderItemId") UUID orderItemId);

    // All scheme applications across all items of a given order.
    // Used for the order detail view — owner sees the complete
    // scheme benefit summary for the entire order in one query.
    // JOIN FETCH orderItem so we can display which item each scheme fired on.
    @Query("""
            SELECT sa FROM SchemeApplication sa
            JOIN FETCH sa.scheme
            JOIN FETCH sa.orderItem
            WHERE sa.orderItem.order.id = :orderId
            """)
    List<SchemeApplication> findByOrderId(@Param("orderId") UUID orderId);
}