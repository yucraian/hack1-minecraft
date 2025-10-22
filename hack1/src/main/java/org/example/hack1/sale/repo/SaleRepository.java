package org.example.hack1.sale.repo;

import org.example.hack1.sale.domain.Sale;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SaleRepository extends JpaRepository<Sale, Long> {
    List<Sale> findByBranch(String branch);
}
