package com.mrtovo.backend.service;

import com.mrtovo.backend.dto.QuestionnaireDTO;
import com.mrtovo.backend.dto.QuestionnaireRequest;
import com.mrtovo.backend.dto.ReponsesTestRequest;
import com.mrtovo.backend.entity.RoleUtilisateur;
import com.mrtovo.backend.entity.Utilisateur;
import com.mrtovo.backend.repository.CandidatRepository;
import com.mrtovo.backend.repository.UtilisateurRepository;

import java.util.ArrayList;
import java.util.List;

/** Outils partages des tests d'integration : un QCM pret, un compte candidat, des reponses choisies. */
final class TestEnLigneSupport {

    private TestEnLigneSupport() {}

    /** Reprend le QCM de l'offre s'il existe, sinon en cree un de trois questions. */
    static QuestionnaireDTO questionnairePret(TestCandidatService service, Long offreId) {
        QuestionnaireDTO existant = service.questionnaire(offreId);
        if (!existant.questions().isEmpty()) {
            return existant;
        }
        return service.enregistrerQuestionnaire(offreId, new QuestionnaireRequest(List.of(
                question("Quel mot-clé déclare une classe en Java ?", "class", "def", "function"),
                question("Quel framework sert l'interface de ce projet ?", "Vue.js", "Angular", "Django"),
                question("Quel code HTTP signifie « introuvable » ?", "404", "200", "500"))));
    }

    /** La premiere proposition est la bonne. */
    static QuestionnaireRequest.Question question(String enonce, String bonne, String... fausses) {
        List<QuestionnaireRequest.Choix> choix = new ArrayList<>();
        choix.add(new QuestionnaireRequest.Choix(bonne, true));
        for (String fausse : fausses) {
            choix.add(new QuestionnaireRequest.Choix(fausse, false));
        }
        return new QuestionnaireRequest.Question(enonce, choix);
    }

    /** Repond juste aux nbBonnes premieres questions, faux aux suivantes. */
    static ReponsesTestRequest reponses(QuestionnaireDTO questionnaire, int nbBonnes) {
        List<ReponsesTestRequest.Reponse> reponses = new ArrayList<>();
        for (int i = 0; i < questionnaire.questions().size(); i++) {
            QuestionnaireDTO.Question q = questionnaire.questions().get(i);
            boolean juste = i < nbBonnes;
            Long choix = q.choix().stream().filter(c -> c.correct() == juste).findFirst().orElseThrow().id();
            reponses.add(new ReponsesTestRequest.Reponse(q.id(), choix));
        }
        return new ReponsesTestRequest(reponses);
    }

    /** Le compte que le candidat creerait par la page d'inscription, relie a son dossier. */
    static void compteCandidat(UtilisateurRepository utilisateurs, CandidatRepository candidats, String email) {
        Utilisateur compte = new Utilisateur();
        compte.setNom("Candidat test");
        compte.setEmail(email);
        compte.setMotDePasse("$2a$10$inutilise.dans.ce.test.................................");
        compte.setRole(RoleUtilisateur.CANDIDAT);
        compte.setCandidat(candidats.findByEmailIgnoreCase(email).orElseThrow());
        utilisateurs.save(compte);
    }
}
