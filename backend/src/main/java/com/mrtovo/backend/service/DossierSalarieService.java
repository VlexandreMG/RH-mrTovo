package com.mrtovo.backend.service;

import com.mrtovo.backend.dto.DocumentSalarieDto;
import com.mrtovo.backend.dto.DossierResumeDto;
import com.mrtovo.backend.dto.DossierSalarieDto;
import com.mrtovo.backend.dto.EvenementCarriereDto;
import com.mrtovo.backend.dto.IdentiteRequest;
import com.mrtovo.backend.dto.JournalAuditDto;
import com.mrtovo.backend.entity.ActionAudit;
import com.mrtovo.backend.entity.Affectation;
import com.mrtovo.backend.entity.CandidatureSelection;
import com.mrtovo.backend.entity.Contrat;
import com.mrtovo.backend.entity.DocumentSalarie;
import com.mrtovo.backend.entity.EmployeEmbauche;
import com.mrtovo.backend.entity.EtatValidite;
import com.mrtovo.backend.entity.JournalAudit;
import com.mrtovo.backend.entity.PropositionOffre;
import com.mrtovo.backend.entity.StatutContrat;
import com.mrtovo.backend.entity.StatutEmploye;
import com.mrtovo.backend.entity.TypeDocument;
import com.mrtovo.backend.exception.ApiException;
import com.mrtovo.backend.repository.AffectationRepository;
import com.mrtovo.backend.repository.ContratRepository;
import com.mrtovo.backend.repository.DocumentSalarieRepository;
import com.mrtovo.backend.repository.EmployeEmbaucheRepository;
import com.mrtovo.backend.repository.JournalAuditRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Bloc 5 : dossier administratif du salarie.
 * Regroupe en une seule vue l'identite, la situation professionnelle, les contrats, les affectations,
 * l'historique de carriere et les documents ; trace chaque acces dans le journal d'audit.
 */
@Service
@Transactional
public class DossierSalarieService {

    /** Pieces que tout dossier complet doit contenir. */
    static final Set<TypeDocument> PIECES_OBLIGATOIRES =
            EnumSet.of(TypeDocument.CIN, TypeDocument.CERTIFICAT_RESIDENCE, TypeDocument.RIB);

    private static final DateTimeFormatter FORMAT_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final Set<StatutContrat> CONTRATS_CLOS = EnumSet.of(StatutContrat.TERMINE, StatutContrat.RUPTURE);

    private final EmployeEmbaucheRepository employeRepository;
    private final ContratRepository contratRepository;
    private final AffectationRepository affectationRepository;
    private final DocumentSalarieRepository documentRepository;
    private final JournalAuditRepository journalRepository;
    private final DocumentStorageService stockage;
    private final RecrutementMapper mapper;
    private final Clock horloge;

    @Autowired
    public DossierSalarieService(EmployeEmbaucheRepository employeRepository,
                                 ContratRepository contratRepository,
                                 AffectationRepository affectationRepository,
                                 DocumentSalarieRepository documentRepository,
                                 JournalAuditRepository journalRepository,
                                 DocumentStorageService stockage,
                                 RecrutementMapper mapper) {
        this(employeRepository, contratRepository, affectationRepository, documentRepository,
                journalRepository, stockage, mapper, Clock.systemDefaultZone());
    }

    DossierSalarieService(EmployeEmbaucheRepository employeRepository,
                          ContratRepository contratRepository,
                          AffectationRepository affectationRepository,
                          DocumentSalarieRepository documentRepository,
                          JournalAuditRepository journalRepository,
                          DocumentStorageService stockage,
                          RecrutementMapper mapper,
                          Clock horloge) {
        this.employeRepository = employeRepository;
        this.contratRepository = contratRepository;
        this.affectationRepository = affectationRepository;
        this.documentRepository = documentRepository;
        this.journalRepository = journalRepository;
        this.stockage = stockage;
        this.mapper = mapper;
        this.horloge = horloge;
    }

    // =====================================================================
    // Lecture
    // =====================================================================

