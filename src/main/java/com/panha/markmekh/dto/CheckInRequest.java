package com.panha.markmekh.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.math.BigDecimal;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class CheckInRequest {
    private String sessionToken;
    private Double latitude;
    private Double longitude;
    private Double accuracyMeters;
    private String method;
}