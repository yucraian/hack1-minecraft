package org.example.hack1.SalesTest;

import org.example.hack1.sale.domain.Sale;
import org.example.hack1.sale.domain.SalesAggregationService;
import org.example.hack1.sale.repo.SaleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SalesAggregationServiceTest {

    @Mock
    private SaleRepository saleRepository;

    @InjectMocks
    private SalesAggregationService salesAggregationService;

    // Test 1: Agregados con datos válidos - CORREGIDO
    @Test
    void shouldCalculateCorrectAggregatesWithValidData() {
        // Given
        List<Sale> mockSales = List.of(
                createSale("OREO_CLASSIC", 10, 1.99, "Miraflores"),
                createSale("OREO_DOUBLE", 5, 2.49, "San Isidro"),
                createSale("OREO_CLASSIC", 15, 1.99, "Miraflores")
        );

        when(saleRepository.findSalesInDateRange(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(mockSales);

        // When
        SalesAggregationService.SalesAggregates result = salesAggregationService.calculateAggregates(
                LocalDate.now().minusDays(7), LocalDate.now(), null
        );

        // Then - CÁLCULOS CORREGIDOS:
        // OREO_CLASSIC: (10 + 15) * 1.99 = 25 * 1.99 = 49.75
        // OREO_DOUBLE: 5 * 2.49 = 12.45
        // Total: 49.75 + 12.45 = 62.20
        assertThat(result.getTotalUnits()).isEqualTo(30);
        assertThat(result.getTotalRevenue()).isEqualTo(62.20);
        assertThat(result.getTopSku()).isEqualTo("OREO_CLASSIC");
        assertThat(result.getTopBranch()).isEqualTo("Miraflores");
    }

    // Test 2: Lista vacía de ventas - CORREGIDO (ya estaba bien)
    @Test
    void shouldHandleEmptySalesList() {
        // Given
        List<Sale> mockSales = List.of();
        when(saleRepository.findSalesInDateRange(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(mockSales);

        // When
        SalesAggregationService.SalesAggregates result = salesAggregationService.calculateAggregates(
                LocalDate.now().minusDays(7), LocalDate.now(), null
        );

        // Then
        assertThat(result.getTotalUnits()).isEqualTo(0);
        assertThat(result.getTotalRevenue()).isEqualTo(0.0);
        assertThat(result.getTopSku()).isEqualTo("N/A");
        assertThat(result.getTopBranch()).isEqualTo("N/A");
    }

    // Test 3: Filtrado por sucursal - CORREGIDO
    @Test
    void shouldFilterByBranchCorrectly() {
        // Given
        List<Sale> mockSales = List.of(
                createSale("OREO_CLASSIC", 10, 1.99, "Miraflores"),
                createSale("OREO_DOUBLE", 5, 2.49, "San Isidro"),
                createSale("OREO_THINS", 8, 2.19, "Miraflores"),
                createSale("OREO_CLASSIC", 12, 1.99, "San Isidro")
        );

        when(saleRepository.findSalesInDateRange(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(mockSales);

        // When - Filtrar solo por Miraflores
        SalesAggregationService.SalesAggregates result = salesAggregationService.calculateAggregates(
                LocalDate.now().minusDays(7), LocalDate.now(), "Miraflores"
        );

        // Then - Solo debería considerar ventas de Miraflores - CÁLCULOS CORREGIDOS:
        // Miraflores: OREO_CLASSIC(10) + OREO_THINS(8) = 18 unidades
        // Revenue: (10 * 1.99) + (8 * 2.19) = 19.90 + 17.52 = 37.42
        assertThat(result.getTotalUnits()).isEqualTo(18);
        assertThat(result.getTotalRevenue()).isEqualTo(37.42);
        assertThat(result.getTopSku()).isEqualTo("OREO_CLASSIC");
        assertThat(result.getTopBranch()).isEqualTo("Miraflores");
    }

    // Test 4: Filtrado por fechas - CORREGIDO
    @Test
    void shouldConsiderOnlySalesInDateRange() {
        // Given - Ventas fuera del rango no deberían ser consideradas
        List<Sale> mockSales = List.of(
                createSale("OREO_CLASSIC", 10, 1.99, "Miraflores"),
                createSale("OREO_DOUBLE", 5, 2.49, "San Isidro")
        );

        when(saleRepository.findSalesInDateRange(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(mockSales);

        // When
        SalesAggregationService.SalesAggregates result = salesAggregationService.calculateAggregates(
                LocalDate.now().minusDays(7), LocalDate.now(), null
        );

        // Then - Solo las 2 ventas mockeadas - CÁLCULOS CORREGIDOS:
        // OREO_CLASSIC: 10 * 1.99 = 19.90
        // OREO_DOUBLE: 5 * 2.49 = 12.45
        // Total: 19.90 + 12.45 = 32.35
        assertThat(result.getTotalUnits()).isEqualTo(15);
        assertThat(result.getTotalRevenue()).isEqualTo(32.35);
        assertThat(result.getTopSku()).isEqualTo("OREO_CLASSIC");
        assertThat(result.getTopBranch()).isEqualTo("Miraflores");
    }

    // Test 5: Cálculo de SKU top con empates - CORREGIDO
    @Test
    void shouldIdentifyTopSkuCorrectlyWithTies() {
        // Given - OREO_CLASSIC tiene 15, OREO_DOUBLE tiene 15 (empate)
        List<Sale> mockSales = List.of(
                createSale("OREO_CLASSIC", 10, 1.99, "Miraflores"),
                createSale("OREO_DOUBLE", 15, 2.49, "San Isidro"),
                createSale("OREO_CLASSIC", 5, 1.99, "Miraflores"),
                createSale("OREO_THINS", 8, 2.19, "San Isidro")
        );

        when(saleRepository.findSalesInDateRange(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(mockSales);

        // When
        SalesAggregationService.SalesAggregates result = salesAggregationService.calculateAggregates(
                LocalDate.now().minusDays(7), LocalDate.now(), null
        );

        // Then - CÁLCULOS CORREGIDOS:
        // Total units: 10 + 15 + 5 + 8 = 38
        // Revenue: (15*1.99) + (15*2.49) + (8*2.19) = 29.85 + 37.35 + 17.52 = 84.72
        // Top SKU: OREO_CLASSIC (15) vs OREO_DOUBLE (15) - puede ser cualquiera
        // Top Branch: San Isidro (15 + 8 = 23) vs Miraflores (15)
        assertThat(result.getTotalUnits()).isEqualTo(38);
        assertThat(result.getTotalRevenue()).isEqualTo(84.72);
        assertThat(result.getTopSku()).isIn("OREO_CLASSIC", "OREO_DOUBLE");
        assertThat(result.getTopBranch()).isEqualTo("San Isidro");
    }

    // Método helper para crear ventas de prueba
    private Sale createSale(String sku, int units, double price, String branch) {
        Sale sale = new Sale();
        sale.setSku(sku);
        sale.setUnits(units);
        sale.setPrice(price);
        sale.setBranch(branch);
        sale.setSoldAt(LocalDateTime.now());
        return sale;
    }
}