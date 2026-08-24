package br.com.ivanildo.tms.views;

import br.com.ivanildo.tms.model.Carregamento;
import br.com.ivanildo.tms.model.Motorista;
import br.com.ivanildo.tms.repository.CarregamentoRepository;
import br.com.ivanildo.tms.repository.MotoristaRepository;
//import br.com.ivanildo.tms.util.UiBroadcaster; // Se você utiliza para atualizar a grid em tempo real
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Route("checkin")
@AnonymousAllowed // Permite acesso público sem exigir login
public class CheckInView extends VerticalLayout {

    private final CarregamentoRepository carregamentoRepository;
    private final MotoristaRepository motoristaRepository;

    private TextField txtCpf = new TextField("CPF do Motorista");
    private TextField txtNome = new TextField("Nome Completo");
    private TextField txtPlaca = new TextField("Placa do Veículo");
    private Button btnBuscar = new Button("Validar CPF");
    private Button btnConfirmar = new Button("Confirmar Chegada e Apresentar");
    private Span lblStatus = new Span();

    public CheckInView(CarregamentoRepository carregamentoRepository, MotoristaRepository motoristaRepository) {
        this.carregamentoRepository = carregamentoRepository;
        this.motoristaRepository = motoristaRepository;

        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);
        setSizeFull();
        getStyle().set("background-color", "#0b1329").set("color", "#ffffff");

        H2 titulo = new H2("📱 Self Check-in Portaria");
        titulo.getStyle().set("color", "#ffffff");

        lblStatus.setText("Informe seu CPF para iniciar o check-in");
        lblStatus.getStyle().set("color", "#cbd5e1").set("margin-bottom", "15px");

        txtCpf.setPlaceholder("Digite apenas números");
        txtCpf.setMaxLength(11);

        estilizarCampo(txtCpf);
        estilizarCampo(txtNome);
        estilizarCampo(txtPlaca);

        txtNome.setVisible(false);
        txtPlaca.setVisible(false);
        btnConfirmar.setVisible(false);

        btnBuscar.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        btnBuscar.getStyle().set("margin-top", "15px");
        btnBuscar.addClickListener(e -> processarCpf());

        btnConfirmar.addThemeVariants(ButtonVariant.LUMO_SUCCESS);
        btnConfirmar.getStyle().set("margin-top", "15px");
        btnConfirmar.addClickListener(e -> realizarCheckIn());

        add(titulo, lblStatus, txtCpf, btnBuscar, txtNome, txtPlaca, btnConfirmar);
    }

    private void processarCpf() {
        String cpf = txtCpf.getValue().replaceAll("\\D", "");
        if (cpf.length() < 11) {
            Notification.show("Informe um CPF válido com 11 dígitos!", 3000, Notification.Position.MIDDLE);
            return;
        }

        Optional<Motorista> opt = motoristaRepository.findByCpf(cpf);
        if (opt.isPresent()) {
            Motorista m = opt.get();
            txtNome.setValue(m.getNome() != null ? m.getNome() : "");
            txtPlaca.setValue(m.getPlaca() != null ? m.getPlaca() : "");
            Notification.show("Motorista localizado no banco de dados!", 3000, Notification.Position.MIDDLE);
        } else {
            txtNome.setValue("");
            txtPlaca.setValue("");
            Notification.show("Primeiro acesso! Preencha seu nome e a placa do veículo.", 3000, Notification.Position.MIDDLE);
        }

        txtNome.setVisible(true);
        txtPlaca.setVisible(true);
        btnConfirmar.setVisible(true);
        btnBuscar.setVisible(false);
    }

    private void realizarCheckIn() {
        String placaDigitada = txtPlaca.getValue();
        if (placaDigitada == null || placaDigitada.trim().isEmpty()) {
            Notification.show("Informe a placa do veículo!", 3000, Notification.Position.MIDDLE);
            return;
        }

        // Limpa a placa (remove espaços e hífens)
        String placaFormatada = placaDigitada.replaceAll("[^a-zA-Z0-9]", "").toUpperCase();

        // 1. Busca todos os carregamentos atrelados a esta placa
        List<Carregamento> carregamentos = carregamentoRepository.findByPlacaIgnoreCase(placaFormatada);

        if (carregamentos.isEmpty()) {
            Notification.show("Nenhum agendamento encontrado para a placa: " + placaDigitada, 4000, Notification.Position.MIDDLE);
            return;
        }

        // 2. Cadastra ou atualiza o Motorista
        String cpf = txtCpf.getValue().replaceAll("\\D", "");
        Motorista motorista = motoristaRepository.findByCpf(cpf).orElse(new Motorista());
        motorista.setCpf(cpf);
        motorista.setNome(txtNome.getValue());
        motorista.setPlaca(placaFormatada);
        motoristaRepository.save(motorista);

        // 3. Atualiza os carregamentos encontrados para "Apresentado"
        LocalDateTime agora = LocalDateTime.now();
        for (Carregamento c : carregamentos) {
            c.setMotorista(motorista.getNome());
            c.setMotoristaEntidade(motorista);
            c.setStatus("Apresentado");
            c.setDataHoraApresentacao(agora);
            carregamentoRepository.save(c);
        }

        // Notifica as views ativas para atualizarem o Grid se usar UiBroadcaster
        // UiBroadcaster.broadcast("CHECKIN_REALIZADO");

        // 4. Exibe a tela de confirmação/sucesso
        removeAll();
        H2 msgSucesso = new H2("✅ Check-in realizado!");
        msgSucesso.getStyle().set("color", "#22c55e");
        
        Span detalhe = new Span("Apresentação confirmada para " + carregamentos.size() + " viagem(ns). Aguarde a chamada para a doca.");
        detalhe.getStyle().set("color", "#f8fafc");
        
        add(msgSucesso, detalhe);
    }

    private void estilizarCampo(TextField campo) {
        campo.setWidth("300px");
        campo.getElement().getStyle()
            .set("--lumo-secondary-text-color", "#cbd5e1")
            .set("--lumo-body-text-color", "#f8fafc")
            .set("color", "#f8fafc");
    }
}