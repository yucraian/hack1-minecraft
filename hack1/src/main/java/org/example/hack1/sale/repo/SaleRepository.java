package org.example.hack1.sale.repo;

import org.example.hack1.sale.domain.Sale;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SaleRepository extends JpaRepository<Sale, Long> {
}
