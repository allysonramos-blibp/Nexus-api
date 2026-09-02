package com.nexus.nexus_api.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.Map;

/**
 * Responsável por gerar e validar tokens JWT usados como credencial de acesso à API.
 * Token carrega: subject = e-mail do usuário, claim "userId" = id do usuário.
 */
@Service
public class JwtService {

    private final SecretKey signingKey;
    private final long expirationMs;

    public JwtService(
            @Value("${jwt.secret}") String base64Secret,
            @Value("${jwt.expiration-ms}") long expirationMs
    ) {
        this.signingKey = Keys.hmacShaKeyFor(java.util.Base64.getDecoder().decode(base64Secret));
        this.expirationMs = expirationMs;
    }

    public String generateToken(UserPrincipal principal) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .subject(principal.getUsername())
                .claims(Map.of("userId", principal.getId()))
                .issuedAt(now)
                .expiration(expiry)
                .signWith(signingKey)
                .compact();
    }

    public long getExpirationMs() {
        return expirationMs;
    }

    /** Extrai o e-mail (subject) do token. Lança JwtException se o token for inválido/expirado. */
    public String extractEmail(String token) {
        return parseClaims(token).getSubject();
    }

    /** Extrai o id do usuário do token. Lança JwtException se o token for inválido/expirado. */
    public Long extractUserId(String token) {
        Object userId = parseClaims(token).get("userId");
        if (userId instanceof Number number) {
            return number.longValue();
        }
        throw new JwtException("Token não contém um userId válido.");
    }

    /**
     * Valida assinatura, formato e expiração do token, e confere se o subject bate com o
     * usuário carregado. Não lança exceção: retorna false para qualquer token inválido/expirado.
     */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        try {
            Claims claims = parseClaims(token);
            boolean subjectMatches = claims.getSubject().equals(userDetails.getUsername());
            boolean notExpired = claims.getExpiration().after(new Date());
            return subjectMatches && notExpired;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
