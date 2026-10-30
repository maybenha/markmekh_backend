package com.panha.markmekh.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class CreateSessionRequest {

    private Long branchId;

    @NotBlank
    private String purpose; // CHECK_IN, CHECK_OUT, BOTH

    private LocalDateTime startTime;
    private LocalDateTime expirationTime;
}