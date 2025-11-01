package org.example.hack1.sale.application;

import jakarta.validation.Valid;
import org.example.hack1.sale.domain.event.ReportRequestedEvent;
import org.example.hack1.sale.domain.SaleService;
import org.example.hack1.sale.dto.SaleRequestDto;
import org.example.hack1.sale.dto.SaleResponseDto;
import org.example.hack1.security.sec.SalesPermissionService;
import org.example.hack1.security.sec.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/sales")
public class SaleController {

    @Autowired
    private SaleService saleService;

    @Autowired
    private SecurityUtils securityUtils;

    @Autowired
    private SalesPermissionService permissionService;

    @Autowired
    private ApplicationEventPublisher eventPublisher;


    // CREATE - Crear nueva venta
    @PostMapping
    @PreAuthorize("hasAnyRole('CENTRAL', 'BRANCH')")
    public ResponseEntity<SaleResponseDto> createSale(@Valid @RequestBody SaleRequestDto request) {
        // Validar permisos usando tu SalesPermissionService
        permissionService.validateSaleCreation(request.getBranch());

        String username = securityUtils.getCurrentUsername();
        SaleResponseDto createdSale = saleService.createSale(request, username);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdSale);
    }

    // READ - Obtener venta por ID
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('CENTRAL', 'BRANCH')")
    public ResponseEntity<SaleResponseDto> getSale(@PathVariable Long id) {
        SaleResponseDto sale = saleService.getSaleById(id);

        // Validar permisos usando tu SalesPermissionService
        permissionService.validateBranchAccess(sale.getBranch());

        return ResponseEntity.ok(sale);
    }

    // READ - Listar ventas con filtros
    @GetMapping
    @PreAuthorize("hasAnyRole('CENTRAL', 'BRANCH')")
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

        // Validar acceso si se especifica branch
        if (branch != null) {
            permissionService.validateBranchAccess(branch);
        }

        Pageable pageable = PageRequest.of(page, size);
        Page<SaleResponseDto> sales = saleService.getSales(branch, from, to, pageable);
        return ResponseEntity.ok(sales);
    }

    // UPDATE - Actualizar venta
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('CENTRAL', 'BRANCH')")
    public ResponseEntity<SaleResponseDto> updateSale(
            @PathVariable Long id,
            @Valid @RequestBody SaleRequestDto request) {

        // Primero obtener la venta para validar permisos
        SaleResponseDto existingSale = saleService.getSaleById(id);

        // Validar permisos sobre la venta existente
        permissionService.validateBranchAccess(existingSale.getBranch());

        // Validar que usuarios BRANCH no cambien la sucursal
        if (!securityUtils.isCentralUser() && request.getBranch() != null &&
                !request.getBranch().equals(existingSale.getBranch())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "No puedes cambiar la sucursal de la venta");
        }

        SaleResponseDto updatedSale = saleService.updateSale(id, request);
        return ResponseEntity.ok(updatedSale);
    }

    // DELETE - Eliminar venta (solo CENTRAL)
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('CENTRAL')")
    public ResponseEntity<Void> deleteSale(@PathVariable Long id) {
        saleService.deleteSale(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/summary/weekly")
    @PreAuthorize("hasAnyRole('CENTRAL', 'BRANCH')")
    public ResponseEntity<?> generateWeeklySummary(@Valid @RequestBody WeeklySummaryRequest request) {

        // Validar permisos de branch
        String branch = request.getBranch();
        if (!securityUtils.isCentralUser()) {
            branch = securityUtils.getCurrentUserBranch();
            request.setBranch(branch); // Asegurar que use la branch del usuario
        }

        // Validar acceso a la branch solicitada
        if (branch != null) {
            permissionService.validateBranchAccess(branch);
        }

        // Validar email obligatorio
        if (request.getEmailTo() == null || request.getEmailTo().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "emailTo es obligatorio");
        }

        // === AQUÍ VA EL NUEVO CÓDIGO ===
        String requestId = "req_" + System.currentTimeMillis();

        // Calcular fechas por defecto si no vienen
        LocalDate fromDate = request.getFrom() != null ? request.getFrom() : LocalDate.now().minusDays(7);
        LocalDate toDate = request.getTo() != null ? request.getTo() : LocalDate.now();

        // Publicar evento asíncrono
        eventPublisher.publishEvent(new ReportRequestedEvent(
                fromDate, toDate, branch, request.getEmailTo(), requestId
        ));
        // === FIN DEL NUEVO CÓDIGO ===

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

        @jakarta.validation.constraints.NotBlank
        @jakarta.validation.constraints.Email
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