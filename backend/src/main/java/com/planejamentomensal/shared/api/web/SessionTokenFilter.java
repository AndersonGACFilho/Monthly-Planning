package com.planejamentomensal.shared.api.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.json.JsonMapper;

/**
 * Rejects API calls that do not carry the session token created by the shell.
 *
 * <p>Implements the security row of SPEC section 12 (IMP-04): only the app window, which received the token
 * at launch, can call {@code /api}. The health endpoint stays open so the shell can wait for startup
 * (POR-08); CORS preflight requests carry no token and are let through to the CORS handling.</p>
 *
 * @since 1.0
 */
public class SessionTokenFilter extends OncePerRequestFilter {

    /** Request header that carries the token. */
    public static final String HEADER = "X-Session-Token";

    private final byte[] expected;
    private final JsonMapper json = JsonMapper.builder().build();

    /**
     * Creates the filter.
     *
     * @param token session token of this launch; must not be blank
     * @throws IllegalArgumentException if {@code token} is blank
     */
    public SessionTokenFilter(String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("Session token is required (APP_SESSION_TOKEN)");
        }
        this.expected = token.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        boolean api = path.equals("/api") || path.startsWith("/api/");
        return !api || HttpMethod.OPTIONS.matches(request.getMethod());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String token = request.getHeader(HEADER);
        if (token != null && MessageDigest.isEqual(expected, token.getBytes(StandardCharsets.UTF_8))) {
            chain.doFilter(request, response);
            return;
        }
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED,
                "Missing or invalid " + HEADER + " header.");
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        json.writeValue(response.getOutputStream(), problem);
    }
}
