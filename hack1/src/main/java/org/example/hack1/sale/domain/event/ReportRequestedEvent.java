package org.example.hack1.sale.domain.event;

import java.time.LocalDate;

public class ReportRequestedEvent {
    private final LocalDate from;
    private final LocalDate to;
    private final String branch;
    private final String emailTo;
    private final String requestId;

    public ReportRequestedEvent(LocalDate from, LocalDate to, String branch, String emailTo, String requestId) {
        this.from = from;
        this.to = to;
        this.branch = branch;
        this.emailTo = emailTo;
        this.requestId = requestId;
    }

    // Getters
    public LocalDate getFrom() { return from; }
    public LocalDate getTo() { return to; }
    public String getBranch() { return branch; }
    public String getEmailTo() { return emailTo; }
    public String getRequestId() { return requestId; }
}