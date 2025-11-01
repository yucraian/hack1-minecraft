package org.example.hack1.email;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.hack1.sale.domain.SalesAggregationService;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {


    private final JavaMailSender mailSender;

    public void sendReportEmail(String emailTo, LocalDate from, LocalDate to,
                                SalesAggregationService.SalesAggregates aggregates, String summary) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(emailTo);
            message.setSubject(String.format("Reporte Semanal Oreo - %s a %s", from, to));
            message.setText(buildEmailContent(aggregates, summary, from, to));

            mailSender.send(message);
            log.info("Email enviado exitosamente a: {}", emailTo);

        } catch (Exception e) {
            log.error("Error enviando email: {}", e.getMessage());
            throw new RuntimeException("Error enviando email", e);
        }
    }

    public void sendFallbackEmail(String emailTo, LocalDate from, LocalDate to,
                                  SalesAggregationService.SalesAggregates aggregates) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(emailTo);
        message.setSubject(String.format("Reporte Semanal Oreo - %s a %s", from, to));
        message.setText(buildFallbackContent(aggregates, from, to));

        mailSender.send(message);
    }

    private String buildEmailContent(SalesAggregationService.SalesAggregates aggregates,
                                     String summary, LocalDate from, LocalDate to) {
        return String.format(
                "Reporte Semanal Oreo\n" +
                        "Período: %s a %s\n\n" +
                        "%s\n\n" +
                        "Métricas Clave:\n" +
                        "- Unidades totales: %d\n" +
                        "- Revenue total: $%.2f\n" +
                        "- SKU más vendido: %s\n" +
                        "- Sucursal líder: %s\n\n" +
                        "¡Gracias por usar Oreo Insight Factory!",
                from, to, summary, aggregates.getTotalUnits(),
                aggregates.getTotalRevenue(), aggregates.getTopSku(), aggregates.getTopBranch()
        );
    }

    private String buildFallbackContent(SalesAggregationService.SalesAggregates aggregates,
                                        LocalDate from, LocalDate to) {
        return String.format(
                "Reporte Semanal Oreo\n" +
                        "Período: %s a %s\n\n" +
                        "Resumen: Se vendieron %d unidades con un revenue total de $%.2f. " +
                        "El SKU más vendido fue %s y la sucursal líder fue %s.\n\n" +
                        "Métricas Clave:\n" +
                        "- Unidades totales: %d\n" +
                        "- Revenue total: $%.2f\n" +
                        "- SKU más vendido: %s\n" +
                        "- Sucursal líder: %s",
                from, to, aggregates.getTotalUnits(), aggregates.getTotalRevenue(),
                aggregates.getTopSku(), aggregates.getTopBranch(),
                aggregates.getTotalUnits(), aggregates.getTotalRevenue(),
                aggregates.getTopSku(), aggregates.getTopBranch()
        );
    }
}