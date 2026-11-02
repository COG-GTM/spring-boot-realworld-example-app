package com.gs.collateral.security;

import java.io.IOException;
import java.time.Clock;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Authenticates requests carrying a desk token. Unknown or invalid tokens are ignored so the
 * request falls through to the normal 401 handling.
 */
public class LegacyDeskTokenFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(LegacyDeskTokenFilter.class);

    private final DeskTokenProperties properties;
    private final Clock clock;

    public LegacyDeskTokenFilter(DeskTokenProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String token = request.getHeader(properties.getHeader());
        if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            DeskTokenCodec.verify(token, properties.getSharedSecret(), clock.instant().getEpochSecond(),
                            properties.getMaxSkewSeconds())
                    .ifPresent(parsed -> authenticate(parsed.getDesk()));
        }
        chain.doFilter(request, response);
    }

    private void authenticate(String desk) {
        String roles = properties.getDesks().get(desk);
        if (roles == null) {
            log.warn("Desk token for unknown desk '{}' rejected", desk);
            return;
        }
        List<GrantedAuthority> authorities = Arrays.stream(roles.split(","))
                .map(String::trim)
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .collect(Collectors.toList());
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(new DeskPrincipal(desk), null, authorities);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
