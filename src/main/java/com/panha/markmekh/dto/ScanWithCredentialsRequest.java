package com.panha.markmekh.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class ScanWithCredentialsRequest {

    @NotBlank(message = "Session token is required")
    private String sessionToken;

    @NotBlank(message = "Username is required")
    private String username;

    @NotBlank(message = "Password is required")
    private String password;

    private Double latitude;
    private Double longitude;
    private String method; // QR / FACE
}