    @Transactional(readOnly = true)
    public List<DossierResumeDto> listerDossiers() {
        LocalDate aujourdhui = LocalDate.now(horloge);
        return employeRepository.findAll().stream()
                // Les dossiers minimaux crees pour les comptes non salaries n'ont pas de dossier administratif.
                .filter(employe -> !employe.isDossierMinimal())
                .sorted(Comparator.comparing(EmployeEmbauche::getMatricule))
                .map(employe -> {
                    List<DocumentSalarie> documents = documentRepository.findByEmployeIdOrderByDateDepotDesc(employe.getId());
                    List<Contrat> contrats = contratRepository.findByEmployeIdOrderByDateDebutAsc(employe.getId());
                    int expires = compterEtat(documents, EtatValidite.EXPIRE, aujourdhui);
                    int bientot = compterEtat(documents, EtatValidite.EXPIRE_BIENTOT, aujourdhui);
                    int alertes = construireAlertes(employe, contrats, documents, aujourdhui).size();
                    return new DossierResumeDto(
                            employe.getId(),
                            employe.getMatricule(),
                            employe.getNom(),
                            employe.getPrenom(),
                            employe.getEmail(),
                            employe.getPoste() != null ? employe.getPoste().getTitre() : null,
                            employe.getDepartement() != null ? employe.getDepartement().getNom() : null,
                            employe.getDateEntree(),
                            employe.getStatut(),
                            documents.size(),
                            expires,
                            bientot,
                            alertes);
                })
                .toList();
    }

    /** Consulter un dossier est une action tracee : la methode n'est donc pas en lecture seule. */
    public DossierSalarieDto dossier(Long employeId, String auteur) {
        EmployeEmbauche employe = chargerEmploye(employeId);
        tracer(employe, ActionAudit.CONSULTATION_DOSSIER, auteur, null);
        return construireDossier(employe);
    }

    @Transactional(readOnly = true)
    public List<JournalAuditDto> journal(Long employeId) {
        chargerEmploye(employeId);
        return journalRepository.findByEmployeIdOrderByDateActionDesc(employeId).stream()
                .map(ligne -> new JournalAuditDto(
                        ligne.getId(),
                        ligne.getAction(),
                        ligne.getAction().getLibelle(),
                        ligne.getAuteur(),
                        ligne.getDetails(),
                        ligne.getDateAction()))
                .toList();
    }

    // =====================================================================
    // Modification de l'identite
    // =====================================================================

