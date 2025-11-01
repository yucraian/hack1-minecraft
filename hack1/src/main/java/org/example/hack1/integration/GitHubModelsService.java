package org.example.hack1.integration;

import lombok.extern.slf4j.Slf4j;
import org.example.hack1.sale.domain.SalesAggregationService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class GitHubModelsService {

    @Value("${github.models.url:https://api.github.com/models}")
    private String modelsUrl;

    @Value("${github.token:test-token}")
    private String githubToken;

    @Value("${model.id:openai/gpt-4}")
    private String modelId;

    private final RestTemplate restTemplate = new RestTemplate();

    public String generateSummary(SalesAggregationService.SalesAggregates aggregates) {
        try {
            // Si no hay token configurado, usar fallback inmediatamente
            if (githubToken == null || githubToken.equals("test-token") || githubToken.isEmpty()) {
                log.warn("GitHub Token no configurado, usando fallback");
                return createFallbackSummary(aggregates);
            }

            String prompt = String.format(
                    "Como analista de ventas de Oreo, genera un resumen ejecutivo de máximo 120 palabras en español " +
                            "basado en estos datos: Vendimos %d unidades, generamos $%.2f en revenue, " +
                            "el producto más vendido fue %s y la sucursal líder fue %s. " +
                            "Sé profesional y conciso para un email ejecutivo.",
                    aggregates.getTotalUnits(), aggregates.getTotalRevenue(),
                    aggregates.getTopSku(), aggregates.getTopBranch()
            );

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + githubToken);
            headers.set("Accept", "application/json");
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> requestBody = Map.of(
                    "model", modelId,
                    "messages", List.of(
                            Map.of("role", "system", "content", "Eres un analista senior de ventas en la empresa Oreo. Escribes resúmenes ejecutivos claros, profesionales y accionables."),
                            Map.of("role", "user", "content", prompt)
                    ),
                    "max_tokens", 250,
                    "temperature", 0.7
            );

            log.info("Enviando solicitud a GitHub Models para resumen...");
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    modelsUrl, HttpMethod.POST, entity, Map.class);

            String summary = extractResponseText(response.getBody());
            log.info("Resumen generado exitosamente por LLM");
            return summary;

        } catch (Exception e) {
            log.error("Error llamando a GitHub Models: {}", e.getMessage());
            log.info("Usando resumen de fallback...");
            return createFallbackSummary(aggregates);
        }
    }

    private String extractResponseText(Map<String, Object> response) {
        try {
            // Estructura esperada de GitHub Models API:
            // {
            //   "choices": [
            //     {
            //       "message": {
            //         "content": "Texto del resumen..."
            //       }
            //     }
            //   ]
            // }

            if (response == null) {
                throw new IllegalArgumentException("Respuesta nula de GitHub Models");
            }

            List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
            if (choices == null || choices.isEmpty()) {
                throw new IllegalArgumentException("No hay choices en la respuesta");
            }

            Map<String, Object> firstChoice = choices.get(0);
            Map<String, Object> message = (Map<String, Object>) firstChoice.get("message");

            if (message == null) {
                throw new IllegalArgumentException("No hay message en la choice");
            }

            String content = (String) message.get("content");
            if (content == null || content.trim().isEmpty()) {
                throw new IllegalArgumentException("Contenido vacío en la respuesta");
            }

            return content.trim();

        } catch (Exception e) {
            log.warn("Error extrayendo texto de respuesta LLM: {}. Usando fallback simple.", e.getMessage());
            // Fallback más inteligente basado en la respuesta cruda
            return response != null ? "Resumen IA: " + response.toString().substring(0, Math.min(100, response.toString().length())) + "..."
                    : "Resumen generado automáticamente con los datos proporcionados.";
        }
    }

    private String createFallbackSummary(SalesAggregationService.SalesAggregates aggregates) {
        return String.format(
                "📊 RESUMEN EJECUTIVO OREO\n\n" +
                        "Esta semana alcanzamos excelentes resultados con %d unidades vendidas y un revenue total de $%.2f. " +
                        "El producto estrella fue %s, demostrando su popularidad entre nuestros clientes. " +
                        "La sucursal de %s se destacó como la más performante, liderando las ventas. " +
                        "Estos números reflejan un sólido desempeño comercial y una gran aceptación de nuestros productos en el mercado.\n\n" +
                        "¡Seguimos trabajando para mantener este impulso!",
                aggregates.getTotalUnits(), aggregates.getTotalRevenue(),
                aggregates.getTopSku(), aggregates.getTopBranch()
        );
    }
}