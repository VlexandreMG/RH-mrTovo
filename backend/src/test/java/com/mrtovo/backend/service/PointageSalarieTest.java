package com.mrtovo.backend.service;

import com.mrtovo.backend.dto.CorrectionPointageRequest;
import com.mrtovo.backend.dto.PointageRequest;
import com.mrtovo.backend.entity.ActionAudit;
import com.mrtovo.backend.entity.EmployeEmbauche;
import com.mrtovo.backend.entity.Pointage;
import com.mrtovo.backend.entity.SourcePointage;
import com.mrtovo.backend.entity.StatutEmploye;
import com.mrtovo.backend.exception.ApiException;
import com.mrtovo.backend.repository.DecisionHeuresSupRepository;
import com.mrtovo.backend.repository.EmployeEmbaucheRepository;
import com.mrtovo.backend.repository.PointageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Bloc 6 : le salarie pointe lui-meme, a l'heure du serveur ; le RH n'ajoute ou ne corrige qu'avec un motif. */
class PointageSalarieTest {

    private static final LocalDate AUJOURDHUI = LocalDate.of(2026, 11, 13);
    private static final String AINA = "aina.ranaivo@gmail.com";

    private PointageRepository pointageRepository;
    private TraceTempsService trace;
    private EmployeEmbauche employe;

    @BeforeEach
    void preparer() {
        pointageRepository = mock(PointageRepository.class);
        trace = mock(TraceTempsService.class);
        employe = new EmployeEmbauche();
        employe.setId(1L);
        employe.setDateEntree(LocalDate.of(2025, 3, 6));
        employe.setStatut(StatutEmploye.ACTIF);
        when(pointageRepository.findByEmployeIdAndDatePointage(1L, AUJOURDHUI)).thenReturn(Optional.empty());
    }

    /** L'horloge du serveur fixee a l'heure voulue, secondes comprises. */
    private PointageService a(int heure, int minute, int seconde) {
        EmployeEmbaucheRepository employes = mock(EmployeEmbaucheRepository.class);
        when(employes.findById(1L)).thenReturn(Optional.of(employe));
        Clock horloge = Clock.fixed(LocalDateTime.of(AUJOURDHUI, LocalTime.of(heure, minute, seconde))
                .atZone(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault());
        return new PointageService(employes, pointageRepository, mock(DecisionHeuresSupRepository.class),
                mock(TempsTravailService.class), trace, horloge);
    }

    private Pointage pointageDuJour(LocalTime entree, LocalTime sortie) {
        Pointage pointage = new Pointage();
        pointage.setEmploye(employe);
        pointage.setDatePointage(AUJOURDHUI);
        pointage.setHeureEntree(entree);
        pointage.setHeureSortie(sortie);
        pointage.setSource(SourcePointage.SALARIE);
        when(pointageRepository.findByEmployeIdAndDatePointage(1L, AUJOURDHUI)).thenReturn(Optional.of(pointage));
        when(pointageRepository.findById(9L)).thenReturn(Optional.of(pointage));
        return pointage;
    }

    private static String code(Runnable action) {
        return assertThrows(ApiException.class, action::run).getCode();
    }

    @Test
    void lArriveeEstLHeureDuServeurALaMinuteEtSigneeParLeSalarie() {
        a(8, 11, 47).pointerArrivee(1L, AINA);

        ArgumentCaptor<Pointage> enregistre = ArgumentCaptor.forClass(Pointage.class);
        verify(pointageRepository).save(enregistre.capture());
        assertEquals(LocalTime.of(8, 11), enregistre.getValue().getHeureEntree());
        assertEquals(AUJOURDHUI, enregistre.getValue().getDatePointage());
        assertEquals(SourcePointage.SALARIE, enregistre.getValue().getSource());
        assertEquals(AINA, enregistre.getValue().getSaisiPar());
        verify(trace).tracer(eq(employe), eq(ActionAudit.POINTAGE_ARRIVEE), eq(AINA), anyString());
    }

    @Test
    void uneSeuleArriveeParJour() {
        pointageDuJour(LocalTime.of(8, 0), null);

        assertEquals("ARRIVEE_DEJA_POINTEE", code(() -> a(9, 0, 0).pointerArrivee(1L, AINA)));
        verify(pointageRepository, never()).save(any());
    }

    @Test
    void leDepartCompleteLePointageDuJour() {
        Pointage pointage = pointageDuJour(LocalTime.of(8, 0), null);

        a(17, 4, 59).pointerDepart(1L, AINA);

        assertEquals(LocalTime.of(17, 4), pointage.getHeureSortie());
        assertEquals(SourcePointage.SALARIE, pointage.getSource());
        verify(trace).tracer(eq(employe), eq(ActionAudit.POINTAGE_DEPART), eq(AINA), anyString());
    }

    @Test
    void pasDeDepartSansArrivee() {
        assertEquals("ARRIVEE_NON_POINTEE", code(() -> a(17, 0, 0).pointerDepart(1L, AINA)));
    }

    @Test
    void unSeulDepartParJour() {
        pointageDuJour(LocalTime.of(8, 0), LocalTime.of(17, 0));

        assertEquals("DEPART_DEJA_POINTE", code(() -> a(18, 0, 0).pointerDepart(1L, AINA)));
    }

    @Test
    void leDepartDoitSuivreLArrivee() {
        pointageDuJour(LocalTime.of(8, 0), null);

        assertEquals("DEPART_TROP_TOT", code(() -> a(8, 0, 30).pointerDepart(1L, AINA)));
    }

    @Test
    void unSalarieSortiNePointePlus() {
        employe.setStatut(StatutEmploye.SORTI);

        assertEquals("SALARIE_SORTI", code(() -> a(8, 0, 0).pointerArrivee(1L, AINA)));
    }

    @Test
    void leRhNAjouteUnPointageQuAvecUnMotif() {
        PointageService service = a(10, 0, 0);
        PointageRequest sansMotif = new PointageRequest(1L, AUJOURDHUI, LocalTime.of(8, 0), null, null, " ");

        assertEquals("MOTIF_OBLIGATOIRE", code(() -> service.saisir(sansMotif, "rh@rh.com")));

        service.saisir(new PointageRequest(1L, AUJOURDHUI, LocalTime.of(8, 0), null, null, "Badge oublié"), "rh@rh.com");
        ArgumentCaptor<Pointage> enregistre = ArgumentCaptor.forClass(Pointage.class);
        verify(pointageRepository).save(enregistre.capture());
        assertEquals(SourcePointage.RH, enregistre.getValue().getSource());
        verify(trace).tracer(eq(employe), eq(ActionAudit.SAISIE_POINTAGE), eq("rh@rh.com"),
                org.mockito.ArgumentMatchers.contains("Badge oublié"));
    }

    @Test
    void unPointageCorrigeParLeRhEnGardeLaTrace() {
        Pointage pointage = pointageDuJour(LocalTime.of(8, 0), null);

        a(18, 0, 0).corriger(9L, new CorrectionPointageRequest(LocalTime.of(8, 0), LocalTime.of(17, 0), null,
                "Sortie oubliée"), "rh@rh.com");

        assertEquals(SourcePointage.CORRIGE_RH, pointage.getSource());
        assertEquals(LocalTime.of(17, 0), pointage.getHeureSortie());
    }
}
