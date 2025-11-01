package org.example.hack1.integration;

import lombok.extern.slf4j.Slf4j;
import org.example.hack1.sale.domain.SalesAggregationService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Slf4j
@Service
public class GitHubModelsService {

    @Value("${github.models.url}")
    private String modelsUrl;

    @Value("${github.token}")
    private String githubToken;

    @Value("${model.id}")
    private String modelId;

    private final RestTemplate restTemplate = new RestTemplate();

    public String generateSummary(SalesAggregationService.SalesAggregates aggregates) {
        try {
            String prompt = String.format(
                    "Con estos datos de ventas de Oreo: totalUnits=%d, totalRevenue=%.2f, topSku=%s, topBranch=%s. " +
                            "Devuelve un resumen ejecutivo de máximo 120 palabras en español para enviar por email. " +
                            "Sé conciso y profesional.",
                    aggregates.getTotalUnits(), aggregates.getTotalRevenue(),
                    aggregates.getTopSku(), aggregates.getTopBranch()
            );

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + githubToken);
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> requestBody = Map.of(
                    "model", modelId,
                    "messages", new Object[]{
                            Map.of("role", "system", "content", "Eres un analista de ventas que escribe resúmenes claros y profesionales."),
                            Map.of("role", "user", "content", prompt)
                    },
                    "max_tokens", 200
            );

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    modelsUrl, HttpMethod.POST, entity, Map.class);

            // Extraer el texto de la respuesta
            return extractResponseText(response.getBody());

        } catch (Exception e) {
            log.error("Error llamando a GitHub Models: {}", e.getMessage());
            return createFallbackSummary(aggregates);
        }
    }

    private String extractResponseText(Map<String, Object> response) {
        // Implementar extracción del texto de la respuesta del LLM
        return "Resumen generado por LLM: " + response.toString(); // Simplificado
    }

    private String createFallbackSummary(SalesAggregationService.SalesAggregates aggregates) {
        return String.format(
                "Resumen Semanal Oreo: Se vendieron %d unidades con un revenue total de $%.2f. " +
                        "El SKU más vendido fue %s y la sucursal líder fue %s.",
                aggregates.getTotalUnits(), aggregates.getTotalRevenue(),
                aggregates.getTopSku(), aggregates.getTopBranch()
        );
    }
}