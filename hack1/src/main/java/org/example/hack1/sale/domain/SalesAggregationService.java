package org.example.hack1.sale.domain;

import lombok.RequiredArgsConstructor;
import org.example.hack1.sale.repo.SaleRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SalesAggregationService {

    private final SaleRepository saleRepository;

    public SalesAggregates calculateAggregates(LocalDate from, LocalDate to, String branch) {
        List<Sale> sales = saleRepository.findSalesInDateRange(
                from.atStartOfDay(),
                to.atTime(23, 59, 59)
        );

        // Filtrar por branch si se especifica
        if (branch != null) {
            sales = sales.stream()
                    .filter(sale -> branch.equals(sale.getBranch()))
                    .collect(Collectors.toList());
        }

        if (sales.isEmpty()) {
            return new SalesAggregates(0, 0.0, "N/A", "N/A");
        }

        // Calcular total units
        int totalUnits = sales.stream().mapToInt(Sale::getUnits).sum();

        // Calcular total revenue
        double totalRevenue = sales.stream()
                .mapToDouble(sale -> sale.getUnits() * sale.getPrice())
                .sum();

        // Encontrar top SKU
        String topSku = sales.stream()
                .collect(Collectors.groupingBy(Sale::getSku, Collectors.summingInt(Sale::getUnits)))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("N/A");

        // Encontrar top branch
        String topBranch = sales.stream()
                .collect(Collectors.groupingBy(Sale::getBranch, Collectors.summingInt(Sale::getUnits)))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("N/A");

        return new SalesAggregates(totalUnits, totalRevenue, topSku, topBranch);
    }

    public static class SalesAggregates {
        private final int totalUnits;
        private final double totalRevenue;
        private final String topSku;
        private final String topBranch;

        public SalesAggregates(int totalUnits, double totalRevenue, String topSku, String topBranch) {
            this.totalUnits = totalUnits;
            this.totalRevenue = totalRevenue;
            this.topSku = topSku;
            this.topBranch = topBranch;
        }

        // Getters
        public int getTotalUnits() { return totalUnits; }
        public double getTotalRevenue() { return totalRevenue; }
        public String getTopSku() { return topSku; }
        public String getTopBranch() { return topBranch; }
    }
}