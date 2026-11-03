package com.mrtovo.backend.service;

import com.mrtovo.backend.exception.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Locale;
import java.util.UUID;

/**
 * Stockage disque des pieces administratives des salaries (Bloc 5).
 * Le fichier est renomme avec un UUID : le nom d'origine n'est conserve qu'en base,
 * jamais utilise pour construire un chemin.
 */
@Service
public class DocumentStorageService {

    private static final long TAILLE_MAX = 10L * 1024 * 1024;
    private static final String CLE_VALIDE = "[0-9a-fA-F-]{36}\\.(pdf|png|jpg)";

    private static final byte[] SIGNATURE_PDF = {'%', 'P', 'D', 'F', '-'};
    private static final byte[] SIGNATURE_PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A};
    private static final byte[] SIGNATURE_JPG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};

    private final Path dossierStockage;

    public DocumentStorageService(@Value("${bloc5.documents-dir:uploads/documents}") String dossierStockage) {
        this.dossierStockage = Path.of(dossierStockage).toAbsolutePath().normalize();
    }

    public FichierStocke enregistrer(MultipartFile fichier) {
        if (fichier == null || fichier.isEmpty()) {
            throw ApiException.badRequest("FICHIER_VIDE", "Le fichier est vide.");
        }
        if (fichier.getSize() > TAILLE_MAX) {
            throw ApiException.badRequest("FICHIER_TROP_GROS", "Le fichier ne doit pas dépasser 10 Mo.");
        }

        String extension = extensionReelle(fichier);
        String cle = UUID.randomUUID() + "." + extension;
        Path destination = dossierStockage.resolve(cle).normalize();
        if (!destination.startsWith(dossierStockage)) {
            throw ApiException.badRequest("NOM_FICHIER_INVALIDE", "Nom de fichier invalide.");
        }

        try {
            Files.createDirectories(dossierStockage);
            try (InputStream entree = fichier.getInputStream()) {
                Files.copy(entree, destination, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Impossible d'enregistrer le document.", exception);
        }
        return new FichierStocke(cle, typeMime(extension).toString());
    }

    public Resource charger(String cle) {
        Path fichier = cheminVerifie(cle);
        if (!Files.isRegularFile(fichier)) {
            throw ApiException.notFound("Fichier du document introuvable sur le serveur.");
        }
        try {
            return new UrlResource(fichier.toUri());
        } catch (IOException exception) {
            throw new IllegalStateException("Impossible de lire le document.", exception);
        }
    }

    public void supprimer(String cle) {
        try {
            Files.deleteIfExists(cheminVerifie(cle));
        } catch (IOException exception) {
            throw new IllegalStateException("Impossible de supprimer le document.", exception);
        }
    }

    private Path cheminVerifie(String cle) {
        if (cle == null || !cle.matches(CLE_VALIDE)) {
            throw ApiException.notFound("Fichier du document introuvable.");
        }
        Path fichier = dossierStockage.resolve(cle).normalize();
        if (!fichier.startsWith(dossierStockage)) {
            throw ApiException.notFound("Fichier du document introuvable.");
        }
        return fichier;
    }

    /** L'extension annoncee doit correspondre aux premiers octets du fichier (signature). */
    private String extensionReelle(MultipartFile fichier) {
        String nom = fichier.getOriginalFilename();
        String extension = nom == null || nom.lastIndexOf('.') < 0
                ? ""
                : nom.substring(nom.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
        if (extension.equals("jpeg")) {
            extension = "jpg";
        }

        byte[] debut;
        try (InputStream entree = fichier.getInputStream()) {
            debut = entree.readNBytes(8);
        } catch (IOException exception) {
            throw new IllegalStateException("Impossible de vérifier le document.", exception);
        }

        boolean conforme = switch (extension) {
            case "pdf" -> commencePar(debut, SIGNATURE_PDF);
            case "png" -> commencePar(debut, SIGNATURE_PNG);
            case "jpg" -> commencePar(debut, SIGNATURE_JPG);
            default -> false;
        };
        if (!conforme) {
            throw ApiException.badRequest("FORMAT_INVALIDE",
                    "Format de document invalide. Formats acceptés : PDF, PNG, JPG.");
        }
        return extension;
    }

    private static boolean commencePar(byte[] contenu, byte[] signature) {
        return contenu.length >= signature.length
                && Arrays.equals(Arrays.copyOf(contenu, signature.length), signature);
    }

    private static MediaType typeMime(String extension) {
        return switch (extension) {
            case "pdf" -> MediaType.APPLICATION_PDF;
            case "png" -> MediaType.IMAGE_PNG;
            default -> MediaType.IMAGE_JPEG;
        };
    }

    public record FichierStocke(String cle, String contentType) {
    }
}
