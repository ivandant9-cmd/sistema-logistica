package br.com.ivanildo.tms.views;

import br.com.ivanildo.tms.model.Carregamento;
import br.com.ivanildo.tms.model.Motorista;
import br.com.ivanildo.tms.repository.CarregamentoRepository;
import br.com.ivanildo.tms.repository.MotoristaRepository;
import br.com.ivanildo.tms.util.UiBroadcaster;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinServletRequest;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import java.util.Optional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Route("portaria")
@AnonymousAllowed
public class CheckInView extends VerticalLayout {

    private final CarregamentoRepository carregamentoRepository;
    private final MotoristaRepository motoristaRepository;

    private final TextField txtCpf = new TextField("CPF do Motorista");
    private final TextField txtNome = new TextField("Nome Completo");
    private final TextField txtPlaca = new TextField("Placa do Veículo");
    private final Button btnSalvarCadastro = new Button("Cadastrar e Fazer Check-in");
    private final Button btnConfirmarCheckin = new Button("Confirmar Check-in");
    private final Span lblStatus = new Span();
    
    private String fotoCapturadaBase64 = null;
    private Motorista motoristaIdentificado = null;
    private List<Long> idsViagensAtivas = new ArrayList<>();

    private UiBroadcaster.Registration broadcastRegistration;

public CheckInView(CarregamentoRepository carregamentoRepository, MotoristaRepository motoristaRepository) {
    this.carregamentoRepository = carregamentoRepository;
    this.motoristaRepository = motoristaRepository;

    setAlignItems(Alignment.CENTER);
    setJustifyContentMode(JustifyContentMode.CENTER);
    setSizeFull();
    getStyle().set("background-color", "#0b1329").set("color", "#ffffff");

    H2 titulo = new H2("📱 Self Check-in Portaria - Reconhecimento Facial");
    titulo.getStyle().set("color", "#ffffff").set("margin-bottom", "5px");

    lblStatus.setText("Realize o reconhecimento facial para iniciar");
    lblStatus.getStyle().set("color", "#cbd5e1").set("margin-bottom", "15px");

    estilizarCampo(txtCpf);
    estilizarCampo(txtNome);
    estilizarCampo(txtPlaca);

    txtCpf.setVisible(false);
    txtNome.setVisible(false);
    txtPlaca.setVisible(false);
    btnSalvarCadastro.setVisible(false);
    btnConfirmarCheckin.setVisible(false);

    Div painelInterativo = new Div();
    painelInterativo.getStyle().set("display", "flex").set("flex-direction", "column").set("align-items", "center").set("gap", "15px");

    Div cameraDiv = criarBotaoCameraReconhecimento();
    String urlAtual = getBaseUrl() + "/portaria";
    Div qrCodeBox = criarBoxQrCode(urlAtual);

    painelInterativo.add(cameraDiv, qrCodeBox);

    btnSalvarCadastro.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    btnSalvarCadastro.getStyle().set("margin-top", "15px");
    btnSalvarCadastro.addClickListener(e -> salvarNovoMotoristaECheckIn());

    btnConfirmarCheckin.addThemeVariants(ButtonVariant.LUMO_SUCCESS);
    btnConfirmarCheckin.getStyle().set("margin-top", "15px");
    btnConfirmarCheckin.addClickListener(e -> realizarCheckInRecorrente());

    add(titulo, lblStatus, painelInterativo, txtCpf, txtNome, txtPlaca, btnSalvarCadastro, btnConfirmarCheckin);

    // Salva a referência do registro para poder limpar depois
    broadcastRegistration = UiBroadcaster.register(message -> {
        getUI().ifPresent(ui -> ui.access(() -> {
            if (idsViagensAtivas != null && !idsViagensAtivas.isEmpty()) {
                atualizarPainelAcompanhamentoFila();
            }
        }));
    });
}

    private String getBaseUrl() {
        VaadinServletRequest request = VaadinServletRequest.getCurrent();
        if (request != null) {
            StringBuffer url = request.getRequestURL();
            String uri = request.getRequestURI();
            return url.substring(0, url.length() - uri.length());
        }
        return "http://localhost:10000";
    }

    private Div criarBoxQrCode(String urlDestino) {
        Div box = new Div();
        box.getStyle()
            .set("background", "#ffffff")
            .set("padding", "15px")
            .set("border-radius", "12px")
            .set("text-align", "center")
            .set("box-shadow", "0 4px 6px rgba(0,0,0,0.3)");

        String qrCodeApiUrl = "https://api.qrserver.com/v1/create-qr-code/?size=160x160&data=" + urlDestino;
        Image qrImage = new Image(qrCodeApiUrl, "QR Code para Check-in");
        qrImage.setWidth("160px");
        qrImage.setHeight("160px");

        Span instrucao = new Span("Ou escaneie com o celular");
        instrucao.getStyle().set("display", "block").set("color", "#1e293b").set("font-size", "12px").set("margin-top", "8px").set("font-weight", "bold");

        box.add(qrImage, instrucao);
        return box;
    }

