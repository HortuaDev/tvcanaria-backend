package com.tvcanaria.dto.moderator;

import com.tvcanaria.entity.ModeratorReporter;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
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
}