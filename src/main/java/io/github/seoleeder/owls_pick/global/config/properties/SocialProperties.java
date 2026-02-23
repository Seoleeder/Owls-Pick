package io.github.seoleeder.owls_pick.global.config.properties;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.Map;
import java.util.Set;

@Validated
@ConfigurationProperties(prefix = "spring.security.oauth2.client")
public record SocialProperties(
     Map<String, @Valid Registration> registration,
     Map<String, @Valid Provider> provider
){

    public record Registration(
        @NotBlank(message = "Client ID is required")
        String clientId,

        @NotBlank(message = "Client Secret is required ")
        String clientSecret,

        @NotBlank(message = "Redirect URI is required")
        String redirectUri,

        @NotNull(message = "Scope must not be empty")
        Set<String> scope,

        @NotBlank(message = "Authorization Grant Type is required")
        String authorizationGrantType,

        String clientAuthenticationMethod,

        String clientName
    ){
    }

    public record Provider (
        @NotBlank(message = "Authorization URI is required")
        String authorizationUri,

        @NotBlank(message = "Token URI is required")
        String tokenUri,

        @NotBlank(message = "JWK Set URI is required")
        String jwkSetUri,

        @NotBlank(message = "User Name Attribute is required")
        String userNameAttribute,

        String userInfoUri){
    }
}