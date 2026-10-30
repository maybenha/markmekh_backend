package com.panha.markmekh.dto;

import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class NotificationDto {
    private Long id;
    private String type;
    private String title;
    private String message;
    private Long referenceId;
    private boolean read;
    private LocalDateTime createdAt;

    private String profilePhotoUrl;
}