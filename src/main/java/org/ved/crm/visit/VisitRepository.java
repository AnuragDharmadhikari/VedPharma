package org.ved.crm.visit;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VisitRepository extends JpaRepository<Visit, UUID> {

    @Query("""
        SELECT v FROM Visit v
        JOIN FETCH v.rep
        JOIN FETCH v.doctor
        LEFT JOIN FETCH v.visitProducts vp
        LEFT JOIN FETCH vp.product
        WHERE v.id = :id
    """)
    Optional<Visit> findByIdWithDetails(@Param("id") UUID id);

    @Query("""
        SELECT v FROM Visit v
        JOIN FETCH v.rep
        JOIN FETCH v.doctor
        LEFT JOIN FETCH v.visitProducts vp
        LEFT JOIN FETCH vp.product
        WHERE v.doctor.id = :doctorId
        ORDER BY v.visitDate DESC
    """)
    List<Visit> findByDoctorIdWithDetails(@Param("doctorId") UUID doctorId);

    @Query("""
        SELECT v FROM Visit v
        JOIN FETCH v.rep
        JOIN FETCH v.doctor
        LEFT JOIN FETCH v.visitProducts vp
        LEFT JOIN FETCH vp.product
        WHERE v.rep.id = :repId
        ORDER BY v.visitDate DESC
    """)
    List<Visit> findByRepIdWithDetails(@Param("repId") UUID repId);

    @Query("""
        SELECT v FROM Visit v
        JOIN FETCH v.rep
        JOIN FETCH v.doctor
        LEFT JOIN FETCH v.visitProducts vp
        LEFT JOIN FETCH vp.product
        ORDER BY v.visitDate DESC
    """)
    List<Visit> findAllWithDetails();
}