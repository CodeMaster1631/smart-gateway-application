package com.example.smartgatewayservice.filter;

import com.example.smartgatewayservice.dto.ClientValidationDto;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class AuthenticationFilter implements GlobalFilter, Ordered {
    private final WebClient.Builder webClientBuilder;


    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        Route route = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);

        if (route == null) {
            return chain.filter(exchange);
        }

        Boolean authRequired = (Boolean) route.getMetadata().get("authRequired");
        String authType = (String) route.getMetadata().get("authType");

        if (authRequired == false || authType == null) {
            return chain.filter(exchange);
        }

        String apiKey = exchange.getRequest().getHeaders().getFirst("X-API-KEY");
        if (apiKey == null || apiKey.isEmpty()) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        return webClientBuilder.build()
                .get()
                .uri("http://localhost:8081/api/admin/clients/validate")
                .header("X-API-KEY", apiKey)
                .exchangeToMono(response -> {
                    if (response.statusCode().is2xxSuccessful()) {
                        return response.bodyToMono(ClientValidationDto.class)
                                .flatMap(client -> {
                                    if (!Boolean.TRUE.equals(client.getIsActive())) {
                                        return Mono.empty();
                                    }

                                    exchange.getAttributes().put("clientUsername", client.getUsername());
                                    exchange.getAttributes().put("clientRateLimit", client.getRateLimitPerMinute());

                                    return Mono.just(client);
                                });
                    }
                    return response.releaseBody().then(Mono.empty());
                })
                .onErrorResume(e -> {
                    exchange.getResponse().setStatusCode(HttpStatus.SERVICE_UNAVAILABLE);
                    return exchange.getResponse().setComplete().then(Mono.empty());
                })
                .flatMap(client -> chain.filter(exchange))
                .switchIfEmpty(unauthorized(exchange));
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(
                HttpStatus.UNAUTHORIZED
        );

        return exchange.getResponse().setComplete();
    }

    @Override
    public int getOrder() {
        return -2;
    }
}
