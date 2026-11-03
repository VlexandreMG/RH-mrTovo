package com.mrtovo.backend.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CvStorageServiceTest {

    @TempDir
    Path storageDirectory;

    @Test
    void enregistreEtRelitUnCvPdf() throws Exception {
        CvStorageService service = new CvStorageService(storageDirectory.toString());
        byte[] contenu = "%PDF-1.7\ncontenu de test".getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        MockMultipartFile fichier = new MockMultipartFile("cv", "candidat.pdf", "application/pdf", contenu);

        String cle = service.enregistrer(fichier);

        assertEquals("application/pdf", service.charger(cle).mediaType().toString());
        assertEquals(contenu.length, Files.size(storageDirectory.resolve(cle)));
    }

    @Test
    void refuseUnFormatQuiNeCorrespondPasAuContenu() {
        CvStorageService service = new CvStorageService(storageDirectory.toString());
        MockMultipartFile fichier = new MockMultipartFile("cv", "candidat.pdf", "application/pdf",
                "pas un pdf".getBytes(java.nio.charset.StandardCharsets.US_ASCII));

        assertThrows(ResponseStatusException.class, () -> service.enregistrer(fichier));
    }
}
