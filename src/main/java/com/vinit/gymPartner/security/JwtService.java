package com.vinit.gymPartner.security;

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

    private Key getSigningKey()
    {
        return Keys.hmacShaKeyFor(secret.getBytes());  //converts your secret string into a secure key object
    }

    public String generateToken(Long userId, String email)
    {
        return Jwts.builder()
                .setSubject(email) //Subject = main identity of the user.
                .claim("userId", userId)   //Claim = extra information stored in JWT.
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();  //Converts it to String and return it
    }

    public Long extractUserId(String token)
    {
        return extractAllClaims(token).get("userId", Long.class);
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

}
