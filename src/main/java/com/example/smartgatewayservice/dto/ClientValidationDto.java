package com.example.smartgatewayservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClientValidationDto {
    private String username;
    private Integer rateLimitPerMinute;
    private Boolean isActive;
}
