package br.com.ivanildo.tms.service;

import br.com.ivanildo.tms.model.Carregamento;
import br.com.ivanildo.tms.model.Entrega;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.Locale;

@Service
public class PdfService {

    public byte[] gerarPdfCarregamento(Carregamento carregamento, List<Entrega> entregas) {
        if (carregamento == null) {
            return new byte[0];
        }
        Document document = new Document(PageSize.A4);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        // Formatador seguro usando API moderna (Java 19+)
     DecimalFormat df = new DecimalFormat("#,##0.00", new DecimalFormatSymbols(new Locale.Builder().setLanguage("pt").setRegion("BR").build()));

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            // Título
            Font fontTitulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
            Paragraph titulo = new Paragraph("Relatório de Carregamento #" + carregamento.getId(), fontTitulo);
            titulo.setAlignment(Element.ALIGN_CENTER);
            document.add(titulo);
            document.add(Chunk.NEWLINE);

            // Dados do Carregamento
            Font fontNormal = FontFactory.getFont(FontFactory.HELVETICA, 12);
            document.add(new Paragraph("Motorista: " + (carregamento.getMotorista() != null ? carregamento.getMotorista() : "-"), fontNormal));
            document.add(new Paragraph("Placa: " + (carregamento.getPlaca() != null ? carregamento.getPlaca() : "-"), fontNormal));
            document.add(new Paragraph("Transportadora: " + (carregamento.getTransportadora() != null ? carregamento.getTransportadora() : "-"), fontNormal));
            document.add(Chunk.NEWLINE);

            // Tabela com 4 colunas
            PdfPTable table = new PdfPTable(4);
            table.setWidthPercentage(100);

            // Cabeçalho
            table.addCell(new PdfPCell(new Phrase("Nota Fiscal", FontFactory.getFont(FontFactory.HELVETICA_BOLD))));
            table.addCell(new PdfPCell(new Phrase("Cliente", FontFactory.getFont(FontFactory.HELVETICA_BOLD))));
            table.addCell(new PdfPCell(new Phrase("Cidade", FontFactory.getFont(FontFactory.HELVETICA_BOLD))));
            table.addCell(new PdfPCell(new Phrase("Peso (kg)", FontFactory.getFont(FontFactory.HELVETICA_BOLD))));

            // Preenchimento de linhas
            if (entregas != null && !entregas.isEmpty()) {
                for (Entrega p : entregas) {
                    // Validações de campos nulos
                    table.addCell(p.getNf() != null ? p.getNf() : "-");
                    table.addCell(p.getCliente() != null ? p.getCliente() : "-");
                    table.addCell(p.getCidade() != null ? p.getCidade() : "-");

                    // Tratamento seguro do valor numérico
                    String pesoFormatado = "0,00";
                    if (p.getPeso() != null) {
                        try {
                            double pesoDouble = Double.parseDouble(p.getPeso().toString().replace(",", "."));
                            pesoFormatado = df.format(pesoDouble);
                        } catch (NumberFormatException e) {
                            pesoFormatado = p.getPeso().toString();
                        }
                    }
                    table.addCell(pesoFormatado);
                }
            }

            document.add(table);
            document.close();

        } catch (Exception e) {
            System.err.println("❌ Falha na renderização interna do iText PDF:");
            e.printStackTrace();
            return new byte[0];
        }

        return out.toByteArray();
    }
}