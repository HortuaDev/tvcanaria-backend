package com.tvcanaria.dto.moderator;

import com.tvcanaria.entity.ModeratorReporter;
import java.time.LocalDateTime;

public class ModeratorResponse {

    private Integer id;
    private Integer reporterId;
    private String reporterUsername;
    private String reporterFirstName;
    private String reporterLastName;
    private String reporterEmail;
    private String status;
    private LocalDateTime createdAt;

    public ModeratorResponse(ModeratorReporter r) {
        this.id = r.getId();
        this.reporterId = r.getReporter().getUserId();
        this.reporterUsername = r.getReporter().getUsername();
        this.reporterFirstName = r.getReporter().getFirstName();
        this.reporterLastName = r.getReporter().getLastName();
        this.reporterEmail = r.getReporter().getEmail();
        this.status = r.getStatus().name();
        this.createdAt = r.getCreatedAt();
    }

    public Integer getId() {
        return id;
    }

    public Integer getReporterId() {
        return reporterId;
    }

    public String getReporterUsername() {
        return reporterUsername;
    }

    public String getReporterFirstName() {
        return reporterFirstName;
    }

    public String getReporterLastName() {
        return reporterLastName;
    }

    public String getReporterEmail() {
        return reporterEmail;
    }

    public String getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}