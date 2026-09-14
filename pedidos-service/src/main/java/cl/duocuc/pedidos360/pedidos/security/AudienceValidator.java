package cl.duocuc.pedidos360.pedidos.security;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Valida que el JWT recibido haya sido emitido especificamente para esta API
 * (evita que un token valido de OTRA aplicacion registrada en el mismo tenant
 * de Azure AD sea aceptado aqui).
 */
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
