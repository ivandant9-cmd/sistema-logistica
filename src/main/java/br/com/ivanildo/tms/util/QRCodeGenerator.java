package br.com.ivanildo.tms.util;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.vaadin.flow.server.StreamResource;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class QRCodeGenerator {

    public static StreamResource gerarQRCodeStream(String texto, int largura, int altura) {
        return new StreamResource("qrcode.png", () -> {
            try {
                QRCodeWriter qrCodeWriter = new QRCodeWriter();
                BitMatrix bitMatrix = qrCodeWriter.encode(texto, BarcodeFormat.QR_CODE, largura, altura);
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream);
                return new ByteArrayInputStream(outputStream.toByteArray());
            } catch (WriterException | IOException e) {
                e.printStackTrace();
                return null;
            }
        });
    }
}   