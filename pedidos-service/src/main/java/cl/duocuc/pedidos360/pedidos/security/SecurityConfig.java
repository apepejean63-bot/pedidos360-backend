package cl.duocuc.pedidos360.pedidos.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity // habilita @PreAuthorize en los controllers
public class SecurityConfig {

    // Se inyecta desde application.yml -> viene de la variable de entorno AZURE_CLIENT_ID_BACKEND
    @Value("${azure.client-id}")
    private String audienciaEsperada;

    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
    private String issuerUri;

    @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}")
    private String jwkSetUri;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
            );

        return http.build();
    }

    /**
     * Decodificador de JWT: descarga las llaves publicas (JWKS) del tenant de Azure AD,
     * valida firma, vigencia (exp/nbf), issuer, y ademas el audience via AudienceValidator.
     */
    @Bean
    public JwtDecoder jwtDecoder() {
        // withJwkSetUri es "lazy": no llama a la red al levantar el contexto de Spring,
        // solo cuando llega el primer token a validar. Esto evita que la app falle al
        // arrancar (o que los tests fallen) si el tenant aun no existe o no hay red.
        NimbusJwtDecoder jwtDecoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();

        OAuth2TokenValidator<Jwt> validadorIssuer = JwtValidators.createDefaultWithIssuer(issuerUri);
        OAuth2TokenValidator<Jwt> validadorAudience = new AudienceValidator(audienciaEsperada);
        OAuth2TokenValidator<Jwt> validadorCombinado =
                new DelegatingOAuth2TokenValidator<>(validadorIssuer, validadorAudience);

        jwtDecoder.setJwtValidator(validadorCombinado);
        return jwtDecoder;
    }

    /**
     * Extrae los roles del claim "roles" (App Roles de Azure AD) y los mapea
     * a GrantedAuthority con prefijo ROLE_, para que funcione hasRole('Admin').
     */
    private JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authoritiesConverter = new JwtGrantedAuthoritiesConverter();
        authoritiesConverter.setAuthoritiesClaimName("roles");
        authoritiesConverter.setAuthorityPrefix("ROLE_");

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);
        return converter;
    }
}
