package com.example.smartgatewayservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RouteDefinitionDto {
    private String routeId;
    private String pathPattern;
    private String targetUrl;
}
