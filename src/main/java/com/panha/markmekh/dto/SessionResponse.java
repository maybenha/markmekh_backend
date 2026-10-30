package com.panha.markmekh.dto;

import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SessionResponse {
    private Long id;
    private String sessionToken;
    private String purpose;
    private String branchName;
    private LocalDateTime startTime;
    private LocalDateTime expirationTime;
    private boolean active;
    private boolean revoked;
}