    public DossierSalarieDto modifierIdentite(Long employeId, IdentiteRequest requete, String auteur) {
        EmployeEmbauche employe = chargerEmploye(employeId);

        String email = nettoyer(requete.email()).toLowerCase();
        employeRepository.findByEmailIgnoreCase(email)
                .filter(autre -> !autre.getId().equals(employe.getId()))
                .ifPresent(autre -> {
                    throw ApiException.conflit("EMAIL_DEJA_UTILISE",
                            "L'email " + email + " est déjà utilisé par le salarié " + autre.getMatricule());
                });
        if (requete.dateNaissance() != null
                && Period.between(requete.dateNaissance(), LocalDate.now(horloge)).getYears() < 18) {
            throw ApiException.badRequest("AGE_INVALIDE", "Le salarié doit avoir au moins 18 ans.");
        }

        List<String> changements = new ArrayList<>();
        comparer(changements, "Nom", employe.getNom(), nettoyer(requete.nom()));
        comparer(changements, "Prénom", employe.getPrenom(), nettoyer(requete.prenom()));
        comparer(changements, "Email", employe.getEmail(), email);
        comparer(changements, "Téléphone", employe.getTelephone(), nettoyer(requete.telephone()));
        comparer(changements, "Adresse", employe.getAdresse(), nettoyer(requete.adresse()));
        comparer(changements, "Date de naissance", employe.getDateNaissance(), requete.dateNaissance());
        comparer(changements, "Lieu de naissance", employe.getLieuNaissance(), nettoyer(requete.lieuNaissance()));
        comparer(changements, "Sexe", employe.getSexe(), nettoyer(requete.sexe()));
        comparer(changements, "Nationalité", employe.getNationalite(), nettoyer(requete.nationalite()));
        comparer(changements, "N° CIN", employe.getNumeroCin(), nettoyer(requete.numeroCin()));
        comparer(changements, "N° CNaPS", employe.getNumeroCnaps(), nettoyer(requete.numeroCnaps()));
        comparer(changements, "Situation familiale", employe.getSituationFamiliale(), nettoyer(requete.situationFamiliale()));
        comparer(changements, "Nombre d'enfants", employe.getNombreEnfants(), requete.nombreEnfants());
        comparer(changements, "Contact d'urgence", employe.getContactUrgenceNom(), nettoyer(requete.contactUrgenceNom()));
        comparer(changements, "Tél. contact d'urgence", employe.getContactUrgenceTelephone(),
                nettoyer(requete.contactUrgenceTelephone()));

        if (changements.isEmpty()) {
            return construireDossier(employe);
        }

        employe.setNom(nettoyer(requete.nom()));
        employe.setPrenom(nettoyer(requete.prenom()));
        employe.setEmail(email);
        employe.setTelephone(nettoyer(requete.telephone()));
        employe.setAdresse(nettoyer(requete.adresse()));
        employe.setDateNaissance(requete.dateNaissance());
        employe.setLieuNaissance(nettoyer(requete.lieuNaissance()));
        employe.setSexe(nettoyer(requete.sexe()));
        employe.setNationalite(nettoyer(requete.nationalite()));
        employe.setNumeroCin(nettoyer(requete.numeroCin()));
        employe.setNumeroCnaps(nettoyer(requete.numeroCnaps()));
        employe.setSituationFamiliale(nettoyer(requete.situationFamiliale()));
        employe.setNombreEnfants(requete.nombreEnfants());
        employe.setContactUrgenceNom(nettoyer(requete.contactUrgenceNom()));
        employe.setContactUrgenceTelephone(nettoyer(requete.contactUrgenceTelephone()));
        employeRepository.save(employe);

        tracer(employe, ActionAudit.MODIFICATION_IDENTITE, auteur, String.join("\n", changements));
        return construireDossier(employe);
    }

    // =====================================================================
    // Documents administratifs
    // =====================================================================

    public DocumentSalarieDto ajouterDocument(Long employeId,
                                              TypeDocument type,
                                              String libelle,
                                              String numero,
                                              LocalDate dateEmission,
                                              LocalDate dateExpiration,
                                              MultipartFile fichier,
                                              String auteur) {
        EmployeEmbauche employe = chargerEmploye(employeId);
        LocalDate aujourdhui = LocalDate.now(horloge);

        if (type == null) {
            throw ApiException.badRequest("TYPE_DOCUMENT_MANQUANT", "Le type de document est obligatoire.");
        }
        if (dateEmission != null && dateEmission.isAfter(aujourdhui)) {
            throw ApiException.badRequest("DATE_EMISSION_FUTURE", "La date d'émission ne peut pas être dans le futur.");
        }
        if (dateEmission != null && dateExpiration != null && dateExpiration.isBefore(dateEmission)) {
            throw ApiException.badRequest("DATES_INCOHERENTES",
                    "La date d'expiration doit être postérieure à la date d'émission.");
        }

        DocumentSalarie document = new DocumentSalarie();
        document.setEmploye(employe);
        document.setTypeDocument(type);
        document.setLibelle(nettoyer(libelle) != null ? nettoyer(libelle) : type.getLibelle());
        document.setNumero(nettoyer(numero));
        document.setDateEmission(dateEmission);
        document.setDateExpiration(dateExpiration);
        document.setDeposePar(auteur);

        if (fichier != null && !fichier.isEmpty()) {
            DocumentStorageService.FichierStocke stocke = stockage.enregistrer(fichier);
            document.setFichierCle(stocke.cle());
            document.setContentType(stocke.contentType());
            document.setNomFichierOriginal(nomFichierSur(fichier.getOriginalFilename()));
            apresAnnulation(() -> stockage.supprimer(stocke.cle()));
        }
        documentRepository.save(document);

        tracer(employe, ActionAudit.AJOUT_DOCUMENT, auteur, decrire(document));
        return toDocumentDto(document, aujourdhui);
    }

