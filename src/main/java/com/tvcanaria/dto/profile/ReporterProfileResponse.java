package com.tvcanaria.dto.profile;

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
public class ReporterProfileResponse {

    private String username;

    private String firstName;

    private String lastName;

    private LocalDateTime createdAt;
}
