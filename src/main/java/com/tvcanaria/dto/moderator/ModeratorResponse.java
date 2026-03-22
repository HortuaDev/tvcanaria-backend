package com.tvcanaria.dto.moderator;

import com.tvcanaria.entity.ModeratorReporter;
import java.time.LocalDateTime;

public class ModeratorResponse {

    private Integer id;

    // Reporter (quien envió la solicitud)
    private Integer reporterId;
    private String reporterUsername;
    private String reporterFirstName;
    private String reporterLastName;
    private String reporterEmail;

    // Moderator (a quien se le envió la solicitud)
    private Integer moderatorId;
    private String moderatorUsername;
    private String moderatorFirstName;
    private String moderatorLastName;
    private String moderatorEmail;

    private String status;
    private LocalDateTime createdAt;

    public ModeratorResponse(ModeratorReporter r) {
        this.id = r.getId();

        this.reporterId = r.getReporter().getUserId();
        this.reporterUsername = r.getReporter().getUsername();
        this.reporterFirstName = r.getReporter().getFirstName();
        this.reporterLastName = r.getReporter().getLastName();
        this.reporterEmail = r.getReporter().getEmail();

        this.moderatorId = r.getModerator().getUserId();
        this.moderatorUsername = r.getModerator().getUsername();
        this.moderatorFirstName = r.getModerator().getFirstName();
        this.moderatorLastName = r.getModerator().getLastName();
        this.moderatorEmail = r.getModerator().getEmail();

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

    public Integer getModeratorId() {
        return moderatorId;
    }

    public String getModeratorUsername() {
        return moderatorUsername;
    }

    public String getModeratorFirstName() {
        return moderatorFirstName;
    }

    public String getModeratorLastName() {
        return moderatorLastName;
    }

    public String getModeratorEmail() {
        return moderatorEmail;
    }

    public String getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}