    public FichierTelecharge telechargerDocument(Long documentId, String auteur) {
        DocumentSalarie document = chargerDocument(documentId);
        if (!document.aUnFichier()) {
            throw ApiException.notFound("Aucun fichier n'est joint à ce document.");
        }
        Resource ressource = stockage.charger(document.getFichierCle());
        tracer(document.getEmploye(), ActionAudit.TELECHARGEMENT_DOCUMENT, auteur, decrire(document));
        String nom = document.getNomFichierOriginal() != null
                ? document.getNomFichierOriginal()
                : document.getFichierCle();
        return new FichierTelecharge(ressource, document.getContentType(), nom);
    }

    public void supprimerDocument(Long documentId, String auteur) {
        DocumentSalarie document = chargerDocument(documentId);
        tracer(document.getEmploye(), ActionAudit.SUPPRESSION_DOCUMENT, auteur, decrire(document));
        documentRepository.delete(document);
        if (document.aUnFichier()) {
            String cle = document.getFichierCle();
            apresValidation(() -> stockage.supprimer(cle));
        }
    }

    // =====================================================================
    // Construction du dossier
    // =====================================================================

    private DossierSalarieDto construireDossier(EmployeEmbauche employe) {
        LocalDate aujourdhui = LocalDate.now(horloge);
        List<Contrat> contrats = contratRepository.findByEmployeIdOrderByDateDebutAsc(employe.getId());
        List<Affectation> affectations = affectationRepository.findByEmployeIdOrderByDateDebutAsc(employe.getId());
        List<DocumentSalarie> documents = documentRepository.findByEmployeIdOrderByDateDepotDesc(employe.getId());

        return new DossierSalarieDto(
                employe.getId(),
                employe.getMatricule(),
                construireIdentite(employe, aujourdhui),
                construireSituation(employe, contrats, aujourdhui),
                construireOrigine(employe),
                contrats.stream().map(mapper::toContratDto).toList(),
                affectations.stream().map(mapper::toAffectationDto).toList(),
                construireCarriere(employe, contrats, affectations),
                documents.stream().map(document -> toDocumentDto(document, aujourdhui)).toList(),
                construireAlertes(employe, contrats, documents, aujourdhui));
    }

    private DossierSalarieDto.Identite construireIdentite(EmployeEmbauche employe, LocalDate aujourdhui) {
        Integer age = employe.getDateNaissance() != null
                ? Period.between(employe.getDateNaissance(), aujourdhui).getYears()
                : null;
        return new DossierSalarieDto.Identite(
                employe.getNom(),
                employe.getPrenom(),
                employe.getEmail(),
                employe.getTelephone(),
                employe.getAdresse(),
                employe.getDateNaissance(),
                age,
                employe.getLieuNaissance(),
                employe.getSexe(),
                employe.getNationalite(),
                employe.getNumeroCin(),
                employe.getNumeroCnaps(),
                employe.getSituationFamiliale(),
                employe.getNombreEnfants(),
                employe.getContactUrgenceNom(),
                employe.getContactUrgenceTelephone());
    }

    private DossierSalarieDto.SituationProfessionnelle construireSituation(EmployeEmbauche employe,
                                                                          List<Contrat> contrats,
                                                                          LocalDate aujourdhui) {
        Contrat enCours = contratEnCours(contrats, aujourdhui);
        return new DossierSalarieDto.SituationProfessionnelle(
                employe.getPoste() != null ? employe.getPoste().getTitre() : null,
                employe.getDepartement() != null ? employe.getDepartement().getNom() : null,
                employe.getStatut(),
                employe.getStatut() != null ? employe.getStatut().getLibelle() : null,
                employe.getDateEmbauche(),
                employe.getDateEntree(),
                employe.getDateFinPeriodeEssai(),
                employe.getDateFinContrat(),
                anciennete(employe.getDateEntree(), aujourdhui),
                enCours != null ? enCours.getReference() : null,
                enCours != null ? enCours.getTypeContrat() : null,
                enCours != null ? enCours.getSalaire() : null);
    }

