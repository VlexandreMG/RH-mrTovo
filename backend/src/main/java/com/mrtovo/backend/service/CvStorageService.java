package com.mrtovo.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class CvStorageService {

    private static final long MAX_CV_BYTES = 10L * 1024 * 1024;
    private static final long MAX_DOCX_EXPANDED_BYTES = 20L * 1024 * 1024;
    private static final int MAX_DOCX_ENTRIES = 1000;

    private final Path storageDirectory;

    public CvStorageService(@Value("${bloc2.cv-storage-dir:uploads/cv}") String storageDirectory) {
        this.storageDirectory = Path.of(storageDirectory).toAbsolutePath().normalize();
    }

    public String enregistrer(MultipartFile fichier) {
        if (fichier == null || fichier.isEmpty()) {
            throw new ResponseStatusException(BAD_REQUEST, "Le fichier CV est obligatoire.");
        }
        if (fichier.getSize() > MAX_CV_BYTES) {
            throw new ResponseStatusException(BAD_REQUEST, "Le CV ne doit pas dépasser 10 Mo.");
        }

        String nomOriginal = fichier.getOriginalFilename();
        String extension = nomOriginal == null || nomOriginal.lastIndexOf('.') < 0
                ? ""
                : nomOriginal.substring(nomOriginal.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
        boolean pdf = extension.equals("pdf") && estPdf(fichier);
        boolean docx = extension.equals("docx") && estDocx(fichier);
        if (!pdf && !docx) {
            throw new ResponseStatusException(BAD_REQUEST, "Format CV invalide. Utilisez un fichier PDF ou DOCX.");
        }

        String cle = UUID.randomUUID() + "." + extension;
        Path destination = storageDirectory.resolve(cle).normalize();
        if (!destination.startsWith(storageDirectory)) {
            throw new ResponseStatusException(BAD_REQUEST, "Nom de fichier invalide.");
        }

        try {
            Files.createDirectories(storageDirectory);
            try (InputStream input = fichier.getInputStream()) {
                Files.copy(input, destination, StandardCopyOption.REPLACE_EXISTING);
            }
            return cle;
        } catch (IOException exception) {
            throw new IllegalStateException("Impossible d'enregistrer le fichier CV.", exception);
        }
    }

    public StoredCv charger(String cle) {
        if (cle == null || !cle.matches("[0-9a-fA-F-]{36}\\.(pdf|docx)")) {
            throw new ResponseStatusException(NOT_FOUND, "CV introuvable.");
        }
        Path fichier = storageDirectory.resolve(cle).normalize();
        if (!fichier.startsWith(storageDirectory) || !Files.isRegularFile(fichier)) {
            throw new ResponseStatusException(NOT_FOUND, "CV introuvable.");
        }
        try {
            MediaType type = cle.endsWith(".pdf")
                    ? MediaType.APPLICATION_PDF
                    : MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
            return new StoredCv(new UrlResource(fichier.toUri()), type);
        } catch (IOException exception) {
            throw new IllegalStateException("Impossible de lire le fichier CV.", exception);
        }
    }

    private boolean estPdf(MultipartFile fichier) {
        try (InputStream input = fichier.getInputStream()) {
            byte[] signature = input.readNBytes(5);
            return signature.length == 5
                    && new String(signature, java.nio.charset.StandardCharsets.US_ASCII).equals("%PDF-");
        } catch (IOException exception) {
            throw new IllegalStateException("Impossible de vérifier le fichier CV.", exception);
        }
    }

    private boolean estDocx(MultipartFile fichier) {
        boolean documentWord = false;
        long tailleDecompressee = 0;
        int entrees = 0;
        try (ZipInputStream zip = new ZipInputStream(fichier.getInputStream())) {
            ZipEntry entree;
            while ((entree = zip.getNextEntry()) != null) {
                if (++entrees > MAX_DOCX_ENTRIES) {
                    return false;
                }
                documentWord |= entree.getName().equals("word/document.xml");
                byte[] tampon = new byte[8192];
                int lus;
                while ((lus = zip.read(tampon)) != -1) {
                    tailleDecompressee += lus;
                    if (tailleDecompressee > MAX_DOCX_EXPANDED_BYTES) {
                        return false;
                    }
                }
            }
            return documentWord;
        } catch (IOException exception) {
            return false;
        }
    }

    public record StoredCv(Resource resource, MediaType mediaType) {}
}
