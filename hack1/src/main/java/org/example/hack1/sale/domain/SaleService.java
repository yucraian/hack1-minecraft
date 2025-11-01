package org.example.hack1.sale.domain;

import lombok.RequiredArgsConstructor;
import org.example.hack1.sale.dto.SaleRequestDto;
import org.example.hack1.sale.dto.SaleResponseDto;
import org.example.hack1.sale.repo.SaleRepository;
import org.example.hack1.user.domain.User;
import org.example.hack1.user.repo.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SaleService {

    private final SaleRepository saleRepository;
    private final UserRepository userRepository;

    public SaleResponseDto createSale(SaleRequestDto request, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        // Validar que usuarios BRANCH solo creen ventas en su sucursal
        if (user.getUserRole().name().equals("BRANCH") && !user.getBranch().equals(request.getBranch())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No puedes crear ventas en otra sucursal");
        }

        Sale sale = new Sale();
        sale.setSku(request.getSku());
        sale.setUnits(request.getUnits());
        sale.setPrice(request.getPrice());
        sale.setBranch(request.getBranch());
        sale.setSoldAt(request.getSoldAt() != null ? request.getSoldAt() : LocalDateTime.now());
        sale.setCreatedBy(user);

        Sale savedSale = saleRepository.save(sale);
        return mapToResponseDto(savedSale);
    }

    public SaleResponseDto getSaleById(Long id) {
        Sale sale = saleRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Venta no encontrada"));
        return mapToResponseDto(sale);
    }

    public Page<SaleResponseDto> getSales(String branch, LocalDate from, LocalDate to, Pageable pageable) {
        LocalDateTime startDate = from != null ? from.atStartOfDay() : null;
        LocalDateTime endDate = to != null ? to.atTime(LocalTime.MAX) : null;

        Page<Sale> sales;
        if (branch != null && startDate != null && endDate != null) {
            sales = saleRepository.findByBranchAndSoldAtBetween(branch, startDate, endDate, pageable);
        } else if (branch != null) {
            sales = saleRepository.findByBranch(branch, pageable);
        } else if (startDate != null && endDate != null) {
            sales = saleRepository.findBySoldAtBetween(startDate, endDate, pageable);
        } else {
            sales = saleRepository.findAll(pageable);
        }

        return sales.map(this::mapToResponseDto);
    }

    public SaleResponseDto updateSale(Long id, SaleRequestDto request) {
        Sale existingSale = saleRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Venta no encontrada"));

        existingSale.setSku(request.getSku());
        existingSale.setUnits(request.getUnits());
        existingSale.setPrice(request.getPrice());
        existingSale.setBranch(request.getBranch());
        if (request.getSoldAt() != null) {
            existingSale.setSoldAt(request.getSoldAt());
        }

        Sale updatedSale = saleRepository.save(existingSale);
        return mapToResponseDto(updatedSale);
    }

    public void deleteSale(Long id) {
        if (!saleRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Venta no encontrada");
        }
        saleRepository.deleteById(id);
    }

    public List<Sale> findSalesByDateRangeAndBranch(LocalDate from, LocalDate to, String branch) {
        LocalDateTime startDate = from.atStartOfDay();
        LocalDateTime endDate = to.atTime(LocalTime.MAX);

        if (branch != null) {
            return saleRepository.findByBranchAndSoldAtBetween(branch, startDate, endDate);
        } else {
            return saleRepository.findBySoldAtBetween(startDate, endDate);
        }
    }

    public String generateWeeklySummaryAsync(Object request) {
        // TODO: Implementar lógica asíncrona
        return "req_" + System.currentTimeMillis();
    }

    private SaleResponseDto mapToResponseDto(Sale sale) {
        return SaleResponseDto.builder()
                .id(sale.getId())
                .sku(sale.getSku())
                .units(sale.getUnits())
                .price(sale.getPrice())
                .branch(sale.getBranch())
                .soldAt(sale.getSoldAt())
                .createdBy(sale.getCreatedBy().getUsername())
                .build();
    }
}