    private DossierSalarieDto.OrigineRecrutement construireOrigine(EmployeEmbauche employe) {
        PropositionOffre proposition = employe.getProposition();
        CandidatureSelection candidature = proposition != null ? proposition.getCandidature() : null;
        return new DossierSalarieDto.OrigineRecrutement(
                employe.getCandidat() != null ? employe.getCandidat().getId() : null,
                candidature != null ? candidature.getId() : null,
                candidature != null && candidature.getOffreEmploi() != null ? candidature.getOffreEmploi().getTitre() : null,
                candidature != null ? candidature.getDateCandidature() : null,
                proposition != null ? proposition.getId() : null,
                proposition != null ? proposition.getDateAcceptation() : null);
    }

    /**
     * Frise chronologique : on ne stocke pas d'historique supplementaire, on le reconstruit
     * a partir des contrats et des affectations qui, eux, ne sont jamais ecrases (regle du Bloc 4).
     */
    List<EvenementCarriereDto> construireCarriere(EmployeEmbauche employe,
                                                  List<Contrat> contrats,
                                                  List<Affectation> affectations) {
        List<EvenementCarriereDto> evenements = new ArrayList<>();

        evenements.add(new EvenementCarriereDto(
                employe.getDateEmbauche(),
                "EMBAUCHE",
                "Embauche — matricule " + employe.getMatricule(),
                employe.getPoste() != null ? "Recruté(e) au poste " + employe.getPoste().getTitre() : null));

        Contrat precedent = null;
        for (Contrat contrat : contrats) {
            evenements.add(new EvenementCarriereDto(
                    contrat.getDateDebut(),
                    "CONTRAT",
                    "Contrat " + contrat.getReference() + " — " + contrat.getTypeContrat().getLibelle(),
                    "Salaire : " + montant(contrat.getSalaire()) + " · statut : " + contrat.getStatutContrat().getLibelle()));

            if (precedent != null) {
                if (precedent.getTypeContrat() != contrat.getTypeContrat()) {
                    evenements.add(new EvenementCarriereDto(
                            contrat.getDateDebut(),
                            "CHANGEMENT_CONTRAT",
                            "Changement de type de contrat",
                            precedent.getTypeContrat().getLibelle() + " → " + contrat.getTypeContrat().getLibelle()));
                }
                if (precedent.getSalaire() != null && contrat.getSalaire() != null
                        && precedent.getSalaire().compareTo(contrat.getSalaire()) != 0) {
                    evenements.add(new EvenementCarriereDto(
                            contrat.getDateDebut(),
                            "EVOLUTION_SALAIRE",
                            "Évolution salariale",
                            montant(precedent.getSalaire()) + " → " + montant(contrat.getSalaire())
                                    + " (" + variation(precedent.getSalaire(), contrat.getSalaire()) + ")"));
                }
            }
            if (contrat.getDateFin() != null && CONTRATS_CLOS.contains(contrat.getStatutContrat())) {
                evenements.add(new EvenementCarriereDto(
                        contrat.getDateFin(),
                        "FIN_CONTRAT",
                        "Fin du contrat " + contrat.getReference(),
                        contrat.getStatutContrat().getLibelle()));
            }
            precedent = contrat;
        }

        for (Affectation affectation : affectations) {
            String lieu = (affectation.getPoste() != null ? affectation.getPoste().getTitre() : "Poste non renseigné")
                    + (affectation.getDepartement() != null ? " · " + affectation.getDepartement().getNom() : "");
            evenements.add(new EvenementCarriereDto(
                    affectation.getDateDebut(),
                    "AFFECTATION",
                    "Affectation " + affectation.getType().name().toLowerCase() + " — " + lieu,
                    affectation.getMotif()));
            if (affectation.getDateFin() != null) {
                evenements.add(new EvenementCarriereDto(
                        affectation.getDateFin(),
                        "FIN_AFFECTATION",
                        "Fin d'affectation — " + lieu,
                        null));
            }
        }

        evenements.sort(Comparator.comparing(EvenementCarriereDto::date,
                Comparator.nullsLast(Comparator.naturalOrder())));
        return evenements;
    }

