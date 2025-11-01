package org.example.hack1.sale.domain;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.hack1.integration.GitHubModelsService;
import org.example.hack1.sale.domain.event.ReportRequestedEvent;
import org.example.hack1.email.EmailService;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReportEventListener {

    private final SalesAggregationService aggregationService;
    private final GitHubModelsService gitHubModelsService;
    private final EmailService emailService;

    @Async
    @EventListener
    public void handleReportRequest(ReportRequestedEvent event) {
        try {
            log.info("Procesando reporte asíncrono para: {}", event.getEmailTo());

            // 1. Calcular agregados
            SalesAggregationService.SalesAggregates aggregates =
                    aggregationService.calculateAggregates(event.getFrom(), event.getTo(), event.getBranch());

            // 2. Generar resumen con LLM
            String summary = gitHubModelsService.generateSummary(aggregates);

            // 3. Enviar email
            emailService.sendReportEmail(event.getEmailTo(), event.getFrom(), event.getTo(),
                    aggregates, summary);

            log.info("Reporte enviado exitosamente a: {}", event.getEmailTo());

        } catch (Exception e) {
            log.error("Error procesando reporte asíncrono: {}", e.getMessage(), e);
            // Fallback: enviar email con datos básicos sin LLM
            try {
                SalesAggregationService.SalesAggregates aggregates =
                        aggregationService.calculateAggregates(event.getFrom(), event.getTo(), event.getBranch());
                emailService.sendFallbackEmail(event.getEmailTo(), event.getFrom(), event.getTo(), aggregates);
            } catch (Exception ex) {
                log.error("Error incluso en fallback: {}", ex.getMessage());
            }
        }
    }
}