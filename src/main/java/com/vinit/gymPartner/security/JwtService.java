package com.vinit.gymPartner.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;

@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long expiration;

    @Value("${jwt.reset-expiration}")
    private long resetExpiration;

    private Key getSigningKey()
    {
        return Keys.hmacShaKeyFor(secret.getBytes());  //converts your secret string into a secure key object
    }

    public String generateToken(Long userId, String email)
    {
        return buildToken(userId, email, expiration, "access");
    }

    public String generatePasswordResetToken(Long userId, String email) {
        return buildToken(userId, email, resetExpiration, "password_reset");
    }

    private String buildToken(Long userId, String email, long tokenExpiration, String purpose) {
        return Jwts.builder()
                .setSubject(email) //Subject = main identity of the user.
                .claim("userId", userId)   //Claim = extra information stored in JWT.
                .claim("purpose", purpose)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + tokenExpiration))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)   //Sign it (prove it's real)
                .compact();  //Converts it to String and return it
    }

    public Long extractUserId(String token)
    {
        return extractAllClaims(token).get("userId", Long.class);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public String extractEmail(String token)
    {
        return extractAllClaims(token).getSubject();
    }

    public boolean isTokenValid(String token)
    {
        try {
            extractAllClaims(token);
            return true;
        } catch (JwtException e) {
            return false;
        }
    }

    public boolean isPasswordResetTokenValid(String token) {
        try {
            Claims claims = extractAllClaims(token);
            return "password_reset".equals(claims.get("purpose", String.class));
        } catch (JwtException e) {
            return false;
        }
    }

}