    List<String> construireAlertes(EmployeEmbauche employe,
                                   List<Contrat> contrats,
                                   List<DocumentSalarie> documents,
                                   LocalDate aujourdhui) {
        List<String> alertes = new ArrayList<>();

        for (DocumentSalarie document : documents) {
            EtatValidite etat = document.etatValidite(aujourdhui);
            if (etat == EtatValidite.EXPIRE) {
                alertes.add(document.getLibelle() + " expiré(e) depuis le " + document.getDateExpiration().format(FORMAT_DATE));
            } else if (etat == EtatValidite.EXPIRE_BIENTOT) {
                long jours = ChronoUnit.DAYS.between(aujourdhui, document.getDateExpiration());
                alertes.add(document.getLibelle() + " expire dans " + jours + " jour(s)");
            }
        }

        Set<TypeDocument> presents = documents.stream()
                .filter(document -> document.etatValidite(aujourdhui) != EtatValidite.EXPIRE)
                .map(DocumentSalarie::getTypeDocument)
                .collect(Collectors.toSet());
        PIECES_OBLIGATOIRES.stream()
                .filter(type -> !presents.contains(type))
                .forEach(type -> alertes.add("Pièce obligatoire manquante : " + type.getLibelle()));

        List<String> manquants = new ArrayList<>();
        if (employe.getDateNaissance() == null) manquants.add("date de naissance");
        if (employe.getNumeroCin() == null) manquants.add("n° CIN");
        if (employe.getAdresse() == null) manquants.add("adresse");
        if (!manquants.isEmpty()) {
            alertes.add("Identité incomplète : " + String.join(", ", manquants));
        }

        if (employe.getStatut() == StatutEmploye.EN_PERIODE_DESSAI && employe.getDateFinPeriodeEssai() != null) {
            long jours = ChronoUnit.DAYS.between(aujourdhui, employe.getDateFinPeriodeEssai());
            if (jours < 0) {
                alertes.add("Période d'essai terminée le " + employe.getDateFinPeriodeEssai().format(FORMAT_DATE)
                        + " : confirmer ou rompre l'embauche");
            } else if (jours <= EtatValidite.SEUIL_ALERTE_JOURS) {
                alertes.add("Fin de période d'essai dans " + jours + " jour(s)");
            }
        }

        if (employe.getStatut() != StatutEmploye.SORTI && employe.getDateFinContrat() != null) {
            long jours = ChronoUnit.DAYS.between(aujourdhui, employe.getDateFinContrat());
            if (jours >= 0 && jours <= EtatValidite.SEUIL_ALERTE_JOURS) {
                alertes.add("Fin de contrat dans " + jours + " jour(s) : renouveler ou clôturer");
            }
        }

        contrats.stream()
                .filter(contrat -> contrat.getStatutContrat() == StatutContrat.A_SIGNER)
                .forEach(contrat -> alertes.add("Contrat " + contrat.getReference() + " en attente de signature"));

        return alertes;
    }

    // =====================================================================
    // Outils
    // =====================================================================

    /** Contrat en cours = le dernier contrat non clos deja commence (a defaut, le dernier non clos). */
    static Contrat contratEnCours(List<Contrat> contrats, LocalDate aujourdhui) {
        List<Contrat> ouverts = contrats.stream()
                .filter(contrat -> !CONTRATS_CLOS.contains(contrat.getStatutContrat()))
                .toList();
        return ouverts.stream()
                .filter(contrat -> !contrat.getDateDebut().isAfter(aujourdhui))
                .max(Comparator.comparing(Contrat::getDateDebut))
                .orElse(ouverts.isEmpty() ? null : ouverts.get(ouverts.size() - 1));
    }

    static String anciennete(LocalDate dateEntree, LocalDate aujourdhui) {
        if (dateEntree == null) {
            return null;
        }
        if (dateEntree.isAfter(aujourdhui)) {
            return "Prise de poste prévue le " + dateEntree.format(FORMAT_DATE);
        }
        Period periode = Period.between(dateEntree, aujourdhui);
        if (periode.getYears() == 0 && periode.getMonths() == 0) {
            return "Moins d'un mois";
        }
        List<String> morceaux = new ArrayList<>();
        if (periode.getYears() > 0) morceaux.add(periode.getYears() + " an" + (periode.getYears() > 1 ? "s" : ""));
        if (periode.getMonths() > 0) morceaux.add(periode.getMonths() + " mois");
        return String.join(" ", morceaux);
    }

