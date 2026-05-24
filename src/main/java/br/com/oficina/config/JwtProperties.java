package br.com.oficina.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "oficina.security.jwt")
public record JwtProperties(String secret, String issuer, long accessTokenTtlMinutes) {}
