package com.mrtovo.backend.controller;

import com.mrtovo.backend.dto.AuthRequest;
import com.mrtovo.backend.dto.AuthResponse;
import com.mrtovo.backend.dto.CandidatInscriptionRequest;
import com.mrtovo.backend.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public AuthResponse login(@RequestBody AuthRequest request) {
        return authService.login(request);
    }

    @PostMapping("/candidats/inscription")
    public AuthResponse inscrireCandidat(@Valid @RequestBody CandidatInscriptionRequest request) {
        return authService.inscrireCandidat(request);
    }
}
