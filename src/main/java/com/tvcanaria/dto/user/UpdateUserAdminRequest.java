package com.tvcanaria.dto.user;

import com.tvcanaria.enums.Role;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

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
public class UpdateUserAdminRequest {

    @NotBlank
    public String firstName;

    @NotBlank
    public String lastName;

    @NotBlank
    public String username;

    @NotBlank
    @Email
    public String email;

    @NotNull
    public Role role;
}
