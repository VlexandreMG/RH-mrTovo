package com.mrtovo.backend.service;

import com.mrtovo.backend.dto.SalarieProfilDto;
import com.mrtovo.backend.entity.EmployeEmbauche;
import com.mrtovo.backend.entity.Utilisateur;
import com.mrtovo.backend.exception.ApiException;
import com.mrtovo.backend.repository.UtilisateurRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Relie le compte connecte a son dossier : aucune requete salarie ne transmet d'identifiant de salarie. */
@Service
@Transactional(readOnly = true)
public class EspaceSalarieService {

    private final UtilisateurRepository utilisateurRepository;
    private final SoldeCongeService soldeService;
    private final CongeService congeService;

    public EspaceSalarieService(UtilisateurRepository utilisateurRepository,
                                SoldeCongeService soldeService,
                                CongeService congeService) {
        this.utilisateurRepository = utilisateurRepository;
        this.soldeService = soldeService;
        this.congeService = congeService;
    }

    public Long employeId(String email) {
        return employe(email).getId();
    }

    public SalarieProfilDto profil(String email) {
        EmployeEmbauche employe = employe(email);
        return new SalarieProfilDto(
                employe.getMatricule(),
                employe.getNomComplet(),
                employe.getPoste() != null ? employe.getPoste().getTitre() : null,
                employe.getDepartement() != null ? employe.getDepartement().getNom() : null,
                soldeService.solde(employe.getId()),
                congeService.types());
    }

    private EmployeEmbauche employe(String email) {
        Utilisateur compte = utilisateurRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> ApiException.notFound("Compte introuvable."));
        if (compte.getEmploye() == null) {
            throw ApiException.forbidden("COMPTE_NON_SALARIE", "Ce compte n'est pas rattaché à un salarié.");
        }
        return compte.getEmploye();
    }
}
