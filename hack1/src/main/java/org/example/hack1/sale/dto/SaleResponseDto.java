package org.example.hack1.sale.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@Builder
public class SaleResponseDto {
    private Long id;
    private String sku;
    private Integer units;
    private Double price;
    private String branch;
    private LocalDateTime soldAt;
    private String createdBy;
}