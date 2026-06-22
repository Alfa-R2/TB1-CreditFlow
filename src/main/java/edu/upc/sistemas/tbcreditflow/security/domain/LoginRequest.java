package edu.upc.sistemas.tbcreditflow.security.domain;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank String username,
        @NotBlank String password
) {
}
