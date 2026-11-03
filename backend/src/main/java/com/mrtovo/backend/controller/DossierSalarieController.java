package com.mrtovo.backend.controller;

import com.mrtovo.backend.dto.DocumentSalarieDto;
import com.mrtovo.backend.dto.DossierResumeDto;
import com.mrtovo.backend.dto.DossierSalarieDto;
import com.mrtovo.backend.dto.IdentiteRequest;
import com.mrtovo.backend.dto.JournalAuditDto;
import com.mrtovo.backend.entity.TypeDocument;
import com.mrtovo.backend.service.DossierSalarieService;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

/**
 * Bloc 5 : API du dossier administratif du salarie.
 * L'acces est reserve au role RH dans {@code SecurityConfig} (regle /api/bloc5/**).
 */
@RestController
@RequestMapping("/api/bloc5/dossiers")
public class DossierSalarieController {

    private final DossierSalarieService service;

    public DossierSalarieController(DossierSalarieService service) {
        this.service = service;
    }

    @GetMapping
    public List<DossierResumeDto> lister() {
        return service.listerDossiers();
    }

    @GetMapping("/{id}")
    public DossierSalarieDto dossier(@PathVariable Long id, Authentication auth) {
        return service.dossier(id, auth.getName());
    }

    @PutMapping("/{id}/identite")
    public DossierSalarieDto modifierIdentite(@PathVariable Long id,
                                              @Valid @RequestBody IdentiteRequest requete,
                                              Authentication auth) {
        return service.modifierIdentite(id, requete, auth.getName());
    }

    @GetMapping("/{id}/journal")
    public List<JournalAuditDto> journal(@PathVariable Long id) {
        return service.journal(id);
    }

    @PostMapping(value = "/{id}/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public DocumentSalarieDto ajouterDocument(
            @PathVariable Long id,
            @RequestParam TypeDocument typeDocument,
            @RequestParam(required = false) String libelle,
            @RequestParam(required = false) String numero,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateEmission,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateExpiration,
            @RequestPart(required = false) MultipartFile fichier,
            Authentication auth) {
        return service.ajouterDocument(id, typeDocument, libelle, numero, dateEmission, dateExpiration,
                fichier, auth.getName());
    }

    @GetMapping("/documents/{documentId}/fichier")
    public ResponseEntity<Resource> telecharger(@PathVariable Long documentId, Authentication auth) {
        DossierSalarieService.FichierTelecharge fichier = service.telechargerDocument(documentId, auth.getName());
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(fichier.nomFichier(), StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .contentType(fichier.contentType() != null
                        ? MediaType.parseMediaType(fichier.contentType())
                        : MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .header("X-Content-Type-Options", "nosniff")
                .body(fichier.ressource());
    }

    @DeleteMapping("/documents/{documentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void supprimerDocument(@PathVariable Long documentId, Authentication auth) {
        service.supprimerDocument(documentId, auth.getName());
    }
}