    private Div criarBotaoCameraReconhecimento() {
        Div container = new Div();
        container.setWidth("300px");
        
        container.getElement().setProperty("innerHTML", 
            "<label style='display: block; width: 100%; background-color: #2563eb; color: white; text-align: center; padding: 14px; border-radius: 8px; font-weight: bold; cursor: pointer; box-shadow: 0 4px 6px rgba(0,0,0,0.2);'>" +
            "📸 Tirar Foto / Reconhecimento" +
            "<input type='file' accept='image/*' capture='user' style='display: none;' id='nativeCameraInput'>" +
            "</label>" +
            "<div id='previewTexto' style='color: #cbd5e1; text-align: center; margin-top: 8px; font-size: 14px;'>Aguardando captura...</div>"
        );

        container.getElement().executeJs(
            "const input = this.querySelector('#nativeCameraInput');" +
            "input.addEventListener('change', (e) => {" +
            "  const file = e.target.files[0];" +
            "  if (file) {" +
            "    const reader = new FileReader();" +
            "    reader.onload = (uploadEvent) => {" +
            "      const base64Image = uploadEvent.target.result;" +
            "      this.querySelector('#previewTexto').innerText = '✅ Foto capturada com sucesso!';" +
            "      $0.$server.processarFotoCapturada(base64Image);" +
            "    };" +
            "    reader.readAsDataURL(file);" +
            "  }" +
            "});", getElement()
        );

        return container;
    }

    @com.vaadin.flow.component.ClientCallable
    public void processarFotoCapturada(String fotoBase64) {
        if (fotoBase64 == null || fotoBase64.trim().isEmpty()) {
            Notification.show("Erro ao capturar imagem da câmera.", 3000, Notification.Position.MIDDLE);
            return;
        }

        this.fotoCapturadaBase64 = fotoBase64;
        String cpfInformado = txtCpf.getValue();
        Optional<Motorista> optMotorista = motoristaRepository.findByCpf(cpfInformado);

        if (optMotorista.isEmpty()) {
            configurarTelaPrimeiroAcesso();
        } else {
            motoristaIdentificado = optMotorista.get();
            configurarTelaAcessoRecorrente();
        }
    }

    private void configurarTelaPrimeiroAcesso() {
        txtCpf.setVisible(true);
        txtNome.setVisible(true);
        txtPlaca.setVisible(true);
        btnSalvarCadastro.setVisible(true);
        txtCpf.setReadOnly(false);
        txtNome.setReadOnly(false);
        lblStatus.setText("Novo motorista detectado! Preencha CPF, Nome e a Placa.");
    }

    private void configurarTelaAcessoRecorrente() {
        txtCpf.setValue(motoristaIdentificado.getCpf());
        txtNome.setValue(motoristaIdentificado.getNome());
        txtCpf.setVisible(true);
        txtNome.setVisible(true);
        txtPlaca.setVisible(true);
        btnConfirmarCheckin.setVisible(true);
        txtCpf.setReadOnly(true);
        txtNome.setReadOnly(true);
        lblStatus.setText("Rosto reconhecido! Informe apenas a placa do veículo.");
    }

    private void salvarNovoMotoristaECheckIn() {
        String cpf = txtCpf.getValue() != null ? txtCpf.getValue().replaceAll("\\D", "") : "";
        String nome = txtNome.getValue() != null ? txtNome.getValue().trim().toUpperCase() : "";
        String placa = txtPlaca.getValue() != null ? txtPlaca.getValue().trim().toUpperCase() : "";

        if (cpf.length() < 11 || nome.isEmpty() || placa.isEmpty()) {
            Notification.show("Preencha todos os campos!", 3000, Notification.Position.MIDDLE);
            return;
        }

        Motorista novo = motoristaRepository.findByCpf(cpf).orElse(new Motorista());
        novo.setCpf(cpf);
        novo.setNome(nome);
        novo.setPlaca(placa);
        novo.setFotoBase64(fotoCapturadaBase64);
        motoristaRepository.save(novo);

        processarCheckInFinal(placa, novo);
    }

    private void realizarCheckInRecorrente() {
        String placa = txtPlaca.getValue() != null ? txtPlaca.getValue().trim().toUpperCase() : "";
        if (placa.isEmpty()) {
            Notification.show("Informe a placa do veículo!", 3000, Notification.Position.MIDDLE);
            return;
        }

        if (motoristaIdentificado != null) {
            motoristaIdentificado.setPlaca(placa);
            motoristaIdentificado.setFotoBase64(fotoCapturadaBase64);
            motoristaRepository.save(motoristaIdentificado);

            processarCheckInFinal(placa, motoristaIdentificado);
        }
    }

