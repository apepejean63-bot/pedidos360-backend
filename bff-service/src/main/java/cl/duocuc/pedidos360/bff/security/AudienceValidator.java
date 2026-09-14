package cl.duocuc.pedidos360.bff.security;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

public class AudienceValidator implements OAuth2TokenValidator<Jwt> {

    private final String audienciaEsperada;

    public AudienceValidator(String audienciaEsperada) {
        this.audienciaEsperada = audienciaEsperada;
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        if (jwt.getAudience() != null && jwt.getAudience().contains(audienciaEsperada)) {
            return OAuth2TokenValidatorResult.success();
        }
        OAuth2Error error = new OAuth2Error(
                "invalid_token",
                "El token no contiene el audience esperado (" + audienciaEsperada + ")",
                null
        );
        return OAuth2TokenValidatorResult.failure(error);
    }
}
