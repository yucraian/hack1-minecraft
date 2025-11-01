package org.example.hack1.sale.repo;

import org.example.hack1.sale.domain.Sale;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface SaleRepository extends JpaRepository<Sale, Long> {

    // Métodos paginados
    Page<Sale> findByBranch(String branch, Pageable pageable);
    Page<Sale> findBySoldAtBetween(LocalDateTime start, LocalDateTime end, Pageable pageable);
    Page<Sale> findByBranchAndSoldAtBetween(String branch, LocalDateTime start, LocalDateTime end, Pageable pageable);

    // Métodos para agregaciones (sin paginación)
    List<Sale> findByBranchAndSoldAtBetween(String branch, LocalDateTime start, LocalDateTime end);
    List<Sale> findBySoldAtBetween(LocalDateTime start, LocalDateTime end);

    // Método para buscar por branch
    List<Sale> findByBranch(String branch);

    // Query para obtener ventas por rango de fechas
    @Query("SELECT s FROM Sale s WHERE s.soldAt BETWEEN :start AND :end")
    List<Sale> findSalesInDateRange(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}