    private void processarCheckInFinal(String placaDigitada, Motorista motorista) {
        String placaFormatada = placaDigitada.replaceAll("[^a-zA-Z0-9]", "");
        List<Carregamento> viagens = carregamentoRepository.findByPlacaIgnoreCase(placaFormatada);

        if (viagens.isEmpty()) {
            Notification.show("Nenhum agendamento encontrado para a placa: " + placaDigitada, 4000, Notification.Position.MIDDLE);
            return;
        }

    idsViagensAtivas.clear();
LocalDateTime agora = LocalDateTime.now();
for (Carregamento c : viagens) {
    c.setMotorista(motorista.getNome());
    c.setMotoristaEntidade(motorista);
    c.setStatus("Apresentado");
    c.setDataHoraApresentacao(agora);
    c.setDataChegada(agora); // Passando LocalDateTime diretamente
    c.setHoraChegada(agora);  // Passando LocalDateTime diretamente
    
    carregamentoRepository.save(c);
    idsViagensAtivas.add(c.getId());
}

        UiBroadcaster.broadcast("STATUS_ATUALIZADO");
        atualizarPainelAcompanhamentoFila();
        iniciarAtualizacaoAutomatica(); // <-- Chame aqui para disparar o timer de 2 em 2 segundos
    }

// Adicione este método na sua classe CheckInView para iniciar a atualização periódica sem alterar a estrutura
    private void iniciarAtualizacaoAutomatica() {
        java.util.concurrent.Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(() -> {
            if (idsViagensAtivas != null && !idsViagensAtivas.isEmpty()) {
                UiBroadcaster.broadcast("STATUS_ATUALIZADO");
            }
        }, 2, 2, java.util.concurrent.TimeUnit.SECONDS);
    }
    
    private void atualizarPainelAcompanhamentoFila() {
        removeAll();

        H2 tituloPainel = new H2("🚚 Acompanhamento da Fila");
        tituloPainel.getStyle().set("color", "#22c55e").set("margin-bottom", "10px");

        Div containerStatus = new Div();
        containerStatus.getStyle()
            .set("background", "#1e293b")
            .set("padding", "20px")
            .set("border-radius", "12px")
            .set("width", "340px")
            .set("box-shadow", "0 4px 6px rgba(0,0,0,0.3)")
            .set("text-align", "center");

        boolean temDocaAtribuida = false;
        String docaEncontrada = "";
        StringBuilder sbViagens = new StringBuilder();

        List<Carregamento> viagensAtuais = new ArrayList<>();
        for (Long id : idsViagensAtivas) {
            carregamentoRepository.findById(id).ifPresent(viagensAtuais::add);
        }

        for (Carregamento c : viagensAtuais) {
            sbViagens.append("Viagem: ").append(c.getViagem()).append("<br>");
            if (c.getDoca() != null && !c.getDoca().trim().isEmpty()) {
                temDocaAtribuida = true;
                docaEncontrada = c.getDoca();
            }
        }

        if (temDocaAtribuida) {
            containerStatus.getStyle().set("border", "2px solid #22c55e");
            containerStatus.getElement().setProperty("innerHTML",
                "<h3 style='color: #22c55e; margin-top:0;'>🎉 É A SUA VEZ!</h3>" +
                "<p style='font-size: 18px; font-weight: bold; color: #ffffff;'>Dirija-se à Doca:</p>" +
                "<div style='font-size: 36px; font-weight: bold; background: #22c55e; color: #ffffff; padding: 10px; border-radius: 8px; margin: 10px 0;'>" + docaEncontrada + "</div>" +
                "<p style='color: #cbd5e1; font-size: 13px;'>" + sbViagens.toString() + "</p>"
            );

            getElement().executeJs("if (navigator.vibrate) { navigator.vibrate([500, 250, 500, 250, 500]); }");
        } else {
            containerStatus.getStyle().set("border", "2px solid #3b82f6");
            containerStatus.getElement().setProperty("innerHTML",
                "<h3 style='color: #3b82f6; margin-top:0;'>⏳ Check-in Realizado</h3>" +
                "<p style='color: #ffffff; font-size: 14px;'>Você está na fila de espera aguardando liberação de doca.</p>" +
                "<hr style='border-color: #334155; margin: 10px 0;'>" +
                "<p style='color: #cbd5e1; font-size: 13px;'>" + sbViagens.toString() + "</p>" +
                "<p style='color: #94a3b8; font-size: 12px; margin-top: 10px;'>Esta tela atualizará automaticamente quando sua doca for definida.</p>"
            );
        }

        add(tituloPainel, containerStatus);
    }

    private void estilizarCampo(TextField campo) {
        campo.setWidth("300px");
        campo.getElement().getStyle()
            .set("--lumo-secondary-text-color", "#cbd5e1")
            .set("--lumo-body-text-color", "#f8fafc")
            .set("color", "#f8fafc");
    }
}