package com.mrtovo.backend.controller;

import com.mrtovo.backend.dto.ActivationCompteSalarieRequest;
import com.mrtovo.backend.dto.AuthRequest;
import com.mrtovo.backend.dto.AuthResponse;
import com.mrtovo.backend.dto.CandidatInscriptionRequest;
import com.mrtovo.backend.service.AuthService;
import com.mrtovo.backend.service.CompteSalarieService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final CompteSalarieService compteSalarieService;

    public AuthController(AuthService authService, CompteSalarieService compteSalarieService) {
        this.authService = authService;
        this.compteSalarieService = compteSalarieService;
    }

    @PostMapping("/login")
    public AuthResponse login(@RequestBody AuthRequest request) {
        return authService.login(request);
    }

    @PostMapping("/candidats/inscription")
    public AuthResponse inscrireCandidat(@Valid @RequestBody CandidatInscriptionRequest request) {
        return authService.inscrireCandidat(request);
    }

    /** Le salarie active son compte avec son matricule et l'email de son dossier. 204 : la page se connecte ensuite. */
    @PostMapping("/salaries/activation")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void activerCompteSalarie(@Valid @RequestBody ActivationCompteSalarieRequest request) {
        compteSalarieService.activer(request);
    }
}
