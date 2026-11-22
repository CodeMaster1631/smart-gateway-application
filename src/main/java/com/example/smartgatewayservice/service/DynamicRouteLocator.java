package com.example.smartgatewayservice.service;

import com.example.smartgatewayservice.dto.RouteDefinitionDto;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.gateway.handler.predicate.PredicateDefinition;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionLocator;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.net.URI;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DynamicRouteLocator implements RouteDefinitionLocator {
    private final WebClient.Builder webClientBuilder;

    @Override
    public Flux<RouteDefinition> getRouteDefinitions() {
        return webClientBuilder.build()
                .get()
                .uri("http://localhost:8081/api/admin/routes/active")
                .retrieve()
                .bodyToFlux(RouteDefinitionDto.class)
                .map(dto -> {
                    RouteDefinition routeDefinition = new RouteDefinition();
                    routeDefinition.setId(dto.getRouteId());
                    routeDefinition.setUri(URI.create(dto.getTargetUrl()));

                    PredicateDefinition predicate1 = new PredicateDefinition("Path=" + dto.getPathPattern());
                    routeDefinition.setPredicates((List.of(predicate1)));

                    PredicateDefinition predicate2 = new PredicateDefinition("Method=" + dto.getAllowedMethods());
                    routeDefinition.setPredicates((List.of(predicate2)));

                    Map<String, Object> metadata = Map.of(
                            "authRequired", dto.getAuthRequired(),
                            "authType", dto.getAuthType(),
                            "rateLimitPerMinute", dto.getRateLimitPerMinute()
                    );

                    routeDefinition.setMetadata(metadata);

                    return routeDefinition;
                });
    }
}
