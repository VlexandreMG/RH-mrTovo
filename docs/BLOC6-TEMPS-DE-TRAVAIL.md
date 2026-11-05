# Bloc 6 — Temps de travail et pointage

> Documentation technique rédigée par l'auteur du code. J'y explique **ce que j'ai fait, comment et pourquoi**, fichier par fichier, bloc par bloc.

---

## Sommaire

1. [Objectif et règles métier retenues](#1-objectif-et-règles-métier-retenues)
2. [L'idée centrale : les faits sont stockés, les résultats sont calculés](#2-lidée-centrale--les-faits-sont-stockés-les-résultats-sont-calculés)
3. [Vue d'ensemble de l'architecture](#3-vue-densemble-de-larchitecture)
4. [Base de données — `schema.sql` et `data.sql`](#4-base-de-données--schemasql-et-datasql)
5. [Configuration](#5-configuration)
6. [Entités et énumérations](#6-entités-et-énumérations)
7. [Repositories](#7-repositories)
8. [DTOs](#8-dtos)
9. [Le moteur de calcul — `CalculTemps`](#9-le-moteur-de-calcul--calcultemps)
10. [Lecture et calculs — `TempsTravailService`](#10-lecture-et-calculs--tempstravailservice)
11. [Écritures — `PointageService`](#11-écritures--pointageservice)
12. [Traçabilité — `TraceTempsService`](#12-traçabilité--tracetempsservice)
13. [Régimes et planning — `RegimeTravailService`](#13-régimes-et-planning--regimetravailservice)
14. [Contrôleur REST — `TempsTravailController`](#14-contrôleur-rest--tempstravailcontroller)
15. [Sécurité](#15-sécurité)
16. [Frontend](#16-frontend)
17. [Tests](#17-tests)
18. [Référence de l'API](#18-référence-de-lapi)
19. [Flux complets](#19-flux-complets)
20. [Exemples chiffrés](#20-exemples-chiffrés)
21. [Procédure de vérification](#21-procédure-de-vérification)
22. [Limites connues et pistes](#22-limites-connues-et-pistes)

---

## 1. Objectif et règles métier retenues

Le cours définit le Bloc 6 ainsi :

- Définir horaires, planning et régime de travail.
- Enregistrer entrées, sorties, retards et heures supplémentaires.
- Calculer le temps réellement travaillé.
- Détecter les anomalies de pointages.
- Les données de temps alimentent les absences et les variables de paie.

S'y ajoute une demande spécifique : **un traitement des retards avec les heures supplémentaires**.

Trois choix ont été arrêtés avant d'écrire le code :

| Question | Choix |
|---|---|
| Comment traiter les retards avec les heures sup ? | **Compensation par semaine** : les minutes dues sont d'abord déduites des heures sup de la même semaine. |
| Qui saisit les pointages ? | **Le RH** : les salariés n'ont pas de compte dans l'application. |
| Validation et taux ? | **Validation par le RH** avant paiement, puis **130 %** sur les 8 premières heures et **150 %** au-delà. |

Les règles qui en découlent :

| Règle | Où elle est implémentée |
|---|---|
| Un retard dans la **tolérance** est pardonné ; au-delà, **tout** le retard compte (pas seulement ce qui dépasse). | `CalculTemps.retard` |
| Les **heures sup** sont les minutes travaillées **après** l'heure de fin prévue. Arriver en avance n'en crée pas. | `CalculTemps.calculerJour` |
| Un jour de **repos** travaillé est entièrement en heures sup. | `CalculTemps.calculerJour` |
| La **pause** n'est déduite que si la présence atteint 5 h. | `CalculTemps.minutesTravaillees` |
| Retards et départs anticipés forment les **minutes dues** de la semaine. | `TempsTravailService.calculerSemaine` |
| Seules les heures sup **validées** compensent : sinon, une heure refusée effacerait quand même un retard. | `calculerSemaine` puis `CalculTemps.compenser` |
| Une décision n'est possible que si la semaine est **terminée** et sans **sortie manquante**. | `TempsTravailService.raisonBlocage` |
| Un refus ou une validation **partielle** doit être **justifié**. | `PointageService.deciderHeuresSup` |
| Modifier un fait d'une semaine déjà décidée **rouvre** la décision. | `TraceTempsService.rouvrirDecision` |
| Une **correction** de pointage exige un **motif**, conservé avec l'ancienne et la nouvelle valeur. | `CorrectionPointageRequest`, `PointageService.corriger` |
| Un **régime** n'est jamais modifié après sa création ; le planning conserve l'historique. | absence de `PUT`, `RegimeTravailService.affecter` |
| Une semaine appartient au **mois de son lundi** pour la paie. | `TempsTravailService.lundisDu` |

> **Hypothèse à faire confirmer** : les taux 130 % / 150 % et la base hebdomadaire ne viennent pas du cours. Je les ai rendus modifiables dans `application.properties` (voir [§ 5](#5-configuration)) pour qu'une correction ne demande aucun changement de code.

---

## 2. L'idée centrale : les faits sont stockés, les résultats sont calculés

C'est la décision la plus importante du bloc. **Je ne stocke ni les retards, ni les heures sup, ni les anomalies.** En base, il n'y a que :

- ce qui était **prévu** (régime, horaires, planning) ;
- ce qui s'est **passé** (pointages) ;
- ce que le RH a **décidé** (décisions d'heures sup).

Tout le reste est recalculé à chaque lecture. Si je stockais `minutes_retard` sur le pointage, une correction de l'heure d'entrée obligerait à mettre à jour cette colonne, puis le total de la semaine, puis le mois : chaque oubli créerait une incohérence. Ici, corriger un pointage suffit : le résultat suit tout seul. C'est le même raisonnement que `EtatValidite` au Bloc 5 et que la frise de carrière.

Le seul chiffre figé est la décision du RH, car c'est un **acte** : il faut savoir ce qui a été validé, par qui et quand.

---

## 3. Vue d'ensemble de l'architecture

```
 Vue 3                                        Spring Boot                               PostgreSQL (mrtovo)
┌──────────────────────────────┐        ┌─────────────────────────────────┐        ┌──────────────────────┐
│ TempsTravailView.vue         │        │ TempsTravailController          │        │ regime_travail       │
│  ├ PointagesSemaine          │  HTTP  │   /api/bloc6/** (RH uniquement) │        │ horaire_regime       │
│  ├ HeuresSupAValider         │───────▶│      │          │          │     │        │ planning_salarie     │
│  ├ AnomaliesPointage         │        │      ▼          ▼          ▼     │        │ pointage             │
│  ├ VariablesPaie             │        │ TempsTravail  Pointage   Regime │  JPA   │ decision_heures_sup  │
│  └ RegimesTravail            │        │ Service       Service    Service│───────▶│ journal_audit (Bl.5) │
│ api/temps.js                 │        │  (lectures)  (écritures)        │        │ employe, contrat     │
└──────────────────────────────┘        │      │          │               │        └──────────────────────┘
                                        │      ▼          ▼               │
                                        │ CalculTemps  TraceTempsService  │
                                        │ (règles pures) (audit)          │
                                        └─────────────────────────────────┘
```

| Couche | Fichier | Nouveau / modifié |
|---|---|---|
| SQL | `backend/src/main/resources/schema.sql`, `data.sql` | modifié |
| Config | `application.properties`, `config/RecrutementConfiguration.java` | modifié |
| Config | `config/TempsTravailProperties.java` | nouveau |
| Entité | `RegimeTravail`, `HoraireRegime`, `PlanningSalarie`, `Pointage`, `DecisionHeuresSup` | nouveau |
| Enum | `StatutHeuresSup`, `TypeAnomaliePointage` | nouveau |
| Enum | `ActionAudit` | modifié |
| Repository | `RegimeTravailRepository`, `PlanningSalarieRepository`, `PointageRepository`, `DecisionHeuresSupRepository` | nouveau |
| DTO | `RegimeTravailDto`, `RegimeTravailRequest`, `AffectationRegimeRequest`, `PlanningSalarieDto`, `PointageRequest`, `CorrectionPointageRequest`, `DecisionHeuresSupRequest`, `AnomaliePointageDto`, `JourTempsDto`, `SemaineTempsDto`, `VariablesPaieDto` | nouveau |
| Service | `CalculTemps`, `TempsTravailService`, `PointageService`, `TraceTempsService`, `RegimeTravailService` | nouveau |
| Contrôleur | `TempsTravailController` | nouveau |
| Sécurité | `SecurityConfig` | modifié |
| Test | `CalculTempsTest`, `TempsTravailServiceTest` | nouveau |
| Front | `api/temps.js`, `views/TempsTravailView.vue`, `components/temps/*.vue` (6 fichiers) | nouveau |
| Front | `utils/format.js`, `router/index.js`, `components/AppLayout.vue`, `components/RecrutementStatutBadge.vue` | modifié |
| Données | `database/bloc6-donnees-test.sql` | nouveau |

---

## 4. Base de données — `schema.sql` et `data.sql`

Comme au Bloc 5, tout est **idempotent** : `schema.sql` s'exécute à chaque démarrage.

### 4.1 `regime_travail`

```sql
CREATE TABLE IF NOT EXISTS mrtovo.regime_travail (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    libelle VARCHAR(255) NOT NULL,
    tolerance_retard_minutes INTEGER NOT NULL DEFAULT 10,
    date_creation TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    cree_par VARCHAR(150) NOT NULL
);
```

- Un régime est un **modèle réutilisable** : plusieurs salariés peuvent suivre le même.
- `code` est unique : c'est l'identifiant lisible (`BUREAU_40H`).
- `tolerance_retard_minutes` est porté par le régime, car un service d'accueil et un bureau n'ont pas forcément la même tolérance.
- `cree_par` et `date_creation` tracent l'origine.
- Je n'ai **pas** de colonne `heures_semaine` : le total se déduit de la grille (`minutesHebdomadaires()`). Stocker les deux permettrait qu'ils se contredisent.

### 4.2 `horaire_regime`

```sql
CREATE TABLE IF NOT EXISTS mrtovo.horaire_regime (
    id BIGSERIAL PRIMARY KEY,
    regime_id BIGINT NOT NULL REFERENCES mrtovo.regime_travail(id) ON DELETE CASCADE,
    jour_semaine INTEGER NOT NULL,
    heure_debut TIME NOT NULL,
    heure_fin TIME NOT NULL,
    pause_minutes INTEGER NOT NULL DEFAULT 0,
    UNIQUE (regime_id, jour_semaine)
);
```

- Une ligne par jour travaillé. **Un jour sans ligne est un jour de repos** : pas besoin d'une colonne « travaillé oui/non ».
- `jour_semaine` suit la norme ISO, comme `DayOfWeek.getValue()` en Java : 1 = lundi, 7 = dimanche.
- `UNIQUE (regime_id, jour_semaine)` interdit deux horaires le même jour.
- `ON DELETE CASCADE` : ces lignes n'ont pas d'existence sans leur régime.

### 4.3 `planning_salarie`

```sql
CREATE TABLE IF NOT EXISTS mrtovo.planning_salarie (
    id BIGSERIAL PRIMARY KEY,
    employe_id BIGINT NOT NULL REFERENCES mrtovo.employe(id),
    regime_id BIGINT NOT NULL REFERENCES mrtovo.regime_travail(id),
    date_debut DATE NOT NULL,
    date_fin DATE,
    affecte_par VARCHAR(150) NOT NULL,
    UNIQUE (employe_id, date_debut)
);
```

- J'aurais pu ajouter `employe.regime_id`, mais cela reproduirait l'erreur que le cours dénonce au Bloc 4 avec `employe.type_contrat` : changer de régime écraserait le passé et fausserait le calcul des anciennes semaines.
- Une ligne = une **période**. `date_fin` vide signifie « en cours ».
- Le schéma suit le modèle de `affectation`.

### 4.4 `pointage`

```sql
CREATE TABLE IF NOT EXISTS mrtovo.pointage (
    id BIGSERIAL PRIMARY KEY,
    employe_id BIGINT NOT NULL REFERENCES mrtovo.employe(id),
    date_pointage DATE NOT NULL,
    heure_entree TIME NOT NULL,
    heure_sortie TIME,
    commentaire VARCHAR(255),
    saisi_par VARCHAR(150) NOT NULL,
    date_saisie TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    modifie_par VARCHAR(150),
    date_modification TIMESTAMP,
    UNIQUE (employe_id, date_pointage)
);
CREATE INDEX IF NOT EXISTS idx_pointage_date ON mrtovo.pointage(date_pointage);
```

- **Un pointage par salarié et par jour**, garanti par la contrainte `UNIQUE`.
- `heure_sortie` est nullable : on peut pointer l'entrée le matin et la sortie plus tard. Si elle n'arrive jamais, c'est une anomalie.
- Aucune colonne `retard` : voir [§ 2](#2-lidée-centrale--les-faits-sont-stockés-les-résultats-sont-calculés).
- L'index sur la date sert à la requête la plus fréquente : « tous les pointages d'une période ».

### 4.5 `decision_heures_sup`

```sql
CREATE TABLE IF NOT EXISTS mrtovo.decision_heures_sup (
    id BIGSERIAL PRIMARY KEY,
    employe_id BIGINT NOT NULL REFERENCES mrtovo.employe(id),
    semaine_debut DATE NOT NULL,
    statut VARCHAR(20) NOT NULL,
    minutes_brutes INTEGER NOT NULL,
    minutes_validees INTEGER NOT NULL,
    commentaire VARCHAR(255),
    decide_par VARCHAR(150) NOT NULL,
    date_decision TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (employe_id, semaine_debut)
);
```

- Une décision par salarié et par **semaine**, désignée par son lundi.
- `minutes_brutes` fige ce qui était constaté au moment de décider ; `minutes_validees` ce qui est retenu. Les deux peuvent différer (validation partielle).
- L'absence de ligne veut dire « pas encore décidé » : je n'enregistre que les actes, pas l'attente.

### 4.6 Contraintes de domaine

```sql
ALTER TABLE mrtovo.pointage ADD CONSTRAINT ck_pointage_heures
    CHECK (heure_sortie IS NULL OR heure_sortie > heure_entree);

ALTER TABLE mrtovo.decision_heures_sup ADD CONSTRAINT ck_decision_hs_minutes
    CHECK (minutes_validees BETWEEN 0 AND minutes_brutes);

ALTER TABLE mrtovo.decision_heures_sup ADD CONSTRAINT ck_decision_hs_lundi
    CHECK (EXTRACT(ISODOW FROM semaine_debut) = 1);
```

- J'ai suivi le motif du projet : `DROP CONSTRAINT IF EXISTS` puis `ADD CONSTRAINT`.
- `ck_pointage_heures` : une sortie doit suivre l'entrée. Conséquence : le travail de nuit à cheval sur minuit n'est pas géré ([§ 22](#22-limites-connues-et-pistes)).
- `ck_decision_hs_lundi` : `ISODOW` vaut 1 pour lundi. Une semaine ne peut pas être désignée par un autre jour, même par un bug applicatif.
- `ck_decision_hs_minutes` : on ne peut pas valider plus que ce qui a été constaté.

J'ai aussi **élargi** `ck_journal_audit_action`, créée au Bloc 5, pour accepter les six nouvelles actions. Le motif `DROP` + `ADD` rend cela automatique sur une base existante.

### 4.7 Données de démarrage (`data.sql`)

```sql
INSERT INTO regime_travail (id, code, libelle, tolerance_retard_minutes, cree_par) VALUES
(1, 'BUREAU_40H', 'Bureau 40 h (lundi au vendredi)', 10, 'systeme'),
(2, 'SERVICE_SAMEDI_40H', 'Service 40 h (samedi matin inclus)', 10, 'systeme') ON CONFLICT DO NOTHING;

SELECT setval('regime_travail_id_seq', (SELECT MAX(id) FROM regime_travail));
```

- `ON CONFLICT DO NOTHING` : insertion unique, jamais d'écrasement.
- `setval` : j'insère des id fixes, je dois donc recaler la séquence, sinon le premier régime créé par le RH tenterait l'id 1.
- Bureau : 5 × (08:00–17:00 − 1 h) = 40 h. Service : 5 × 7 h + 5 h le samedi = 40 h.

---

## 5. Configuration

### `application.properties`

```properties
bloc6.hs-palier1-minutes=480
bloc6.taux-hs-palier1=130
bloc6.taux-hs-palier2=150
bloc6.seuil-retard-excessif-minutes=60
bloc6.seuil-presence-excessive-minutes=720
```

- `hs-palier1-minutes` : 480 minutes par semaine au premier taux, soit 8 heures.
- `seuil-retard-excessif-minutes` : au-delà, le retard est signalé comme anomalie, en plus d'être compté.
- `seuil-presence-excessive-minutes` : 12 h. Une présence de 16 h est souvent une erreur de saisie.

### `TempsTravailProperties.java`

```java
@ConfigurationProperties(prefix = "bloc6")
public class TempsTravailProperties {
    private int hsPalier1Minutes = 480;
    ...
```

- `@ConfigurationProperties` relie le préfixe `bloc6` aux attributs : `hs-palier1-minutes` alimente `hsPalier1Minutes`.
- Les valeurs par défaut servent quand la clé est absente, notamment dans les tests.
- Même modèle que `RecrutementProperties`, activé dans `RecrutementConfiguration` :

```java
@EnableConfigurationProperties({RecrutementProperties.class, TempsTravailProperties.class})
```

---

## 6. Entités et énumérations

Les nouvelles entités utilisent Lombok (`@Getter`, `@Setter`, `@NoArgsConstructor`) : le projet l'emploie déjà sur `Employe` et `Utilisateur`, et cela évite plusieurs centaines de lignes de getters sans intérêt.

### 6.1 `RegimeTravail`

```java
    @OneToMany(mappedBy = "regime", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("jourSemaine ASC")
    private List<HoraireRegime> horaires = new ArrayList<>();
```
- `mappedBy` : la clé étrangère est du côté `HoraireRegime`.
- `cascade = ALL` : sauvegarder le régime sauvegarde ses horaires.
- `@OrderBy` : la grille revient toujours de lundi à dimanche.

```java
    public void ajouterHoraire(HoraireRegime horaire) {
        horaire.setRegime(this);
        horaires.add(horaire);
    }
```
- Une relation bidirectionnelle doit être renseignée des deux côtés. Cette méthode empêche de l'oublier.

```java
    public HoraireRegime horaireDu(int jourSemaine) { ... .orElse(null); }

    public int minutesHebdomadaires() {
        return horaires.stream().mapToInt(HoraireRegime::minutesPrevues).sum();
    }
```
- `horaireDu` renvoie `null` pour un jour de repos, ce que comprend `CalculTemps`.
- `minutesHebdomadaires` remplace la colonne que je n'ai pas créée.

### 6.2 `HoraireRegime`

```java
    public int minutesPrevues() {
        return (int) Duration.between(heureDebut, heureFin).toMinutes() - pauseMinutes;
    }
```
- 08:00–17:00 avec 60 min de pause donne 480 minutes.

### 6.3 `PlanningSalarie`

```java
    public boolean couvre(LocalDate date) {
        return !date.isBefore(dateDebut) && (dateFin == null || !date.isAfter(dateFin));
    }
```
- Les deux bornes sont **incluses** : d'où les `!isBefore` et `!isAfter`, et non `isAfter`/`isBefore`.
- `dateFin == null` : la période est ouverte.

### 6.4 `Pointage`

```java
    @Column(name = "heure_entree", nullable = false)
    private LocalTime heureEntree;

    @Column(name = "heure_sortie")
    private LocalTime heureSortie;
```
- `LocalTime` correspond exactement au type SQL `TIME`.
- `saisiPar`, `dateSaisie`, `modifiePar`, `dateModification` donnent la dernière modification directement sur la ligne ; le détail complet est dans le journal d'audit.

### 6.5 `DecisionHeuresSup`

Reflète la table. `statut` utilise `EnumType.STRING`, comme toutes les énumérations du projet.

### 6.6 `StatutHeuresSup`

```java
public enum StatutHeuresSup {
    SANS_HS("Aucune heure supplémentaire"),
    A_VALIDER("À valider"),
    VALIDEE("Validée"),
    REFUSEE("Refusée");
```
- Seuls `VALIDEE` et `REFUSEE` existent en base (voir le `CHECK`). `SANS_HS` et `A_VALIDER` sont **calculés** : ils décrivent l'absence de décision.

### 6.7 `TypeAnomaliePointage`

| Type | Déclencheur |
|---|---|
| `POINTAGE_MANQUANT` | Jour prévu et passé, sans pointage |
| `SORTIE_MANQUANTE` | Jour passé avec une entrée, sans sortie |
| `TRAVAIL_JOUR_REPOS` | Pointage un jour sans horaire |
| `RETARD_EXCESSIF` | Retard supérieur au seuil (60 min) |
| `DEPART_ANTICIPE` | Sortie avant l'heure de fin |
| `PRESENCE_EXCESSIVE` | Travail supérieur au seuil (12 h) |
| `SANS_REGIME` | Salarié sans planning, impossible à évaluer |

### 6.8 `ActionAudit` (modifié)

J'ai ajouté `AFFECTATION_REGIME`, `SAISIE_POINTAGE`, `CORRECTION_POINTAGE`, `SUPPRESSION_POINTAGE`, `DECISION_HEURES_SUP` et `REOUVERTURE_HEURES_SUP`. Je **réutilise** le journal du Bloc 5 : ces actions apparaissent donc automatiquement dans l'onglet « Journal d'audit » du dossier salarié, sans une ligne de code en plus.

---

## 7. Repositories

```java
public interface PlanningSalarieRepository extends JpaRepository<PlanningSalarie, Long> {

    @EntityGraph(attributePaths = {"regime", "regime.horaires"})
    @Query("""
            select p from PlanningSalarie p
            where p.dateDebut <= :au and (p.dateFin is null or p.dateFin >= :du)
            order by p.dateDebut
            """)
    List<PlanningSalarie> chevauchant(@Param("du") LocalDate du, @Param("au") LocalDate au);
```
- La condition de chevauchement : la période commence avant la fin de l'intervalle **et** finit après son début (ou ne finit pas).
- `@EntityGraph` charge régimes et horaires en une requête. Sans cela, Hibernate émettrait une requête par régime.

```java
public interface PointageRepository extends JpaRepository<Pointage, Long> {
    List<Pointage> findByDatePointageBetween(LocalDate du, LocalDate au);
    Optional<Pointage> findByEmployeIdAndDatePointage(Long employeId, LocalDate datePointage);
}
```
- La première charge une période pour **tous** les salariés ; la seconde vérifie qu'un pointage n'existe pas déjà.

```java
public interface DecisionHeuresSupRepository extends JpaRepository<DecisionHeuresSup, Long> {
    List<DecisionHeuresSup> findBySemaineDebutBetween(LocalDate du, LocalDate au);
    Optional<DecisionHeuresSup> findByEmployeIdAndSemaineDebut(Long employeId, LocalDate semaineDebut);
    List<DecisionHeuresSup> findByEmployeIdAndSemaineDebutGreaterThanEqual(Long employeId, LocalDate semaineDebut);
}
```
- La dernière sert quand un changement de régime invalide toutes les semaines à partir d'une date.

---

## 8. DTOs

### Requêtes

```java
public record PointageRequest(
        @NotNull Long employeId,
        @NotNull LocalDate date,
        @NotNull LocalTime heureEntree,
        LocalTime heureSortie,
        @Size(max = 255) String commentaire) {
}
```
- `heureSortie` n'a pas de `@NotNull` : on peut saisir l'entrée seule.

```java
public record CorrectionPointageRequest(
        @NotNull LocalTime heureEntree,
        LocalTime heureSortie,
        @Size(max = 255) String commentaire,
        @NotBlank @Size(max = 255) String motif) {
}
```
- La date n'y figure pas : on corrige des heures, pas le jour. Pour changer de jour, on supprime et on ressaisit, ce qui laisse deux traces claires.
- `motif` est obligatoire : c'est la règle métier, exprimée dans le type.

```java
public record DecisionHeuresSupRequest(
        @NotNull Long employeId,
        @NotNull LocalDate semaineDebut,
        @NotNull StatutHeuresSup decision,
        @Min(0) int minutesValidees,
        @Size(max = 255) String commentaire) {
}
```

```java
public record RegimeTravailRequest(
        @NotBlank @Size(max = 50) String code,
        @NotBlank @Size(max = 255) String libelle,
        @Min(0) @Max(60) int toleranceRetardMinutes,
        @NotEmpty @Valid List<Horaire> horaires) {
```
- `@Valid` sur la liste : sans lui, les contraintes de chaque `Horaire` ne seraient pas vérifiées.

### Réponses

`JourTempsDto` rassemble sur une ligne ce qui était **prévu**, ce qui a été **pointé**, et ce qui en est **déduit**. C'est exactement une ligne du tableau à l'écran.

`SemaineTempsDto` regroupe les jours, les totaux, et deux records imbriqués :

```java
    public record Decision(StatutHeuresSup statut, int minutesBrutes, int minutesValidees,
                           boolean validable, String raisonBlocage, String commentaire, String decidePar) { }

    public record Bilan(int minutesDues, int minutesCompensees, int minutesRetenues,
                        int hsPayees, int hsPalier1, int hsPalier2) { }
```
- `validable` et `raisonBlocage` viennent du serveur : l'écran n'a pas à recoder la règle, il affiche la raison.

`VariablesPaieDto` est la sortie du bloc : ce que la paie et les absences consommeront. `provisoire` indique que les chiffres peuvent encore changer.

---

## 9. Le moteur de calcul — `CalculTemps`

C'est le fichier le plus important pour les chiffres. Je l'ai volontairement isolé :

```java
public final class CalculTemps {
    static final int PRESENCE_MIN_POUR_PAUSE = 300;
    private CalculTemps() { }
```
- `final` et constructeur privé : classe utilitaire, jamais instanciée.
- **Ni base de données ni Spring** : des nombres entrent, des nombres sortent. Chaque règle se teste en une ligne, sans mock.
- Toutes les durées sont en **minutes entières** : pas d'erreur d'arrondi comme avec `1.25` heure.

```java
    public record HoraireJour(LocalTime debut, LocalTime fin, int pauseMinutes) {
        public int minutesPrevues() { return minutesEntre(debut, fin) - pauseMinutes; }
    }

    public record ResultatJour(int prevues, int travaillees, int retard, int departAnticipe, int heuresSup) { }
```
- Le moteur ne connaît pas les entités : le service lui passe un `HoraireJour` simple.

### `calculerJour`

```java
    public static ResultatJour calculerJour(HoraireJour horaire, LocalTime entree, LocalTime sortie, int tolerance) {
        int prevues = horaire != null ? horaire.minutesPrevues() : 0;
        if (entree == null) {
            return new ResultatJour(prevues, 0, 0, 0, 0);
        }
```
1. `horaire == null` signifie jour de repos.
2. Sans pointage, rien n'est déduit. C'est l'appelant qui décide s'il s'agit d'une absence, car cela dépend de la date du jour, que le moteur ignore volontairement.

```java
        int retard = horaire != null ? retard(horaire, entree, tolerance) : 0;
        if (sortie == null) {
            return new ResultatJour(prevues, 0, retard, 0, 0);
        }
```
3. Le retard se calcule dès l'entrée. Sans sortie, je ne peux pas calculer le temps travaillé : je garde le retard et je laisse le travail à 0 plutôt que d'inventer une valeur.

```java
        int travaillees = minutesTravaillees(horaire, entree, sortie);
        if (horaire == null) {
            return new ResultatJour(0, travaillees, 0, 0, travaillees);
        }
```
4. Un jour de repos travaillé est entièrement en heures sup, sans retard possible.

```java
        int depart = sortie.isBefore(horaire.fin()) ? minutesEntre(sortie, horaire.fin()) : 0;
        int departPlafonne = Math.min(depart, Math.max(0, prevues - travaillees - retard));
```
5. Le départ anticipé est plafonné. Exemple d'horaire 08:00–17:00 avec 1 h de pause, salarié présent de 08:00 à 12:00 : l'écart avec 17:00 est de 5 h, mais il ne manque que 4 h, car l'heure de pause n'était pas due. Sans plafond, je retiendrais une heure de trop. C'est le test `laPauseNestPasDeduiteDUneDemiJournee`.

```java
        LocalTime debutHs = entree.isAfter(horaire.fin()) ? entree : horaire.fin();
        int heuresSup = sortie.isAfter(debutHs) ? minutesEntre(debutHs, sortie) : 0;
```
6. Les heures sup comptent à partir de l'heure de fin. Si le salarié arrive après l'heure de fin, on part de son entrée, sinon on compterait du temps où il n'était pas là.

**Pourquoi compter les heures sup après l'heure de fin, et non « travaillé − prévu » ?** C'est ce qui rend la compensation juste. Un salarié qui arrive à 08:45 et part à 17:45 a travaillé ses 8 heures. Avec « travaillé − prévu », il aurait 45 min de retard et 0 heure sup, donc 45 min retenues pour une journée complète. Avec ma règle, il a 45 min de retard **et** 45 min d'heures sup, qui se compensent une fois validées.

### `compenser`

```java
    public static Compensation compenser(int minutesDues, int hsValidees, int palier1Minutes) {
        int compensees = Math.min(minutesDues, hsValidees);
        int hsPayees = hsValidees - compensees;
        int retenues = minutesDues - compensees;
        int palier1 = Math.min(hsPayees, palier1Minutes);
        int palier2 = hsPayees - palier1;
        return new Compensation(minutesDues, hsValidees, compensees, hsPayees, retenues, palier1, palier2);
    }
```

C'est le **traitement du retard avec les heures supplémentaires** :

1. `compensees` : on compense autant que possible, sans dépasser ni les minutes dues, ni les heures validées.
2. `hsPayees` : ce qui reste d'heures sup.
3. `retenues` : ce qui reste de minutes dues.
4. Par construction, l'un des deux restes vaut **toujours 0** : un salarié ne peut pas à la fois être payé en heures sup et subir une retenue la même semaine.
5. Les paliers s'appliquent **après** compensation : on ne majore que ce qui est réellement payé.

La compensation consomme les heures sup à **1 pour 1**, sans majoration : une minute de retard efface une minute d'heure sup.

### Méthodes privées

```java
    private static int retard(HoraireJour horaire, LocalTime entree, int tolerance) {
        if (!entree.isAfter(horaire.debut())) {
            return 0;
        }
        LocalTime finRetard = entree.isAfter(horaire.fin()) ? horaire.fin() : entree;
        int retard = minutesEntre(horaire.debut(), finRetard);
        return retard > tolerance ? retard : 0;
    }
```
- `retard > tolerance` : à 10 min de tolérance, 08:10 est pardonné, 08:11 compte pour **11 minutes**. C'est un effet de seuil voulu.
- `finRetard` plafonne le retard à la journée prévue.

```java
    private static int minutesTravaillees(HoraireJour horaire, LocalTime entree, LocalTime sortie) {
        int presence = minutesEntre(entree, sortie);
        int pause = horaire != null && presence >= PRESENCE_MIN_POUR_PAUSE ? horaire.pauseMinutes() : 0;
        return presence - pause;
    }
```
- Sous 5 h de présence, le salarié n'a pas pris la pause du midi : je ne la déduis pas.

---

## 10. Lecture et calculs — `TempsTravailService`

```java
@Service
@Transactional(readOnly = true)
public class TempsTravailService {
```
- Toute la classe est en lecture seule. Comme pour le Bloc 5, la présence de `Clock horloge` et d'un second constructeur *package* permet de figer la date dans les tests.

### 10.1 Charger une fois, calculer en mémoire

```java
    private record Donnees(Map<Long, Map<LocalDate, Pointage>> pointages,
                           Map<Long, List<PlanningSalarie>> plannings,
                           Map<Long, Map<LocalDate, DecisionHeuresSup>> decisions) {
```
- Calculer un mois pour tous les salariés demanderait des centaines de requêtes si chaque jour interrogeait la base. Je charge **une fois** toute la période, je range le tout dans des `Map`, puis je lis en mémoire.

```java
    private Donnees chargerDonnees(LocalDate du, LocalDate au) {
        Map<Long, Map<LocalDate, Pointage>> pointages = new HashMap<>();
        for (Pointage pointage : pointageRepository.findByDatePointageBetween(du, au)) {
            pointages.computeIfAbsent(pointage.getEmploye().getId(), id -> new HashMap<>())
                    .put(pointage.getDatePointage(), pointage);
        }
```
- `computeIfAbsent` crée la sous-`Map` à la première rencontre du salarié.

### 10.2 `calculerSemaine`

```java
        for (LocalDate date = lundi; !date.isAfter(dimanche); date = date.plusDays(1)) {
            PlanningSalarie planning = planningDu(employe, date, donnees);
            if (planning != null) {
                regimeLibelle = planning.getRegime().getLibelle();
            }
            jours.add(calculerJour(employe, date, planning, donnees.pointage(employe, date), aujourdhui));
        }
```
- Le planning est recherché **pour chaque jour** : un changement de régime peut tomber un mercredi.

```java
        DecisionHeuresSup existante = donnees.decision(employe, lundi);
        SemaineTempsDto.Decision decision = construireDecision(existante, hsBrutes, jours, dimanche, aujourdhui);
        CalculTemps.Compensation bilan = CalculTemps.compenser(
                retard + depart, decision.minutesValidees(), reglages.getHsPalier1Minutes());
```
- `retard + depart` : ce sont les minutes dues.
- `decision.minutesValidees()` vaut 0 tant que rien n'est décidé. Conséquence voulue : **sans validation, rien n'est compensé**. Le bilan affiche la retenue maximale jusqu'à la décision du RH.

### 10.3 `calculerJour`

```java
        boolean dansEffectif = !date.isBefore(employe.getDateEntree());
        RegimeTravail regime = planning != null ? planning.getRegime() : null;
        HoraireRegime prevu = regime != null && dansEffectif ? regime.horaireDu(date.getDayOfWeek().getValue()) : null;
```
- Avant son entrée, un salarié n'est attendu nulle part : pas d'absence fantôme.

```java
        CalculTemps.ResultatJour resultat = regime == null
                ? new CalculTemps.ResultatJour(0, 0, 0, 0, 0)
                : CalculTemps.calculerJour(horaire, ...);
```
- **Sans régime, je ne calcule rien.** Le moteur traiterait `horaire == null` comme un repos et transformerait tout le pointage en heures sup. Je préfère ne rien affirmer et lever `SANS_REGIME`. Ce cas a son test dédié.

```java
        boolean absent = horaire != null && pointage == null && date.isBefore(aujourdhui);
```
- Strictement avant aujourd'hui : à 9 h, un salarié sans pointage n'est pas absent, le RH n'a peut-être pas encore saisi.

### 10.4 `detecterAnomalies`

Une suite de conditions indépendantes : un même jour peut cumuler plusieurs anomalies (retard important **et** sortie manquante). `date.isBefore(aujourdhui)` évite de signaler une sortie manquante pour la journée en cours.

### 10.5 `construireDecision` et `raisonBlocage`

```java
    private static String raisonBlocage(int hsBrutes, List<JourTempsDto> jours, LocalDate dimanche, LocalDate aujourdhui) {
        if (hsBrutes == 0) {
            return "Aucune heure supplémentaire sur cette semaine.";
        }
        if (!dimanche.isBefore(aujourdhui)) {
            return "La semaine n'est pas terminée.";
        }
        boolean sortieManquante = jours.stream()
                .anyMatch(jour -> jour.pointageId() != null && jour.heureSortie() == null);
        return sortieManquante ? "Une sortie reste à pointer." : null;
    }
```
- `null` veut dire « rien ne bloque ».
- Décider une semaine incomplète reviendrait à valider des chiffres qui peuvent encore changer.

### 10.6 `heuresSupAValider`

Parcourt les **12 dernières semaines terminées** et garde celles en `A_VALIDER`. Je renvoie aussi les semaines bloquées, avec leur raison : le RH doit savoir qu'il a une sortie à compléter.

### 10.7 `anomalies(du, au)`

```java
        if (ChronoUnit.DAYS.between(du, au) > PERIODE_ANOMALIES_MAX_JOURS) {
            throw ApiException.badRequest("PERIODE_TROP_LONGUE", ...);
        }
```
- Plafond de 92 jours : cela évite de recalculer des années sur une simple recherche.
- Le calcul se fait par semaines entières, puis je filtre les dates demandées.

### 10.8 `variablesPaie(mois)`

```java
    static List<LocalDate> lundisDu(YearMonth mois) {
        List<LocalDate> lundis = new ArrayList<>();
        LocalDate lundi = mois.atDay(1).with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY));
        while (YearMonth.from(lundi).equals(mois)) { ... }
```
- **Une semaine appartient au mois de son lundi.** Si je découpais une semaine à cheval sur deux mois, je ne saurais pas à quel mois rattacher la compensation, qui est hebdomadaire. Avec cette règle, chaque semaine compte une et une seule fois.

```java
    static BigDecimal tauxHoraire(BigDecimal salaire, int minutesHebdomadaires) {
        if (minutesHebdomadaires == 0) {
            return null;
        }
        return salaire.multiply(BigDecimal.valueOf(12L * 60))
                .divide(BigDecimal.valueOf(52L * minutesHebdomadaires), 6, RoundingMode.HALF_UP);
    }
```
- Heures mensuelles = heures hebdomadaires × 52 / 12, soit 173,33 h pour 40 h.
- La formule regroupe tout pour diviser **une seule fois**, à la fin. Passer par `173,33` introduirait une erreur d'arrondi.
- 6 décimales en interne, 2 à l'affichage.

```java
    private BigDecimal montantHeuresSup(BigDecimal taux, int palier1, int palier2) {
        BigDecimal minutesPonderees = BigDecimal.valueOf((long) palier1 * reglages.getTauxHsPalier1()
                + (long) palier2 * reglages.getTauxHsPalier2());
        return taux.multiply(minutesPonderees).divide(BigDecimal.valueOf(60L * 100), 2, RoundingMode.HALF_UP);
    }
```
- `/ (60 × 100)` : les minutes deviennent des heures, les pourcentages des coefficients.
- `BigDecimal` et non `double` : on manipule de l'argent.

Le salaire vient de `DossierSalarieService.contratEnCours` : je **réutilise** la règle du Bloc 5, le contrat en cours reste défini à un seul endroit. Si le salarié n'a ni régime ni contrat, les montants restent `null` plutôt qu'un 0 trompeur.

---

## 11. Écritures — `PointageService`

```java
@Service
@Transactional
public class PointageService {
```
- Chaque méthode publique est atomique : le pointage et sa trace sont enregistrés ensemble ou pas du tout.

### 11.1 `saisir`

```java
        verifierDate(employe, requete.date());
        verifierHeures(requete.heureEntree(), requete.heureSortie());
        if (pointageRepository.findByEmployeIdAndDatePointage(employe.getId(), requete.date()).isPresent()) {
            throw ApiException.conflit("POINTAGE_EXISTANT", ...);
        }
```
- Je vérifie l'existence pour renvoyer un message clair ; la contrainte `UNIQUE` reste le vrai garde-fou si deux requêtes se croisent.

```java
        trace.tracer(employe, ActionAudit.SAISIE_POINTAGE, auteur, decrire(pointage));
        rouvrirSemaine(employe, requete.date(), auteur);
        return tempsService.semaine(employe.getId(), requete.date());
```
- Ajouter un pointage change les chiffres de la semaine : toute décision existante est rouverte.
- Je renvoie la semaine recalculée : l'écran se met à jour sans deuxième appel.

### 11.2 `corriger`

```java
        List<String> changements = new ArrayList<>();
        comparer(changements, "Entrée", pointage.getHeureEntree(), requete.heureEntree());
        comparer(changements, "Sortie", pointage.getHeureSortie(), requete.heureSortie());
        comparer(changements, "Commentaire", pointage.getCommentaire(), nettoyer(requete.commentaire()));
        if (changements.isEmpty()) {
            return tempsService.semaine(employe.getId(), pointage.getDatePointage());
        }
```
- Même technique que `modifierIdentite` au Bloc 5 : comparer **avant** d'écrire.
- Si rien ne change, on s'arrête : pas de trace inutile et, surtout, **pas de réouverture** d'une décision valable.

### 11.3 `supprimer`

```java
        trace.tracer(employe, ActionAudit.SUPPRESSION_POINTAGE, auteur, decrire(pointage) + " — motif : " + motif.trim());
        pointageRepository.delete(pointage);
        pointageRepository.flush();
        rouvrirSemaine(employe, date, auteur);
        return tempsService.semaine(employe.getId(), date);
```
- La trace part **avant** la suppression et contient les heures supprimées : le journal garde la mémoire du fait effacé.
- `flush()` force le `DELETE` tout de suite. Sinon, la relecture de la semaine pourrait encore voir le pointage.

### 11.4 `deciderHeuresSup`

```java
        SemaineTempsDto semaine = tempsService.semaine(requete.employeId(), lundi);
        SemaineTempsDto.Decision etat = semaine.decision();
        if (!etat.validable()) {
            throw ApiException.conflit("HEURES_SUP_NON_DECIDABLES", etat.raisonBlocage());
        }
```
- Je **recalcule** côté serveur. Je ne fais jamais confiance au chiffre affiché à l'écran : `minutesBrutes` vient d'ici, pas de la requête.
- La règle de blocage n'est écrite qu'une fois, partagée entre l'affichage et l'écriture.

```java
        if (validee && (minutes < 1 || minutes > etat.minutesBrutes())) { ... }
        if ((!validee || minutes < etat.minutesBrutes()) && commentaire == null) { ... }
```
- Valider plus que le constaté est impossible. Un refus ou une validation partielle doit être justifié : le salarié perd du temps payé, il faut pouvoir expliquer pourquoi.

```java
        DecisionHeuresSup decision = decisionRepository
                .findByEmployeIdAndSemaineDebut(requete.employeId(), lundi)
                .orElseGet(DecisionHeuresSup::new);
```
- Le RH peut changer d'avis : la décision est remplacée, et la trace d'audit garde l'ancienne valeur (`remplace VALIDEE : 122 min`).

### 11.5 Vérifications

```java
    private void verifierDate(EmployeEmbauche employe, LocalDate date) {
        if (date.isAfter(LocalDate.now(horloge))) { ... "DATE_FUTURE" ... }
        if (date.isBefore(employe.getDateEntree())) { ... "DATE_AVANT_ENTREE" ... }
        if (employe.getStatut() == StatutEmploye.SORTI) { ... "SALARIE_SORTI" ... }
    }
```
- Un pointage est un fait : il ne peut pas être dans le futur. Une date passée reste autorisée, car le RH saisit a posteriori.

---

## 12. Traçabilité — `TraceTempsService`

```java
    public void tracer(EmployeEmbauche employe, ActionAudit action, String auteur, String details) {
        journalRepository.save(new JournalAudit(employe, action, auteur != null ? auteur : "inconnu", details));
    }
```
- Écrit dans le **même journal** que le Bloc 5.

```java
    public void rouvrirDecision(EmployeEmbauche employe, LocalDate lundi, String raison, String auteur) {
        decisionRepository.findByEmployeIdAndSemaineDebut(employe.getId(), lundi)
                .ifPresent(decision -> rouvrir(decision, raison, auteur));
    }

    public void rouvrir(DecisionHeuresSup decision, String raison, String auteur) {
        tracer(decision.getEmploye(), ActionAudit.REOUVERTURE_HEURES_SUP, auteur,
                "Semaine du " + decision.getSemaineDebut() + " : décision " + decision.getStatut()
                        + " (" + decision.getMinutesValidees() + " min validées) annulée. " + raison);
        decisionRepository.delete(decision);
        decisionRepository.flush();
    }
```
- **Pourquoi rouvrir au lieu de garder la décision ?** Elle portait sur des chiffres qui n'existent plus. La garder pourrait payer des heures sup qui n'ont plus lieu d'être, puisque le RH les a validées en voyant autre chose.
- La ligne disparaît, mais le journal conserve ce qui était décidé. Rien n'est perdu.
- J'ai isolé ce service parce que `PointageService` et `RegimeTravailService` en ont tous deux besoin.

---

## 13. Régimes et planning — `RegimeTravailService`

### `creer`

```java
        String code = requete.code().trim().toUpperCase(Locale.ROOT);
        if (regimeRepository.findByCodeIgnoreCase(code).isPresent()) { ... "REGIME_EXISTANT" ... }

        Set<Integer> jours = new HashSet<>();
        for (RegimeTravailRequest.Horaire horaire : requete.horaires()) {
            if (!jours.add(horaire.jourSemaine())) { ... "JOUR_EN_DOUBLE" ... }
            if (!horaire.heureFin().isAfter(horaire.heureDebut())) { ... "HORAIRE_INVALIDE" ... }
            long amplitude = Duration.between(horaire.heureDebut(), horaire.heureFin()).toMinutes();
            if (horaire.pauseMinutes() >= amplitude) { ... "PAUSE_INVALIDE" ... }
```
- `Set.add` renvoie `false` si la valeur existe déjà : détection de doublon en une ligne.
- Il n'y a **pas de modification** de régime. Changer un horaire recalculerait toutes les semaines passées et leurs décisions. Pour changer d'horaires, on crée un nouveau régime et on l'affecte à une date.

### `affecter`

```java
        List<PlanningSalarie> existants = planningRepository.findByEmployeIdOrderByDateDebutAsc(employeId);
        if (existants.stream().anyMatch(planning -> !planning.getDateDebut().isBefore(debut))) {
            throw ApiException.conflit("PLANNING_POSTERIEUR", ...);
        }
        existants.stream()
                .filter(planning -> planning.getDateFin() == null || !planning.getDateFin().isBefore(debut))
                .forEach(planning -> planning.setDateFin(debut.minusDays(1)));
```
- On ne réécrit pas une période déjà commencée ou prévue après la date choisie.
- La période en cours est **fermée la veille** : pas de trou, pas de chevauchement, et l'historique reste consultable.
- Pas besoin d'appeler `save` sur la période fermée : l'entité est suivie par Hibernate dans la transaction.

```java
        decisionRepository.findByEmployeIdAndSemaineDebutGreaterThanEqual(employeId, TempsTravailService.lundiDe(debut))
                .forEach(decision -> trace.rouvrir(decision, "Le régime de travail a changé.", auteur));
```
- Une affectation dans le passé change ce qui était prévu : les décisions de ces semaines ne tiennent plus.

---

## 14. Contrôleur REST — `TempsTravailController`

```java
@RestController
@RequestMapping("/api/bloc6")
public class TempsTravailController {
```
- Le contrôleur ne contient aucune règle : il traduit HTTP vers les trois services.

```java
    @GetMapping("/variables-paie")
    public List<VariablesPaieDto> variablesPaie(@RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth mois) {
        return tempsService.variablesPaie(mois);
    }
```
- `YearMonth` est converti depuis `2026-10`, le format d'un `<input type="month">`.

```java
    @DeleteMapping("/pointages/{id}")
    public SemaineTempsDto supprimer(@PathVariable Long id, @RequestParam String motif, Authentication auth) {
```
- Le motif passe en paramètre d'URL, car une requête `DELETE` n'a pas de corps fiable. Elle renvoie la semaine recalculée, donc `200` et non `204`.

`auth.getName()` donne l'email de l'utilisateur connecté : l'auteur d'une action vient du serveur, jamais de la requête.

---

## 15. Sécurité

```java
                        .requestMatchers("/api/bloc5/**").hasRole("RH")
                        // Bloc 6 : pointages et variables de paie, reserves au RH.
                        .requestMatchers("/api/bloc6/**").hasRole("RH")
                        .requestMatchers("/api/**").hasAnyRole("MANAGER", "RH", "FINANCE", "DIRECTION")
```
- Même technique qu'au Bloc 5, et même raison : ma règle précède la règle générique, et une règle d'URL renvoie un vrai 403 alors qu'un `@PreAuthorize` serait transformé en 500 par `RecrutementExceptionHandler`.

Vérifié : RH → 200, manager → 403, anonyme → 401.

---

## 16. Frontend

### 16.1 `utils/format.js`

```js
export function duree(minutes) {
  const total = Number(minutes) || 0
  if (total === 0) return '—'
  const heures = Math.floor(total / 60)
  const reste = total % 60
  if (heures === 0) return `${reste} min`
  return reste === 0 ? `${heures} h` : `${heures} h ${String(reste).padStart(2, '0')}`
}
```
- Le backend parle en minutes ; l'écran affiche `2 h 15`. `padStart` donne `1 h 05` et non `1 h 5`.

```js
export function heure(valeur) { return valeur ? valeur.slice(0, 5) : '' }

export function dateIso(valeur = new Date()) { ... }
```
- `heure` : l'API renvoie `08:00:00`, un `<input type="time">` attend `08:00`.
- `dateIso` : j'évite `toISOString()`, qui passe en UTC et peut donner la date de la veille selon le fuseau.

### 16.2 `api/temps.js`

Un module par bloc, comme `api/dossiers.js`. Une seule particularité :

```js
supprimerPointage: (id, motif) => apiDelete(`${BASE}/pointages/${id}?motif=${encodeURIComponent(motif)}`),
```
- `encodeURIComponent` : un motif peut contenir des espaces et des accents.

### 16.3 `views/TempsTravailView.vue`

```js
const cible = ref(null)

function ouvrirSemaine(destination) {
  cible.value = destination
  onglet.value = 'pointages'
}
```
- Depuis une anomalie ou une semaine à valider, le bouton amène directement sur la bonne semaine du bon salarié. La vue parente porte cet état parce que les onglets sont des composants frères.

### 16.4 `components/temps/PointagesSemaine.vue`

```js
function remplirLignes(donnees) {
  Object.keys(lignes).forEach((cle) => delete lignes[cle])
  donnees.jours.forEach((jour) => {
    lignes[jour.date] = {
      entree: heure(jour.heureEntree),
      sortie: heure(jour.heureSortie),
      commentaire: jour.commentaire ?? '',
      motif: ''
    }
  })
}
```
- `lignes` est une **copie modifiable** de la semaine. L'écran garde ainsi les deux versions : celle du serveur et celle en cours de saisie.

```js
function modifiee(jour) {
  const ligne = lignes[jour.date]
  return ligne.entree !== heure(jour.heureEntree)
    || ligne.sortie !== heure(jour.heureSortie)
    || ligne.commentaire !== (jour.commentaire ?? '')
}
```
- Le bouton « Enregistrer » et le champ « Motif » n'apparaissent que sur la ligne modifiée.

```js
async function enregistrer(jour) {
  ...
    if (jour.pointageId) {
      afficher(await tempsApi.corrigerPointage(jour.pointageId, { ... motif: ligne.motif }))
    } else {
      afficher(await tempsApi.saisirPointage({ employeId: employeId.value, date: jour.date, ... }))
    }
```
- La présence d'un `pointageId` décide entre correction et saisie. `ligne.sortie || null` transforme le champ vide en `null`, que le backend sait interpréter.
- Dans les deux cas, je réaffiche la semaine renvoyée par le serveur : les calculs ne sont **jamais** refaits côté navigateur.

```js
const dejaDecidee = computed(() => ['VALIDEE', 'REFUSEE'].includes(semaine.value?.decision.statut))
```
- Une fois décidée, le formulaire se replie derrière « Modifier la décision » : un formulaire ouvert laisserait croire qu'il reste quelque chose à faire.

Dans le gabarit, les champs des jours futurs sont désactivés (`:disabled="jour.date > aujourdhui"`) : la comparaison de chaînes fonctionne car le format `AAAA-MM-JJ` se trie comme les dates.

### 16.5 `components/temps/DecisionHeuresSup.vue`

```js
const prefixe = computed(() => `decision-${props.semaine.employeId}-${props.semaine.semaineDebut}`)

function initialiser() {
  const brutes = props.semaine.decision.minutesBrutes
  heures.value = Math.floor(brutes / 60)
  minutes.value = brutes % 60
  commentaire.value = ''
}
```
- Réutilisé dans deux écrans. Le formulaire est pré-rempli avec le total constaté : le cas courant se valide en un clic.
- `prefixe` : ce composant apparaît plusieurs fois dans la liste « à valider », et chaque `id` doit être unique pour que les `<label for>` fonctionnent.

```js
minutesValidees: decision === 'VALIDEE' ? heures.value * 60 + minutes.value : 0,
```
- Le RH raisonne en heures et minutes, l'API en minutes.

### 16.6 `HeuresSupAValider.vue`, `AnomaliesPointage.vue`

- La liste « à valider » se recharge après chaque décision : la semaine traitée disparaît.
- Les semaines bloquées affichent leur raison au lieu du formulaire.
- Dans les anomalies, chaque ligne a un bouton « Corriger » qui émet `ouvrir` vers la vue parente.

### 16.7 `VariablesPaie.vue`

```js
function moisPrecedent() {
  const jour = new Date()
  jour.setDate(1)
  jour.setMonth(jour.getMonth() - 1)
  return dateIso(jour).slice(0, 7)
}
```
- Par défaut, le mois précédent : on prépare une paie sur un mois terminé.
- `setDate(1)` **d'abord** : le 31 mars moins un mois donnerait sinon « 31 février », que JavaScript transforme en mars.
- La ligne « Provisoire » ou « Prêt pour la paie » est calculée par le serveur. Le texte d'aide rappelle que les jours d'absence ne sont pas dans le montant retenu.

### 16.8 `RegimesTravail.vue`

- Trois blocs : affecter un régime, consulter les régimes, en créer un.
- `regimeVide()` pré-remplit le formulaire avec un lundi-vendredi 08:00–17:00 : c'est le cas le plus fréquent.
- À l'envoi, seuls les jours cochés sont transmis : un jour absent de la liste est un repos, exactement comme en base.

### 16.9 Intégration

- `router/index.js` : route `bloc6/temps`, `meta.roles: ['RH']`.
- `components/AppLayout.vue` : entrée « Temps de travail » visible par le RH.
- `components/RecrutementStatutBadge.vue` : libellés et couleurs de `A_VALIDER`, `SANS_HS`, `VALIDEE`, `REFUSEE`.

---

## 17. Tests

### `CalculTempsTest` — 13 tests, sans aucun mock

| Test | Règle vérifiée |
|---|---|
| `uneJourneeNormaleDonneHuitHeuresSansRetard` | 08:00–17:00 donne 480 min, rien d'autre |
| `unRetardDansLaToleranceEstPardonne` | 08:10 avec 10 min de tolérance → 0 |
| `auDelaDeLaToleranceToutLeRetardCompte` | 08:11 → 11 min, et non 1 |
| `uneSortieTardiveDonneDesHeuresSup` | Sortie à 19:30 → 150 min |
| `uneArriveeEnAvanceNeDonnePasDHeuresSup` | Entrée à 07:00 → 0 |
| `unDepartAnticipeEstCompte` | Sortie à 16:00 → 60 min |
| `laPauseNestPasDeduiteDUneDemiJournee` | 08:00–12:00 → 240 travaillées, 240 dues |
| `uneEntreeSansSortieGardeLeRetardMaisPasLeTravail` | Retard conservé, travail à 0 |
| `sansPointageRienNEstDeduit` | Aucune déduction inventée |
| `unJourDeReposEstEntierementEnHeuresSup` | Samedi 3 h → 180 min d'HS |
| `lesHeuresSupCompensentDAbordLesRetards` | 45 dues, 120 HS → 75 payées, 0 retenue |
| `leRetardRestantEstRetenu` | 120 dues, 30 HS → 90 retenues |
| `lesHeuresSupPayeesSontRepartiesEntreLesDeuxPaliers` | 540 payées → 480 + 60 |

### `TempsTravailServiceTest` — 10 tests avec dépôts simulés et horloge figée au 04/11/2026

| Test | Ce qu'il vérifie |
|---|---|
| `laSemaineCompenseLeRetardDuLundiParLesHeuresSupValideesDuJeudi` | Le cœur de la demande : compensation sur plusieurs jours |
| `desHeuresSupNonValideesNeCompensentRien` | Sans décision, tout le retard est retenu |
| `detecteLesAnomaliesDePointage` | Retard, sortie, départ, repos, jours manquants |
| `aujourdhuiEtLesJoursAvenirNeSontPasDesAbsences` | Seuls les jours passés comptent |
| `unSalarieSansRegimeEstSignaleSansInventerDeRetard` | Pas de calcul sans référence |
| `onNePeutPasDeciderSurUneSemaineEnCours` | Blocage et aucune écriture |
| `uneValidationPartielleDoitEtreJustifiee` | Commentaire obligatoire |
| `corrigerUnPointageRouvreLaDecisionDeLaSemaine` | Réouverture et trace de l'auteur |
| `uneCorrectionSansChangementNeRouvreRien` | Pas de réouverture inutile |
| `lesVariablesDePaieCalculentLesMontantsAvecLaMajoration` | 2 080 000 Ar → 12 000 Ar/h ; 3 h à 130 % = 46 800 Ar |

```bash
cd backend
./mvnw test -Dtest='CalculTempsTest,TempsTravailServiceTest'
```

Résultat sur tout le backend (Blocs 1 à 6) : **47 tests, 0 échec**. Les tests d'intégration ont besoin d'une base PostgreSQL.

### Parcours dans un vrai navigateur

J'ai aussi piloté l'interface avec Playwright et Chromium, sur une base de test : 18 vérifications, de la correction d'un pointage à la création d'un régime. Ce script n'est pas conservé dans le dépôt. Ce parcours a révélé des défauts que les tests d'API ne pouvaient pas voir, depuis corrigés : les libellés du formulaire de décision n'étaient pas reliés à leurs champs, et le tableau de paie débordait.

---

## 18. Référence de l'API

Toutes les routes exigent l'authentification HTTP Basic d'un utilisateur **RH**.

| Méthode | URL | Corps / paramètres | Réponse | Audit |
|---|---|---|---|---|
| GET | `/api/bloc6/regimes` | — | `RegimeTravailDto[]` | non |
| POST | `/api/bloc6/regimes` | `RegimeTravailRequest` | 201 `RegimeTravailDto` | non (`cree_par`) |
| GET | `/api/bloc6/employes/{id}/plannings` | — | `PlanningSalarieDto[]` | non |
| POST | `/api/bloc6/employes/{id}/plannings` | `AffectationRegimeRequest` | 201 `PlanningSalarieDto` | AFFECTATION_REGIME |
| GET | `/api/bloc6/employes/{id}/semaine` | `?date=2026-10-28` (n'importe quel jour) | `SemaineTempsDto` | non |
| POST | `/api/bloc6/pointages` | `PointageRequest` | 201 `SemaineTempsDto` | SAISIE_POINTAGE |
| PUT | `/api/bloc6/pointages/{id}` | `CorrectionPointageRequest` | `SemaineTempsDto` | CORRECTION_POINTAGE |
| DELETE | `/api/bloc6/pointages/{id}` | `?motif=...` | `SemaineTempsDto` | SUPPRESSION_POINTAGE |
| GET | `/api/bloc6/heures-sup/a-valider` | — | `SemaineTempsDto[]` | non |
| POST | `/api/bloc6/heures-sup/decision` | `DecisionHeuresSupRequest` | `SemaineTempsDto` | DECISION_HEURES_SUP |
| GET | `/api/bloc6/anomalies` | `?du=...&au=...` (92 jours max) | `AnomaliePointageDto[]` | non |
| GET | `/api/bloc6/variables-paie` | `?mois=2026-10` | `VariablesPaieDto[]` | non |

Codes d'erreur métier :

| HTTP | code | Cause |
|---|---|---|
| 400 | `VALIDATION` | Champ invalide (liste dans `details`) |
| 400 | `REQUETE_INVALIDE` | Paramètre manquant ou valeur d'enum inconnue |
| 400 | `DATE_FUTURE`, `DATE_AVANT_ENTREE` | Date de pointage ou de planning impossible |
| 400 | `HEURES_INCOHERENTES` | Sortie avant l'entrée |
| 400 | `MOTIF_OBLIGATOIRE` | Suppression sans motif |
| 400 | `SEMAINE_INVALIDE`, `DECISION_INVALIDE`, `MINUTES_INVALIDES`, `COMMENTAIRE_OBLIGATOIRE` | Décision incorrecte |
| 400 | `JOUR_EN_DOUBLE`, `HORAIRE_INVALIDE`, `PAUSE_INVALIDE` | Grille de régime incorrecte |
| 400 | `PERIODE_INVALIDE`, `PERIODE_TROP_LONGUE` | Recherche d'anomalies incorrecte |
| 401 / 403 | — | Non authentifié / rôle autre que RH |
| 404 | `INTROUVABLE` | Salarié, pointage ou régime absent |
| 409 | `POINTAGE_EXISTANT` | Un pointage existe déjà ce jour-là |
| 409 | `SALARIE_SORTI` | Salarié sorti des effectifs |
| 409 | `HEURES_SUP_NON_DECIDABLES` | Semaine en cours, sortie manquante ou aucune heure sup |
| 409 | `REGIME_EXISTANT`, `PLANNING_POSTERIEUR` | Conflit de régime ou de période |

Exemples :

```bash
# Semaine contenant le 28 octobre
curl -u rh@rh.com:rh123 "http://localhost:8080/api/bloc6/employes/1/semaine?date=2026-10-28"

# Saisir un pointage
curl -u rh@rh.com:rh123 -X POST -H 'Content-Type: application/json' \
  http://localhost:8080/api/bloc6/pointages \
  -d '{"employeId":1,"date":"2026-11-02","heureEntree":"08:45","heureSortie":"17:00"}'

# Valider 2 h d'heures sup
curl -u rh@rh.com:rh123 -X POST -H 'Content-Type: application/json' \
  http://localhost:8080/api/bloc6/heures-sup/decision \
  -d '{"employeId":1,"semaineDebut":"2026-10-26","decision":"VALIDEE","minutesValidees":120}'

# Variables de paie
curl -u rh@rh.com:rh123 "http://localhost:8080/api/bloc6/variables-paie?mois=2026-10"
```

---

## 19. Flux complets

### Lecture d'une semaine

```
GET /employes/1/semaine?date=2026-10-28
  → lundiDe(date) = 26/10
  → chargerDonnees(26/10, 01/11)   pointages + plannings + décisions (3 requêtes)
  → pour chaque jour :
       planning du jour → horaire prévu
       CalculTemps.calculerJour → travaillé, retard, départ, heures sup
       detecterAnomalies
  → sommes de la semaine
  → construireDecision → statut, validable, raison de blocage
  → CalculTemps.compenser(retards + départs, minutes validées)
  ← SemaineTempsDto
```

### Validation des heures sup

```
RH clique « Valider »
  → POST /heures-sup/decision
  → la date est-elle un lundi ?          400 sinon
  → recalcul de la semaine côté serveur
  → semaine terminée, sorties complètes ? 409 sinon
  → minutes cohérentes, justification ?   400 sinon
  → enregistrement de la décision + trace DECISION_HEURES_SUP   [commit]
  ← semaine recalculée : compensation appliquée
```

### Correction d'un pointage déjà décidé

```
RH modifie une heure et saisit un motif
  → PUT /pointages/{id}
  → comparaison avant / après             rien n'a changé : on s'arrête
  → mise à jour du pointage
  → trace CORRECTION_POINTAGE (motif, ancienne → nouvelle valeur)
  → décision de la semaine annulée + trace REOUVERTURE_HEURES_SUP   [commit]
  ← semaine « À valider » : le RH doit se prononcer sur les nouveaux chiffres
```

---

## 20. Exemples chiffrés

Régime bureau : 08:00–17:00, 1 h de pause, 10 min de tolérance.

### Compensation complète

| Jour | Entrée | Sortie | Retard | Heures sup |
|---|---|---|---|---|
| Lundi | 08:45 | 17:00 | 45 min | — |
| Mardi | 08:00 | 17:00 | — | — |
| Mercredi | 07:58 | 17:02 | — | 2 min |
| Jeudi | 08:00 | 19:00 | — | 2 h |
| Vendredi | 08:05 | 17:00 | toléré | — |

Le RH valide les 122 min constatées :

- Minutes dues : 45 min.
- Compensé : 45 min.
- Retenu : 0.
- Heures sup payées : 122 − 45 = **77 min**, soit 1 h 17 à 130 %.

Avec un salaire de 1 250 000 Ar, le taux horaire est de 1 250 000 × 12 / (52 × 40) = 3 605,77 Ar. Le montant vaut 3 605,77 × 77/60 × 1,30 ≈ **6 016 Ar**.

### Retard supérieur aux heures sup

Semaine avec 3 h 10 dues et des heures sup refusées : rien n'est compensé, **3 h 10** sont retenues. À 2 596 Ar/h, cela représente environ **8 221 Ar**.

---

## 21. Procédure de vérification

1. **Tests** : `cd backend && ./mvnw test -Dtest='CalculTempsTest,TempsTravailServiceTest'` → 23 tests.
2. **Démarrage** : au prochain lancement, `schema.sql` crée les tables et `data.sql` ajoute les deux régimes.
3. **Données de test**, après `database/bloc5-donnees-test.sql` :

   ```bash
   docker exec -i postgres-16 psql -U mrtovo -d rh_db < database/bloc6-donnees-test.sql
   ```

   Ce script affecte le régime bureau aux deux salariés de test et crée leurs pointages de la semaine dernière.
4. **Interface** : se connecter en `rh@rh.com`, menu **Temps de travail**.
   - **Pointages** : choisir Aina, « Semaine précédente », vérifier les 45 min de retard et 2 h 02 d'heures sup.
   - Valider : le retard est compensé, il reste 1 h 17 à payer.
   - Choisir Luc : la décision est bloquée par la sortie manquante. La saisir, puis tester un refus sans commentaire.
   - **Anomalies** : le bouton « Corriger » amène sur la bonne semaine.
   - **Variables de paie** : choisir le mois de la semaine testée.
   - **Dossiers salariés** → onglet Journal d'audit : toutes les actions y figurent.
5. **Sécurité** : `curl -i -u manager.rh@rh.com:manager123 http://localhost:8080/api/bloc6/regimes` → 403.

---

## 22. Limites connues et pistes

- **Taux et paliers** : 130 % / 150 % sur 8 h sont une hypothèse de travail à confirmer. Ils sont configurables, mais la règle ne distingue pas les heures de nuit, de dimanche ou de jour férié, souvent majorées différemment.
- **Seuil légal hebdomadaire** : une heure sup est comptée dès la sortie après l'horaire, même si la semaine n'atteint pas 40 h à cause d'une absence. C'est au RH d'arbitrer au moment de la validation.
- **Absences** : depuis le Bloc 7, le RH les qualifie et une absence non justifiée est retenue à salaire brut / 30 (voir [`BLOC7-CONGES-ABSENCES.md`](./BLOC7-CONGES-ABSENCES.md)). Une absence ne se compense pas avec des heures sup.
- **Micro-heures sup** : sortir à 17:02 crée 2 min d'heures sup, car la tolérance ne s'applique qu'aux retards. Le RH peut les retirer par une validation partielle.
- **Travail de nuit** : une sortie après minuit est refusée, un seul pointage étant possible par jour et la sortie devant suivre l'entrée. Il faudrait une date de sortie distincte.
- **Pauses** : pas de pointage de pause, et le seuil de 5 h est fixé dans le code. Une journée coupée (plusieurs entrées et sorties) n'est pas gérée.
- **Nouveaux salariés** : l'embauche n'affecte pas de régime automatiquement. L'anomalie `SANS_REGIME` et le bandeau de l'écran le signalent au RH.
- **Liste « à valider »** : elle couvre 12 semaines. Une semaine plus ancienne jamais décidée n'y apparaît plus, mais reste visible dans l'onglet Pointages et marque sa paie comme provisoire.
- **Saisie** : le RH saisit tout. Un import CSV de badgeuse ou des comptes salariés seraient l'évolution naturelle.
- **Plannings** : pas d'horaires par exception (un jour férié, un remplacement) ni de calendrier des jours fériés.
- **Clôture de paie** : rien ne verrouille encore un mois payé. Une correction peut donc modifier des chiffres déjà transmis, ce qui devra être traité avec le bloc paie.
