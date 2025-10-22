package org.example.hack1.sale.domain;


import lombok.RequiredArgsConstructor;
import org.example.hack1.sale.dto.SaleRequestDto;
import org.example.hack1.sale.dto.SaleResponseDto;
import org.example.hack1.sale.repo.SaleRepository;
import org.example.hack1.user.domain.User;
import org.example.hack1.user.repo.UserRepository;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SaleService {

    private final SaleRepository saleRepository;
    private final UserRepository userRepository;
    private final ModelMapper modelMapper;

    public SaleResponseDto createSale(SaleRequestDto request, String username) {

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        if (user.getUserRole().name().equals("BRANCH") && !user.getBranch().equals(request.getBranch())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No puedes crear ventas en otra sucursal");
        }

        Sale sale = modelMapper.map(request, Sale.class);
        sale.setCreatedBy(user);

        if (sale.getSoldAt() == null) {
            sale.setSoldAt(LocalDateTime.now());
        }

        Sale saved = saleRepository.save(sale);

        SaleResponseDto response = modelMapper.map(saved, SaleResponseDto.class);
        response.setCreatedBy(user.getUsername());

        return response;
    }

    public List<SaleResponseDto> listSales(String branch) {
        List<Sale> sales = (branch == null)
                ? saleRepository.findAll()
                : saleRepository.findByBranch(branch);

        return sales.stream()
                .map(sale -> {
                    SaleResponseDto dto = modelMapper.map(sale, SaleResponseDto.class);
                    dto.setCreatedBy(sale.getCreatedBy().getUsername());
                    return dto;
                })
                .collect(Collectors.toList());
    }

    public SaleResponseDto getSale(Long id) {
        Sale sale = saleRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Venta no encontrada"));

        SaleResponseDto dto = modelMapper.map(sale, SaleResponseDto.class);
        dto.setCreatedBy(sale.getCreatedBy().getUsername());
        return dto;
    }

    public void deleteSale(Long id) {
        if (!saleRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Venta no encontrada");
        }
        saleRepository.deleteById(id);
    }
}