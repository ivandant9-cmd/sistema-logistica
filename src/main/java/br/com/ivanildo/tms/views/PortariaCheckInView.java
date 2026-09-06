//package br.com.ivanildo.tms.views;

import br.com.ivanildo.tms.util.QRCodeGenerator;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinServletRequest;
import com.vaadin.flow.server.auth.AnonymousAllowed;

@Route("portaria")
@AnonymousAllowed
public class PortariaCheckInView extends VerticalLayout {

    public PortariaCheckInView() {
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);
        setSizeFull();
        getStyle().set("background-color", "#0b1329").set("color", "#ffffff");

        H2 titulo = new H2("📱 Autoatendimento - Portaria");
        titulo.getStyle().set("color", "#ffffff").set("margin-bottom", "0");

        Paragraph instrucao = new Paragraph("Aponte a câmera do seu celular para o QR Code abaixo para realizar o check-in:");
        instrucao.getStyle().set("color", "#cbd5e1").set("font-size", "18px");

        // Descobre dinamicamente a URL base do sistema (ex: http://localhost:10000/checkin)
        String baseUrl = getBaseUrl();
        String linkCheckIn = baseUrl + "/checkin";

        // Gera o componente de Imagem do Vaadin usando o gerador de QR Code
        Image qrCodeImage = new Image(QRCodeGenerator.gerarQRCodeStream(linkCheckIn, 250, 250), "QR Code Check-in");
        qrCodeImage.getStyle()
            .set("border-radius", "12px")
            .set("background-color", "#ffffff")
            .set("padding", "15px")
            .set("box-shadow", "0 10px 25px rgba(0,0,0,0.3)");

        Paragraph linkText = new Paragraph("Link direto: " + linkCheckIn);
        linkText.getStyle().set("color", "#64748b").set("font-size", "14px");

        add(titulo, instrucao, qrCodeImage, linkText);
    }

    private String getBaseUrl() {
    VaadinServletRequest request = VaadinServletRequest.getCurrent();
    if (request != null) {
        String scheme = request.getScheme(); // http ou https
        String serverName = request.getServerName(); // localhost ou o IP/domínio atual
        int serverPort = request.getServerPort(); // porta da aplicação (ex: 10000)
        return scheme + "://" + serverName + ":" + serverPort;
    }
    return "http://localhost:10000"; // Fallback de segurança caso venha nulo
}
}//