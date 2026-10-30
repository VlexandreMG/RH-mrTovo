package com.mrtovo.backend.service;

import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;

import org.springframework.stereotype.Service;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import com.mrtovo.backend.entity.contrat.Contrat;

@Service
public class ContratPdfService {

    private static final Font TITLE_FONT = new Font(Font.FontFamily.HELVETICA, 22, Font.BOLD, new BaseColor(33, 37, 41));
    private static final Font SUBTITLE_FONT = new Font(Font.FontFamily.HELVETICA, 12, Font.NORMAL, new BaseColor(108, 117, 125));
    private static final Font HEADER_FONT = new Font(Font.FontFamily.HELVETICA, 11, Font.BOLD, BaseColor.WHITE);
    private static final Font CELL_FONT = new Font(Font.FontFamily.HELVETICA, 11, Font.NORMAL, new BaseColor(33, 37, 41));
    private static final Font SECTION_FONT = new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD, new BaseColor(13, 110, 253));
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy");

    public byte[] generateContratPdf(Contrat contrat) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 50, 50, 50, 50);
        PdfWriter.getInstance(document, baos);
        document.open();

        // Titre principal
        Paragraph title = new Paragraph("CONTRAT DE TRAVAIL", TITLE_FONT);
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(5);
        document.add(title);

        // Sous-titre
        String typeLabel = contrat.getTypeContrat() != null ? contrat.getTypeContrat().getLibelle() : "N/A";
        Paragraph subtitle = new Paragraph("Type : " + typeLabel, SUBTITLE_FONT);
        subtitle.setAlignment(Element.ALIGN_CENTER);
        subtitle.setSpacingAfter(30);
        document.add(subtitle);

        // Ligne de séparation
        PdfPTable separator = new PdfPTable(1);
        separator.setWidthPercentage(100);
        PdfPCell sepCell = new PdfPCell();
        sepCell.setBorderWidthBottom(2);
        sepCell.setBorderColorBottom(new BaseColor(13, 110, 253));
        sepCell.setBorderWidthTop(0);
        sepCell.setBorderWidthLeft(0);
        sepCell.setBorderWidthRight(0);
        sepCell.setFixedHeight(3);
        separator.addCell(sepCell);
        document.add(separator);
        document.add(new Paragraph(" "));

        // Section Informations de l'employé
        Paragraph empSection = new Paragraph("Informations de l'employé", SECTION_FONT);
        empSection.setSpacingAfter(10);
        document.add(empSection);

        if (contrat.getEmploye() != null) {
            PdfPTable empTable = new PdfPTable(2);
            empTable.setWidthPercentage(100);
            empTable.setWidths(new float[]{1, 2});

            addTableRow(empTable, "Nom", contrat.getEmploye().getNom());
            addTableRow(empTable, "Prénom", contrat.getEmploye().getPrenom());
            addTableRow(empTable, "Email", contrat.getEmploye().getEmail());
            addTableRow(empTable, "Téléphone", contrat.getEmploye().getTelephone());
            addTableRow(empTable, "Adresse", contrat.getEmploye().getAdresse());

            document.add(empTable);
        }

        document.add(new Paragraph(" "));

        // Section Détails du contrat
        Paragraph contratSection = new Paragraph("Détails du contrat", SECTION_FONT);
        contratSection.setSpacingAfter(10);
        document.add(contratSection);

        PdfPTable contratTable = new PdfPTable(2);
        contratTable.setWidthPercentage(100);
        contratTable.setWidths(new float[]{1, 2});

        addTableRow(contratTable, "N° Contrat", String.valueOf(contrat.getId()));
        addTableRow(contratTable, "Type de contrat", typeLabel);
        addTableRow(contratTable, "Date de début",
                contrat.getDateDebut() != null ? DATE_FORMAT.format(contrat.getDateDebut()) : "N/A");
        addTableRow(contratTable, "Date de fin",
                contrat.getDateFin() != null ? DATE_FORMAT.format(contrat.getDateFin()) : "N/A");
        addTableRow(contratTable, "Salaire",
                String.format("%,.2f Ar", contrat.getSalaire()));
        addTableRow(contratTable, "Statut",
                contrat.getStatutContrat() != null ? contrat.getStatutContrat().getLibelle() : "N/A");

        document.add(contratTable);

        document.add(new Paragraph(" "));
        document.add(new Paragraph(" "));

        // Signatures
        PdfPTable signTable = new PdfPTable(2);
        signTable.setWidthPercentage(100);

        PdfPCell empSignCell = new PdfPCell(new Phrase("Signature de l'employé", CELL_FONT));
        empSignCell.setBorder(0);
        empSignCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        empSignCell.setPaddingTop(30);
        signTable.addCell(empSignCell);

        PdfPCell dirSignCell = new PdfPCell(new Phrase("Signature de l'employeur", CELL_FONT));
        dirSignCell.setBorder(0);
        dirSignCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        dirSignCell.setPaddingTop(30);
        signTable.addCell(dirSignCell);

        PdfPCell lineCell1 = new PdfPCell(new Phrase("________________________", CELL_FONT));
        lineCell1.setBorder(0);
        lineCell1.setHorizontalAlignment(Element.ALIGN_CENTER);
        lineCell1.setPaddingTop(40);
        signTable.addCell(lineCell1);

        PdfPCell lineCell2 = new PdfPCell(new Phrase("________________________", CELL_FONT));
        lineCell2.setBorder(0);
        lineCell2.setHorizontalAlignment(Element.ALIGN_CENTER);
        lineCell2.setPaddingTop(40);
        signTable.addCell(lineCell2);

        document.add(signTable);

        document.close();
        return baos.toByteArray();
    }

    private void addTableRow(PdfPTable table, String label, String value) {
        PdfPCell headerCell = new PdfPCell(new Phrase(label, HEADER_FONT));
        headerCell.setBackgroundColor(new BaseColor(13, 110, 253));
        headerCell.setPadding(8);
        headerCell.setBorderColor(BaseColor.WHITE);
        table.addCell(headerCell);

        PdfPCell valueCell = new PdfPCell(new Phrase(value != null ? value : "N/A", CELL_FONT));
        valueCell.setPadding(8);
        valueCell.setBackgroundColor(new BaseColor(248, 249, 250));
        valueCell.setBorderColor(new BaseColor(222, 226, 230));
        table.addCell(valueCell);
    }
}
