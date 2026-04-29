package org.ved.crm.stockist;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StockistRepository extends JpaRepository<Stockist, UUID> {

    @Query("SELECT s FROM Stockist s JOIN FETCH s.assignedRep WHERE s.assignedRep.id = :repId")
    List<Stockist> findByAssignedRepId(@Param("repId") UUID repId);

    @Query("SELECT s FROM Stockist s JOIN FETCH s.assignedRep WHERE s.isActive = true")
    List<Stockist> findAllActive();

    @Query("SELECT s FROM Stockist s JOIN FETCH s.assignedRep WHERE s.id = :id")
    Optional<Stockist> findByIdWithDetails(@Param("id") UUID id);

    boolean existsByGstin(String gstin);
}