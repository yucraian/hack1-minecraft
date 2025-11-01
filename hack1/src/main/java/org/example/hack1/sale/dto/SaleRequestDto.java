package org.example.hack1.sale.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class SaleRequestDto {

    @NotBlank(message = "El SKU es obligatorio")
    private String sku;

    @NotNull(message = "Las unidades son obligatorias")
    @Min(value = 1, message = "Las unidades deben ser al menos 1")
    private Integer units;

    @NotNull(message = "El precio es obligatorio")
    @Positive(message = "El precio debe ser positivo")
    private Double price;

    @NotBlank(message = "La sucursal es obligatoria")
    private String branch;

    private LocalDateTime soldAt;
}
