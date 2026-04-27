package org.ved.crm.doctor;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DoctorRepository extends JpaRepository<Doctor, UUID> {

    @Query("SELECT d FROM Doctor d LEFT JOIN FETCH d.territory WHERE d.territory.id = :territoryId")
    List<Doctor> findByTerritoryId(@Param("territoryId") UUID territoryId);

    @Query("SELECT d FROM Doctor d LEFT JOIN FETCH d.territory WHERE d.specialty = :specialty")
    List<Doctor> findBySpecialty(@Param("specialty") String specialty);

    @Query("SELECT d FROM Doctor d LEFT JOIN FETCH d.territory WHERE d.isActive = true")
    List<Doctor> findByIsActiveTrue();

    @Query("SELECT d FROM Doctor d LEFT JOIN FETCH d.territory WHERE d.id = :id")
    Optional<Doctor> findByIdWithDetails(@Param("id") UUID id);
}