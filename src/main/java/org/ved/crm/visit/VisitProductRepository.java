package org.ved.crm.visit;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface VisitProductRepository extends JpaRepository<VisitProduct, UUID> {
}