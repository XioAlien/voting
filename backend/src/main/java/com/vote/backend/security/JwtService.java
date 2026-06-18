package com.vote.backend.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtService {

  private final SecretKey key;
  private final long expirationMs;

  public JwtService(@Value("${jwt.secret}") String secret, @Value("${jwt.expiration}") long expirationMs) {
    this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    this.expirationMs = expirationMs;
  }

  public String generateToken(UserPrincipal principal) {
    Date now = new Date();
    Date exp = new Date(now.getTime() + expirationMs);
    return Jwts.builder()
        .setSubject(principal.getUsername())
        .claim("uid", principal.getId())
        .setIssuedAt(now)
        .setExpiration(exp)
        .signWith(key, SignatureAlgorithm.HS256)
        .compact();
  }

  public Claims parseClaims(String token) {
    return Jwts.parserBuilder()
        .setSigningKey(key)
        .build()
        .parseClaimsJws(token)
        .getBody();
  }

  public String extractUsername(String token) {
    return parseClaims(token).getSubject();
  }

  public boolean isTokenValid(String token, UserPrincipal user) {
    Claims claims = parseClaims(token);
    Date expiration = claims.getExpiration();
    return user.getUsername().equals(claims.getSubject()) && expiration != null && expiration.after(new Date());
  }
}

