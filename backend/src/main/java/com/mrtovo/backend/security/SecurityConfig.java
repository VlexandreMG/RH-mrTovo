package com.mrtovo.backend.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/public/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/public/**").permitAll()
                        .requestMatchers("/api/candidat/**").hasRole("CANDIDAT")
<<<<<<< HEAD
                        // Bloc 5 : dossier administratif confidentiel, reserve au RH.
                        .requestMatchers("/api/bloc5/**").hasRole("RH")
                        // Bloc 6 : pointages et variables de paie, reserves au RH.
                        .requestMatchers("/api/bloc6/**").hasRole("RH")
                        // Bloc 7 : l'espace salarie, les ecrans du RH, puis les demandes (RH et manager de son departement).
                        .requestMatchers("/api/salarie/**")
                        .hasAnyRole("EMPLOYE", "RH", "MANAGER", "FINANCE", "DIRECTION")
                        .requestMatchers("/api/bloc7/rh/**").hasRole("RH")
                        .requestMatchers("/api/bloc7/**").hasAnyRole("RH", "MANAGER")
=======
                        // Qui a fait quoi sur une candidature : information reservee au RH.
                        .requestMatchers(HttpMethod.GET, "/api/candidatures/*/historique").hasRole("RH")
                        // Bloc 2 : le QCM (bonnes réponses comprises) et les réponses d'un candidat, réservés au RH.
                        .requestMatchers("/api/offres/*/questionnaire").hasRole("RH")
                        .requestMatchers(HttpMethod.GET, "/api/candidatures/*/test").hasRole("RH")
>>>>>>> main
                        .requestMatchers("/api/**").hasAnyRole("MANAGER", "RH", "FINANCE", "DIRECTION")
                        .anyRequest().authenticated()
                )
                // 401 sans en-tête WWW-Authenticate : évite la boîte de dialogue du navigateur.
                .httpBasic(basic -> basic.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)));
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:5173"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