    private DocumentSalarieDto toDocumentDto(DocumentSalarie document, LocalDate aujourdhui) {
        Long jours = document.getDateExpiration() != null
                ? ChronoUnit.DAYS.between(aujourdhui, document.getDateExpiration())
                : null;
        return new DocumentSalarieDto(
                document.getId(),
                document.getEmploye().getId(),
                document.getTypeDocument(),
                document.getTypeDocument().getLibelle(),
                document.getLibelle(),
                document.getNumero(),
                document.getDateEmission(),
                document.getDateExpiration(),
                document.etatValidite(aujourdhui),
                jours,
                document.aUnFichier(),
                document.getNomFichierOriginal(),
                document.getDateDepot(),
                document.getDeposePar());
    }

    private void tracer(EmployeEmbauche employe, ActionAudit action, String auteur, String details) {
        journalRepository.save(new JournalAudit(employe, action, auteur != null ? auteur : "inconnu", details));
    }

    private EmployeEmbauche chargerEmploye(Long employeId) {
        return employeRepository.findById(employeId)
                .orElseThrow(() -> ApiException.notFound("Dossier salarié introuvable : " + employeId));
    }

    private DocumentSalarie chargerDocument(Long documentId) {
        return documentRepository.findById(documentId)
                .orElseThrow(() -> ApiException.notFound("Document introuvable : " + documentId));
    }

    private static int compterEtat(List<DocumentSalarie> documents, EtatValidite etat, LocalDate aujourdhui) {
        return (int) documents.stream().filter(document -> document.etatValidite(aujourdhui) == etat).count();
    }

    private static void comparer(List<String> changements, String champ, Object avant, Object apres) {
        if (!Objects.equals(avant, apres)) {
            changements.add(champ + " : " + (avant != null ? avant : "—") + " → " + (apres != null ? apres : "—"));
        }
    }

    private static String nettoyer(String valeur) {
        if (valeur == null || valeur.isBlank()) {
            return null;
        }
        return valeur.trim();
    }

    static String nomFichierSur(String nomOriginal) {
        if (nomOriginal == null) {
            return null;
        }
        String nom = nomOriginal.replace('\\', '/');
        nom = nom.substring(nom.lastIndexOf('/') + 1).replaceAll("[\\r\\n\"]", "_");
        return nom.length() > 255 ? nom.substring(nom.length() - 255) : nom;
    }

    private static String decrire(DocumentSalarie document) {
        return document.getTypeDocument().getLibelle() + " — " + document.getLibelle()
                + (document.getNumero() != null ? " (n° " + document.getNumero() + ")" : "")
                + (document.getNomFichierOriginal() != null ? " · fichier : " + document.getNomFichierOriginal() : "");
    }

    private static String montant(BigDecimal valeur) {
        return valeur == null ? "—" : String.format(Locale.FRANCE, "%,.2f Ar", valeur);
    }

    private static String variation(BigDecimal avant, BigDecimal apres) {
        if (avant.signum() == 0) {
            return "n/a";
        }
        BigDecimal pourcentage = apres.subtract(avant)
                .multiply(BigDecimal.valueOf(100))
                .divide(avant, 1, RoundingMode.HALF_UP);
        return (pourcentage.signum() > 0 ? "+" : "") + pourcentage + " %";
    }

    /** Le fichier a ete ecrit sur disque : si la transaction echoue, on le retire pour ne pas laisser d'orphelin. */
    static void apresAnnulation(Runnable action) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCompletion(int statut) {
                    if (statut == STATUS_ROLLED_BACK) {
                        action.run();
                    }
                }
            });
        }
    }

    /** On ne supprime le fichier qu'une fois la suppression en base validee (sinon le document resterait sans fichier). */
    static void apresValidation(Runnable action) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    action.run();
                }
            });
        } else {
            action.run();
        }
    }

    public record FichierTelecharge(Resource ressource, String contentType, String nomFichier) {
    }
}
