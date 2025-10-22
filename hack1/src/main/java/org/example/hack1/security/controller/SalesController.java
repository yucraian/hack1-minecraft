package controller;


import jakarta.validation.Valid;
import org.example.hack1.sale.domain.SaleService;
import org.example.hack1.sale.dto.SaleRequestDto;
import org.example.hack1.sale.dto.SaleResponseDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import security.SalesPermissionService;
import security.SecurityUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/sales")
public class SalesController {

    @Autowired
    private SaleService salesService;

    @Autowired
    private SalesPermissionService permissionService;

    @Autowired
    private SecurityUtils securityUtils;

    // CREATE - Crear nueva venta
    @PostMapping
    @PreAuthorize("hasAnyRole('ROLE_CENTRAL', 'ROLE_BRANCH')")
    public ResponseEntity<SaleResponseDto> createSale(@Valid @RequestBody SaleRequestDto request) {
        // Validar permisos de creación
        permissionService.validateSaleCreation(request.getBranch());

        // Validar acceso a la sucursal
        permissionService.validateBranchAccess(request.getBranch());

        SaleResponseDto createdSale = salesService.createSale(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdSale);
    }

    // READ - Obtener venta por ID
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ROLE_CENTRAL', 'ROLE_BRANCH')")
    public ResponseEntity<SaleResponseDto> getSale(@PathVariable String id) {
        SaleResponseDto sale = salesService.getSaleById(id);

        // Validar que el usuario tenga acceso a esta venta
        permissionService.validateBranchAccess(sale.getBranch());

        return ResponseEntity.ok(sale);
    }

    // READ - Listar ventas con filtros
    @GetMapping
    @PreAuthorize("hasAnyRole('ROLE_CENTRAL', 'ROLE_BRANCH')")
    public ResponseEntity<Page<SaleResponseDto>> getSales(
            @RequestParam(required = false) String branch,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        // Si es usuario BRANCH, solo puede ver su sucursal
        if (!securityUtils.isCentralUser()) {
            branch = securityUtils.getCurrentUserBranch();
        }

        // Si se especifica branch, validar acceso
        if (branch != null) {
            permissionService.validateBranchAccess(branch);
        }

        Pageable pageable = PageRequest.of(page, size);
        Page<SaleResponseDto> sales = salesService.getSales(branch, from, to, pageable);

        return ResponseEntity.ok(sales);
    }

    // UPDATE - Actualizar venta
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ROLE_CENTRAL', 'ROLE_BRANCH')")
    public ResponseEntity<SaleResponseDto> updateSale(
            @PathVariable String id,
            @Valid @RequestBody SaleRequestDto request) {

        // Primero obtener la venta para validar permisos
        SaleResponseDto existingSale = salesService.getSaleById(id);
        permissionService.validateBranchAccess(existingSale.getBranch());

        // Validar permisos para la nueva branch si se cambia
        if (request.getBranch() != null && !request.getBranch().equals(existingSale.getBranch())) {
            permissionService.validateSaleCreation(request.getBranch());
            permissionService.validateBranchAccess(request.getBranch());
        }

        SaleResponseDto updatedSale = salesService.updateSale(id, request);
        return ResponseEntity.ok(updatedSale);
    }

    // DELETE - Eliminar venta (solo CENTRAL)
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_CENTRAL')")
    public ResponseEntity<Void> deleteSale(@PathVariable String id) {
        salesService.deleteSale(id);
        return ResponseEntity.noContent().build();
    }

    // Endpoint para resumen semanal (ASÍNCRONO)
    @PostMapping("/summary/weekly")
    @PreAuthorize("hasAnyRole('ROLE_CENTRAL', 'ROLE_BRANCH')")
    public ResponseEntity<?> generateWeeklySummary(
            @RequestBody WeeklySummaryRequest request) {

        // Validar permisos de branch
        if (request.getBranch() != null) {
            permissionService.validateBranchAccess(request.getBranch());
        } else if (!securityUtils.isCentralUser()) {
            // Si es BRANCH y no especifica branch, usar su branch
            request.setBranch(securityUtils.getCurrentUserBranch());
        }

        // Validar email obligatorio
        if (request.getEmailTo() == null || request.getEmailTo().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "emailTo es obligatorio");
        }

        // Procesar asíncronamente (esto lo implementarás después)
        String requestId = salesService.generateWeeklySummaryAsync(request);

        // Response inmediata
        SummaryResponse response = new SummaryResponse();
        response.setRequestId(requestId);
        response.setStatus("PROCESSING");
        response.setMessage("Su solicitud de reporte está siendo procesada. Recibirá el resumen en " + request.getEmailTo() + " en unos momentos.");
        response.setEstimatedTime("30-60 segundos");
        response.setRequestedAt(LocalDateTime.now());

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    // Clases internas para DTOs específicos del controlador
    public static class WeeklySummaryRequest {
        private LocalDate from;
        private LocalDate to;
        private String branch;
        private String emailTo;

        // Getters and Setters
        public LocalDate getFrom() { return from; }
        public void setFrom(LocalDate from) { this.from = from; }

        public LocalDate getTo() { return to; }
        public void setTo(LocalDate to) { this.to = to; }

        public String getBranch() { return branch; }
        public void setBranch(String branch) { this.branch = branch; }

        public String getEmailTo() { return emailTo; }
        public void setEmailTo(String emailTo) { this.emailTo = emailTo; }
    }

    public static class SummaryResponse {
        private String requestId;
        private String status;
        private String message;
        private String estimatedTime;
        private LocalDateTime requestedAt;

        // Getters and Setters
        public String getRequestId() { return requestId; }
        public void setRequestId(String requestId) { this.requestId = requestId; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public String getEstimatedTime() { return estimatedTime; }
        public void setEstimatedTime(String estimatedTime) { this.estimatedTime = estimatedTime; }

        public LocalDateTime getRequestedAt() { return requestedAt; }
        public void setRequestedAt(LocalDateTime requestedAt) { this.requestedAt = requestedAt; }
    }
}