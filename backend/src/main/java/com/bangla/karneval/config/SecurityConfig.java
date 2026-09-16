package com.bangla.karneval.config;

import com.bangla.karneval.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private JwtUtil jwtUtil;

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth

                        // ── Static frontend files ──────────────────────────────
                        .requestMatchers(
                                new AntPathRequestMatcher("/"),
                                new AntPathRequestMatcher("/index.html"),
                                new AntPathRequestMatcher("/about_us.html"),
                                new AntPathRequestMatcher("/gallery.html"),
                                new AntPathRequestMatcher("/registration.html"),
                                new AntPathRequestMatcher("/performer_registration.html"),
                                new AntPathRequestMatcher("/contact.html"),
                                new AntPathRequestMatcher("/admin.html"),
                                new AntPathRequestMatcher("/admin_dashboard.html"),
                                new AntPathRequestMatcher("/css/**"),
                                new AntPathRequestMatcher("/js/**"),
                                new AntPathRequestMatcher("/assets/**"),
                                new AntPathRequestMatcher("/uploads/**"),
                                new AntPathRequestMatcher("/components/**")
                        ).permitAll()

                        // ── Public API endpoints ───────────────────────────────
                        .requestMatchers(
                                new AntPathRequestMatcher("/error"),
                                new AntPathRequestMatcher("/api/config/current"),
                                new AntPathRequestMatcher("/api/config/button-status"),
                                new AntPathRequestMatcher("/api/admin/config/performer-status"),
                                new AntPathRequestMatcher("/api/sponsors"),
                                new AntPathRequestMatcher("/api/events/**"),
                                new AntPathRequestMatcher("/api/gallery/**"),
                                new AntPathRequestMatcher("/api/register/**"),
                                new AntPathRequestMatcher("/api/contact"),
                                new AntPathRequestMatcher("/api/admin/auth/login")
                        ).permitAll()

                        // ── Swagger UI ─────────────────────────────────────────
                        .requestMatchers(
                                new AntPathRequestMatcher("/swagger-ui.html"),
                                new AntPathRequestMatcher("/swagger-ui/**"),
                                new AntPathRequestMatcher("/v3/api-docs/**")
                        ).permitAll()

                        // ── Everything else requires JWT ───────────────────────
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public OncePerRequestFilter jwtAuthFilter() {
        return new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(HttpServletRequest request,
                                            HttpServletResponse response,
                                            FilterChain filterChain)
                    throws ServletException, IOException {

                String authHeader = request.getHeader("Authorization");

                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                    String token = authHeader.substring(7);
                    if (jwtUtil.validateToken(token)) {
                        String email = jwtUtil.extractEmail(token);
                        String role  = jwtUtil.extractRole(token);

                        var auth = new org.springframework.security.authentication
                                .UsernamePasswordAuthenticationToken(
                                email, null,
                                java.util.List.of(new org.springframework.security.core
                                        .authority.SimpleGrantedAuthority("ROLE_" + role))
                        );
                        org.springframework.security.core.context.SecurityContextHolder
                                .getContext().setAuthentication(auth);
                    }
                }
                filterChain.doFilter(request, response);
            }
        };
    }
}
