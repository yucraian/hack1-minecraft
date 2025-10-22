package org.example.hack1.sale.domain;


import lombok.RequiredArgsConstructor;
import org.example.hack1.sale.repo.SaleRepository;
import org.example.hack1.user.domain.User;
import org.example.hack1.user.repo.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SaleService {
    private final SaleRepository saleRepository;
    private final UserRepository userRepository;

    public Sale createSale(Sale sale, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
        if (user.getUserRole().name().equals("BRANCH") && !user.getBranch().equals(sale.getBranch())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No puedes crear ventas en otra sucursal");
        }

        sale.setCreatedBy(user);
        sale.setSoldAt(sale.getSoldAt() == null ? LocalDateTime.now() : sale.getSoldAt());

        return saleRepository.save(sale);
    }

    public List<Sale> listSales(String branch) {
        if (branch == null) return saleRepository.findAll();
        return saleRepository.findByBranch(branch);
    }

    public Sale getSale(Long id) {
        return saleRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Venta no encontrada"));
    }

    public void deleteSale(Long id) {
        if (!saleRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Venta no encontrada");
        }
        saleRepository.deleteById(id);
    }
}