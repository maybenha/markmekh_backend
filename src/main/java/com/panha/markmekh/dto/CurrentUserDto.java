package com.panha.markmekh.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CurrentUserDto {
    private Long id;
    private String username;
    private String email;
    private String role;
    private boolean hasEmployeeProfile;
    private String employeeCode;
    private String fullNameEn;
    private String fullNameKh;
    private String phoneNumber;
}