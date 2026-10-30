package com.panha.markmekh.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class PublishPayslipDto {

    /** ALL = everyone in this period, SINGLE = one employee, FILTER = by name/department */
    @NotBlank
    private String mode;

    /** Used when mode=SINGLE */
    private Long employeeId;

    /** Used when mode=FILTER — matches employee name/code substring */
    private String keyword;
}