package com.analytics.reporting.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.security.Key;
import java.util.ArrayList;

@Component
public class JWTFilter extends OncePerRequestFilter {

    private final StringRedisTemplate redisTemplate;
    private final Key key;

    // The secret is passed into the constructor, so it is loaded before the key is built
    public JWTFilter(StringRedisTemplate redisTemplate, @Value("${jwt.secret}") String secret) {
        this.redisTemplate = redisTemplate;
        this.key = Keys.hmacShaKeyFor(secret.getBytes());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            try {
                Claims claims = Jwts.parserBuilder().setSigningKey(key).build()
                        .parseClaimsJws(token.substring(7)).getBody();

                String appId = claims.get("appId", String.class);
                String tokenType = claims.get("type", String.class);
                Integer tokenVersion = claims.get("token_version", Integer.class);

                String savedVersion = redisTemplate.opsForValue().get("app_token_version" + appId);
                String status = redisTemplate.opsForValue().get("app_status" + appId);

                boolean valid = !"refresh_token".equalsIgnoreCase(tokenType)
                        && tokenVersion != null && savedVersion != null
                        && tokenVersion.equals(Integer.valueOf(savedVersion))
                        && "active".equalsIgnoreCase(status);

                if (!valid) {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    return;
                }
                request.setAttribute("appId", appId);
                SecurityContextHolder.getContext().setAuthentication(
                        new UsernamePasswordAuthenticationToken(claims.getSubject(), null, new ArrayList<>()));
            } catch (Exception e) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }
        }
        // Always pass the request on (the old code only did this when a token was present)
        filterChain.doFilter(request, response);
    }
}
