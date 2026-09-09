package br.unipar.trilha.security;

import br.unipar.trilha.entities.Usuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

@Component
public class JwtTokenProvider {
    private final JwtProperties properties;
    private final SecretKey key;

    public JwtTokenProvider(JwtProperties properties) {
        this.properties = properties;
        if (properties.secret() == null || properties.secret().getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalArgumentException("JWT_SECRET deve possuir ao menos 32 bytes.");
        }
        this.key = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
    }

    public String gerar(Usuario usuario) {
        Instant agora = Instant.now();
        Instant expiracao = agora.plus(properties.expirationMinutes(), ChronoUnit.MINUTES);
        return Jwts.builder()
                .subject(usuario.getLogin())
                .issuer(properties.issuer())
                .issuedAt(Date.from(agora))
                .expiration(Date.from(expiracao))
                .claim("usuarioId", usuario.getId())
                .claim("perfil", usuario.getPerfil().name())
                .signWith(key)
                .compact();
    }

    public String obterLogin(String token) {
        return claims(token).getSubject();
    }

    public boolean valido(String token) {
        try {
            Claims claims = claims(token);
            return properties.issuer().equals(claims.getIssuer()) && claims.getExpiration().after(new Date());
        } catch (RuntimeException ex) {
            return false;
        }
    }

    private Claims claims(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }
}
