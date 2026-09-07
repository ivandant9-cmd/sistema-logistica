package br.com.ivanildo.tms.views;

import br.com.ivanildo.tms.model.Carregamento;
import br.com.ivanildo.tms.repository.CarregamentoRepository;
import br.com.ivanildo.tms.repository.EntregaRepository;
import br.com.ivanildo.tms.service.ExcelService;
import br.com.ivanildo.tms.util.UiBroadcaster;
import jakarta.annotation.security.PermitAll;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.FileBuffer;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.component.Component;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.transaction.annotation.Transactional;

import br.com.ivanildo.tms.model.Conferente;
import br.com.ivanildo.tms.repository.ConferenteRepository;

@Route("")
@PageTitle("Gestão Operacional de Carregamento | TMS")
@PermitAll
public class MainView extends VerticalLayout implements BeforeEnterObserver {

    private final CarregamentoRepository repository;
    private final EntregaRepository entregaRepository;
    private final ExcelService excelService;
    private final ConferenteRepository conferenteRepository;
    
    private UiBroadcaster.Registration broadcasterRegistration;

    private final Grid<Carregamento> grid = new Grid<>(Carregamento.class, false);
    private final Map<Carregamento, Checkbox> mapaCheckboxesMain = new HashMap<>();

    private final Span txtTotal = new Span("0");
    private final Span txtApresentados = new Span("0");
    private final Span txtCarregando = new Span("0");
    private final Span txtExpedidos = new Span("0");
    private final Span txtPeso = new Span("0 kg");
    private final Span txtPendentes = new Span("0");

    private String statusFiltroAtual = "TODOS";

    // Componentes de ação que precisam ser acessados para controle de permissão
    private Button btnNovo;
    private Button btnArquivarExpedidas;
    private Button btnExcluirSelecionadas;
    private Button btnLimparCheckin;
    private Button btnVerArquivados;
    private Button btnVerFila;
    private Button btnRelatorioPaletes;
    private Upload uploadExcelComponent;

    public MainView(CarregamentoRepository repository, EntregaRepository entregaRepository, ExcelService excelService, ConferenteRepository conferenteRepository) {
        this.repository = repository;
        this.entregaRepository = entregaRepository;
        this.excelService = excelService;
        this.conferenteRepository = conferenteRepository;

        setSizeFull();
        setPadding(true);
        setSpacing(true);

        getStyle()
            .set("background-color", "#0b1329")
            .set("color", "#f8fafc")
            .set("--lumo-secondary-text-color", "#cbd5e1")
            .set("--lumo-body-text-color", "#f8fafc")
            .set("--lumo-primary-text-color", "#f8fafc")
            .set("--lumo-contrast-60pct", "#cbd5e1")
            .set("--lumo-contrast-70pct", "#cbd5e1");

        getElement().executeJs(
            "const styleId = 'placa-animada-style';" +
            "if (!document.getElementById(styleId)) {" +
            "  const style = document.createElement('style');" +
            "  style.id = styleId;" +
            "  style.innerHTML = `" +
            "    @keyframes piscarPlacaFundo { " +
            "      0% { background-color: #f59e0b; color: #000000; } " +
            "      50% { background-color: #fcd34d; color: #000000; } " +
            "      100% { background-color: #f59e0b; color: #000000; } " +
            "    } " +
            "    .placa-alterada { " +
            "      animation: piscarPlacaFundo 1.5s infinite ease-in-out; " +
            "      font-weight: bold; " +
            "      padding: 4px 8px; " +
            "      border-radius: 4px; " +
            "      display: inline-block; " +
            "    } " +
            "  `;" +
            "  document.head.appendChild(style);" +
            "}"
        );

        H2 titulo = new H2("🚚 Gestão Operacional de Carregamento");
        titulo.getStyle()
                .set("color", "#ffffff")
                .set("margin", "0")
                .set("font-size", "1.6rem")
                .set("font-weight", "700");

        Div containerKPI = criarCardsKPIs();
        HorizontalLayout barraAcoes = criarBarraAcoes();
        configurarGrid();

        aplicarPermissoesPerfil();

        add(titulo, containerKPI, barraAcoes, grid);
        atualizarGridEIndicators();
    }
    
    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        atualizarGridEIndicators();
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        UI ui = attachEvent.getUI();

        broadcasterRegistration = UiBroadcaster.register(message -> {
            ui.access(() -> {
                atualizarApenasDadosGridEIndicators();
            });
        });
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        if (broadcasterRegistration != null) {
            broadcasterRegistration.remove();
            broadcasterRegistration = null;
        }
        super.onDetach(detachEvent);
    }

    private void aplicarPermissoesPerfil() {
        boolean isPcl = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication()
            .getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_PCL"));

        if (btnArquivarExpedidas != null) btnArquivarExpedidas.setVisible(!isPcl);
        if (btnExcluirSelecionadas != null) btnExcluirSelecionadas.setVisible(!isPcl);
        if (btnLimparCheckin != null) btnLimparCheckin.setVisible(!isPcl);
        if (btnNovo != null) btnNovo.setVisible(!isPcl);

        if (btnVerArquivados != null) btnVerArquivados.setVisible(true);
        if (btnVerFila != null) btnVerFila.setVisible(true);
        if (btnRelatorioPaletes != null) btnRelatorioPaletes.setVisible(true);
        if (uploadExcelComponent != null) uploadExcelComponent.setVisible(true);
    }

    private Div criarCardsKPIs() {
        Div container = new Div();
        container.addClassName("kpi-container");

        txtTotal.addClassName("kpi-value");
        txtPendentes.addClassName("kpi-value");
        txtApresentados.addClassName("kpi-value");
        txtCarregando.addClassName("kpi-value");
        txtExpedidos.addClassName("kpi-value");
        txtPeso.addClassName("kpi-value");

        container.add(
            criarCardSingle("TOTAL CARREGAMENTOS", txtTotal, VaadinIcon.TRUCK.create(), "total", "kpi-icon-total", "TODOS"),
            criarCardSingle("PENDENTES", txtPendentes, VaadinIcon.TIME_BACKWARD.create(), "pendentes", "kpi-icon-pendentes", "Pendente"),
            criarCardSingle("APRESENTADOS", txtApresentados, VaadinIcon.CHECK_CIRCLE.create(), "apresentados", "kpi-icon-apresentados", "Apresentado"),
            criarCardSingle("CARREGANDO", txtCarregando, VaadinIcon.CLOCK.create(), "carregando", "kpi-icon-carregando", "Carregando"),
            criarCardSingle("EXPEDIDOS", txtExpedidos, VaadinIcon.PACKAGE.create(), "expedidos", "kpi-icon-expedidos", "Expedido"),
            criarCardSingle("PESO PROGRAMADO", txtPeso, VaadinIcon.SCALE.create(), "peso", "kpi-icon-peso", null)
        );

        return container;
    }

    private Div criarCardSingle(String titulo, Span valorSpan, Icon icone, String classeVariacao, String classeIcone, String statusFiltro) {
        Div card = new Div();
        card.addClassName("kpi-card");
        card.addClassName(classeVariacao);

        Div header = new Div();
        header.addClassName("kpi-header");

        Span lblTitulo = new Span(titulo);
        lblTitulo.addClassName("kpi-title");

        icone.addClassName(classeIcone);
        header.add(lblTitulo, icone);

        card.add(header, valorSpan);

        if (statusFiltro != null) {
            card.getStyle().set("cursor", "pointer");
            card.addClickListener(e -> {
                statusFiltroAtual = statusFiltro;
                aplicarFiltroStatus(statusFiltroAtual);
            });
        }

        return card;
    }
  
    private void atualizarApenasDadosGridEIndicators() {
    List<Carregamento> listaAtivos = excelService.listarCarregamentosParaMainView();
    listaAtivos.sort((c1, c2) -> Long.compare(c2.getId() != null ? c2.getId() : 0L, c1.getId() != null ? c1.getId() : 0L));

    aplicarFiltroStatus(statusFiltroAtual);

    long total = listaAtivos.size();
    long apresentados = listaAtivos.stream()
        .filter(c -> c.getStatus() != null && c.getStatus().trim().equalsIgnoreCase("Apresentado"))
        .count();
    long carregando = listaAtivos.stream()
        .filter(c -> c.getStatus() != null && c.getStatus().trim().equalsIgnoreCase("Carregando"))
        .count();
    long expedidos = listaAtivos.stream()
        .filter(c -> c.getStatus() != null && c.getStatus().trim().equalsIgnoreCase("Expedido"))
        .count();
    long pendentes = total - (apresentados + carregando + expedidos);

    double pesoTotal = listaAtivos.stream()
        .mapToDouble(c -> converterPesoParaDouble(c.getPeso()))
        .sum();

    DecimalFormat df = new DecimalFormat("#,##0.00", new DecimalFormatSymbols(Locale.forLanguageTag("pt-BR")));

    txtTotal.setText(String.valueOf(total));
    txtPendentes.setText(String.valueOf(pendentes));
    txtApresentados.setText(String.valueOf(apresentados));
    txtCarregando.setText(String.valueOf(carregando));
    txtExpedidos.setText(String.valueOf(expedidos));
    txtPeso.setText(df.format(pesoTotal) + " kg");
}

    private void aplicarFiltroStatus(String status) {
    List<Carregamento> todosAtivos = excelService.listarCarregamentosParaMainView();

    if (status == null) return;

    if ("TODOS".equalsIgnoreCase(status)) {
        grid.setItems(todosAtivos);
    } else if ("PENDENTE".equalsIgnoreCase(status) || "PENDENTES".equalsIgnoreCase(status)) {
        List<Carregamento> pendentes = todosAtivos.stream()
            .filter(c -> {
                if (c.getStatus() == null || c.getStatus().trim().isEmpty()) {
                    return true;
                }
                String st = c.getStatus().trim();
                return "Pendente".equalsIgnoreCase(st)
                    || (!"Apresentado".equalsIgnoreCase(st) 
                     && !"Carregando".equalsIgnoreCase(st) 
                     && !"Expedido".equalsIgnoreCase(st));
            })
            .toList();
        grid.setItems(pendentes);
    } else {
        List<Carregamento> filtrados = todosAtivos.stream()
            .filter(c -> c.getStatus() != null && c.getStatus().trim().equalsIgnoreCase(status))
            .toList();
        grid.setItems(filtrados);
    }
}

    @SuppressWarnings("null")
    private HorizontalLayout criarBarraAcoes() {
        HorizontalLayout layout = new HorizontalLayout();
        layout.setWidthFull();
        layout.setAlignItems(Alignment.CENTER);
        layout.setJustifyContentMode(JustifyContentMode.BETWEEN);

        HorizontalLayout grupoEsquerda = new HorizontalLayout();
        grupoEsquerda.setAlignItems(Alignment.CENTER);

        btnNovo = new Button("Novo Carregamento", VaadinIcon.PLUS.create());
        btnNovo.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        btnNovo.getStyle()
                .set("background", "linear-gradient(135deg, #2563eb, #1d4ed8)")
                .set("font-weight", "600")
                .set("border-radius", "6px")
                .set("box-shadow", "0 4px 12px rgba(37, 99, 235, 0.3)");
        btnNovo.addClickListener(e -> abrirFormularioModal(new Carregamento()));

        btnArquivarExpedidas = new Button("Arquivar Expedidas", VaadinIcon.ARCHIVE.create());
        btnArquivarExpedidas.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);
        btnArquivarExpedidas.getStyle()
                .set("background", "linear-gradient(135deg, #059669, #047857)")
                .set("color", "#ffffff")
                .set("font-weight", "600");

        btnArquivarExpedidas.addClickListener(e -> {
            List<Carregamento> expedidosAtivos = excelService.listarCarregamentosParaMainView().stream()
                .filter(c -> c.getStatus() != null && c.getStatus().equalsIgnoreCase("Expedido"))
                .toList();
            
            if (expedidosAtivos.isEmpty()) {
                Notification.show("Nenhuma carga expedida ativa para arquivar.", 3000, Notification.Position.BOTTOM_END);
                return;
            }

            for (Carregamento c : expedidosAtivos) {
                c.setArquivado(true);
                repository.save(c);
            }
            atualizarGridEIndicators();
            UiBroadcaster.broadcast("STATUS_ATUALIZADO");
            Notification.show(expedidosAtivos.size() + " cargas expedidas foram arquivadas.", 3000, Notification.Position.BOTTOM_END);
        });

        btnExcluirSelecionadas = new Button("Excluir Selecionadas", VaadinIcon.TRASH.create());
        btnExcluirSelecionadas.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_SMALL);
        btnExcluirSelecionadas.addClickListener(e -> {
            List<Carregamento> selecionados = mapaCheckboxesMain.entrySet().stream()
                    .filter(entry -> entry.getValue() != null && entry.getValue().getValue())
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());

            if (selecionados.isEmpty()) {
                Notification.show("Nenhuma carga selecionada para exclusão.", 3000, Notification.Position.MIDDLE);
                return;
            }

            try {
                for (Carregamento c : selecionados) {
                    entregaRepository.deleteByCarregamentoId(c.getId());
                }

                repository.deleteAll(selecionados);
                mapaCheckboxesMain.clear();
                atualizarGridEIndicators();
                UiBroadcaster.broadcast("STATUS_ATUALIZADO");
                
                Notification.show(selecionados.size() + " carga(s) excluída(s) com sucesso!", 3000, Notification.Position.BOTTOM_END);
            } catch (Exception ex) {
                Notification.show("Erro ao excluir: " + ex.getMessage(), 5000, Notification.Position.MIDDLE);
            }
        }); 
        
        btnLimparCheckin = new Button("Limpar Checkin", VaadinIcon.REFRESH.create());
        btnLimparCheckin.addThemeVariants(ButtonVariant.LUMO_SMALL);
        btnLimparCheckin.getStyle().set("font-weight", "600");
        btnLimparCheckin.addClickListener(e -> {
            List<Carregamento> selecionados = mapaCheckboxesMain.entrySet().stream()
                    .filter(entry -> entry.getValue().getValue())
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());

            if (selecionados.isEmpty()) {
                Notification.show("Nenhuma carga selecionada para limpar o checkin.", 3000, Notification.Position.MIDDLE);
                return;
            }

            for (Carregamento c : selecionados) {
                c.setStatus("Pendente");
                c.setMotorista(null);
                c.setMotoristaEntidade(null);
            }
            repository.saveAll(selecionados);
            mapaCheckboxesMain.clear();
            atualizarGridEIndicators();
            UiBroadcaster.broadcast("STATUS_ATUALIZADO");
            
            Notification.show("Checkin e motorista limpos para " + selecionados.size() + " carga(s)!", 3000, Notification.Position.BOTTOM_END);
        });

        btnVerArquivados = new Button("Ver Arquivados", VaadinIcon.FOLDER_OPEN.create());
        btnVerArquivados.addThemeVariants(ButtonVariant.LUMO_SMALL);
        btnVerArquivados.addClickListener(e -> abrirModalArquivados());

        btnVerFila = new Button("Ver Fila", VaadinIcon.LIST.create());
        btnVerFila.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);
        btnVerFila.getStyle()
                .set("background", "linear-gradient(135deg, #3b82f6, #1d4ed8)")
                .set("color", "#ffffff")
                .set("font-weight", "600");
        btnVerFila.addClickListener(e -> abrirModalFila());

        btnRelatorioPaletes = new Button("Relatório Paletes", VaadinIcon.PRINT.create());
        btnRelatorioPaletes.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);
        btnRelatorioPaletes.getStyle()
                .set("background", "linear-gradient(135deg, #059669, #047857)")
                .set("color", "#ffffff")
                .set("font-weight", "600");
        btnRelatorioPaletes.addClickListener(e -> UI.getCurrent().navigate(RelatorioPaletesView.class));

        grupoEsquerda.add(btnNovo, btnArquivarExpedidas, btnExcluirSelecionadas, btnLimparCheckin, btnVerArquivados, btnVerFila, btnRelatorioPaletes);
        
        FileBuffer fileBuffer = new FileBuffer();
        Upload uploadExcel = new Upload(fileBuffer);
        uploadExcel.setAcceptedFileTypes(".xlsx", ".xls");
        uploadExcel.setDropLabel(new Span("Arraste o arquivo Excel (.xlsx) aqui"));
        uploadExcel.setUploadButton(new Button("Upload Excel", VaadinIcon.UPLOAD.create()));
        uploadExcelComponent = uploadExcel;

        uploadExcel.addSucceededListener(event -> {
            try {
                InputStream is = fileBuffer.getInputStream();
                excelService.processarExcel(is);

                getUI().ifPresent(ui -> ui.access(() -> {
                    atualizarGridEIndicators();
                    UiBroadcaster.broadcast("STATUS_ATUALIZADO");
                    Notification n = Notification.show("Planilha importada com sucesso!", 3000, Notification.Position.BOTTOM_END);
                    n.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                }));

            } catch (Exception ex) {
                ex.printStackTrace();
                getUI().ifPresent(ui -> ui.access(() -> {
                    String msg = ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName();
                    Notification n = Notification.show("Erro ao processar: " + msg, 5000, Notification.Position.MIDDLE);
                    n.addThemeVariants(NotificationVariant.LUMO_ERROR);
                }));
            }
        });

        layout.add(grupoEsquerda, uploadExcel);
        return layout;
    }

    @SuppressWarnings("null")
    private void abrirModalArquivados() {
        Dialog modalArquivados = new Dialog();
        modalArquivados.setWidth("85vw");
        modalArquivados.setHeight("80vh");
        modalArquivados.setHeaderTitle("Histórico de Cargas Arquivadas");

        modalArquivados.getElement().getStyle()
            .set("background-color", "#0f172a")
            .set("color", "#ffffff")
            .set("--lumo-base-color", "#0f172a")
            .set("--lumo-body-text-color", "#ffffff");

        Grid<Carregamento> gridArquivados = new Grid<>(Carregamento.class, false);
        gridArquivados.setSizeFull();
        gridArquivados.addThemeVariants(GridVariant.LUMO_ROW_STRIPES, GridVariant.LUMO_COMPACT);

        Map<Carregamento, Checkbox> mapaCheckboxesArquivados = new HashMap<>();

        Checkbox masterCheckboxArquivados = new Checkbox();
        masterCheckboxArquivados.setValue(false);
        masterCheckboxArquivados.getStyle().set("border", "2px solid #3b82f6");
        masterCheckboxArquivados.getStyle().set("border-radius", "4px");
        masterCheckboxArquivados.addValueChangeListener(event -> {
            boolean masterValue = event.getValue();
            for (Checkbox cb : mapaCheckboxesArquivados.values()) {
                cb.setValue(masterValue);
            }
        });
                
        gridArquivados.addComponentColumn(carregamento -> {
            Checkbox checkbox = new Checkbox();
            checkbox.setValue(false);
            checkbox.getStyle().set("border", "2px solid #3b82f6");
            checkbox.getStyle().set("border-radius", "4px");
            checkbox.getStyle().set("padding", "2px");
            mapaCheckboxesArquivados.put(carregamento, checkbox);
            return checkbox;
        }).setHeader(masterCheckboxArquivados).setWidth("70px").setFlexGrow(0);

        gridArquivados.addColumn(Carregamento::getId).setHeader("ID").setAutoWidth(true);
        gridArquivados.addColumn(Carregamento::getDataProgramacao).setHeader("DATA PROG.").setAutoWidth(true);
        gridArquivados.addColumn(Carregamento::getTransportadora).setHeader("TRANSPORTADORA").setAutoWidth(true);
        gridArquivados.addColumn(Carregamento::getPlaca).setHeader("PLACA").setAutoWidth(true);
        gridArquivados.addColumn(Carregamento::getViagem).setHeader("VIAGEM").setAutoWidth(true);
        gridArquivados.addColumn(Carregamento::getStatus).setHeader("STATUS").setAutoWidth(true);

       DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

       // Função auxiliar para calcular e formatar a duração em horas e minutos
        java.util.function.BiFunction<LocalDateTime, LocalDateTime, String> formatarDuracao = (inicio, fim) -> {
    if (inicio == null || fim == null) return "-";
    java.time.Duration duracao = java.time.Duration.between(inicio, fim);
    if (duracao.isNegative()) return "0h 0m";
    long horas = duracao.toHours();
    long minutos = duracao.toMinutesPart();
    return horas + "h " + minutos + "m";
};

        // Espera Fila (Início da Carga - Apresentação/Chegada)
        gridArquivados.addColumn(c -> formatarDuracao.apply(c.getDataHoraApresentacao(), c.getHoraInicioCarregamento()))
            .setHeader("ESPERA FILA").setAutoWidth(true);

        // Tempo de Carga (Fim da Carga - Início da Carga)
        gridArquivados.addColumn(c -> formatarDuracao.apply(c.getHoraInicioCarregamento(), c.getHoraFimCarregamento()))
            .setHeader("TEMPO CARGA").setAutoWidth(true);

        // Lead Time Total (Fim da Carga - Apresentação/Chegada)
        gridArquivados.addColumn(c -> formatarDuracao.apply(c.getDataHoraApresentacao(), c.getHoraFimCarregamento()))
            .setHeader("LEAD TIME TOTAL").setAutoWidth(true);

        gridArquivados.addColumn(c -> c.getDataHoraApresentacao() != null ? c.getDataHoraApresentacao().format(formatter) : "-")
            .setHeader("CHEGADA").setAutoWidth(true);
        gridArquivados.addColumn(c -> c.getHoraInicioCarregamento() != null ? c.getHoraInicioCarregamento().format(formatter) : "-")
            .setHeader("INÍCIO CARGA").setAutoWidth(true);

        gridArquivados.addColumn(c -> c.getHoraFimCarregamento() != null ? c.getHoraFimCarregamento().format(formatter) : "-")
            .setHeader("FIM CARGA").setAutoWidth(true);
        
        gridArquivados.addColumn(new ComponentRenderer<>(carregamento -> {
            Button btnDesarquivar = new Button("Desarquivar", VaadinIcon.UPLOAD_ALT.create());
            btnDesarquivar.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_SUCCESS);
            btnDesarquivar.addClickListener(e -> {
                carregamento.setArquivado(false);
                repository.save(carregamento);
                atualizarGridEIndicators();
                UiBroadcaster.broadcast("STATUS_ATUALIZADO");
                
               mapaCheckboxesArquivados.clear();
            List<Carregamento> novaListaArquivados = repository.findByArquivadoTrue();
            gridArquivados.setItems(novaListaArquivados);

            Notification.show("Viagem desarquivada com sucesso!", 3000, Notification.Position.BOTTOM_END);
            });
            return btnDesarquivar;
            })).setHeader("AÇÃO").setAutoWidth(true);

       List<Carregamento> listaArquivados = repository.findByArquivadoTrue();
         gridArquivados.setItems(listaArquivados);;

        Button btnDesarquivarSelecionados = new Button("Desarquivar Selecionadas", VaadinIcon.UPLOAD_ALT.create());
        btnDesarquivarSelecionados.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SUCCESS);
        btnDesarquivarSelecionados.addClickListener(e -> {
            List<Carregamento> selecionadas = mapaCheckboxesArquivados.entrySet().stream()
                .filter(entry -> entry.getValue() != null && entry.getValue().getValue())
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

            if (selecionadas.isEmpty()) {
                Notification.show("Nenhuma carga selecionada para desarquivar.", 3000, Notification.Position.MIDDLE);
                return;
            }

            for (Carregamento c : selecionadas) {
                c.setArquivado(false);
                repository.save(c);
            }

            mapaCheckboxesArquivados.clear();
            List<Carregamento> novaListaArquivados = repository.findByArquivadoTrue();
            gridArquivados.setItems(novaListaArquivados);

            atualizarGridEIndicators();
            UiBroadcaster.broadcast("STATUS_ATUALIZADO");
            Notification.show(selecionadas.size() + " carga(s) desarquivada(s) com sucesso!", 3000, Notification.Position.BOTTOM_END);
        });

        Button btnFechar = new Button("Fechar", e -> modalArquivados.close());
        btnFechar.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        HorizontalLayout footerLayout = new HorizontalLayout(btnDesarquivarSelecionados, btnFechar);
        footerLayout.setJustifyContentMode(JustifyContentMode.BETWEEN);
        footerLayout.setWidthFull();

        modalArquivados.getFooter().add(footerLayout);
        modalArquivados.add(gridArquivados);
        modalArquivados.open();
    }

    @SuppressWarnings("null")
    private void abrirModalFila() {
        Dialog modalFila = new Dialog();
        modalFila.setWidth("85vw");
        modalFila.setHeight("80vh");
        modalFila.setHeaderTitle("🕒 Fila de Espera para Carregamento (Ordem de Chegada)");

        modalFila.getElement().getStyle()
            .set("background-color", "#0f172a")
            .set("color", "#ffffff")
            .set("--lumo-base-color", "#0f172a")
            .set("--lumo-body-text-color", "#ffffff");

        Grid<Carregamento> gridFila = new Grid<>(Carregamento.class, false);
        gridFila.setSizeFull();
        gridFila.addThemeVariants(GridVariant.LUMO_ROW_STRIPES, GridVariant.LUMO_COMPACT);

        gridFila.getStyle()
            .set("background-color", "#0f172a")
            .set("border", "1px solid #1e293b")
            .set("border-radius", "8px");

        gridFila.addColumn(Carregamento::getId).setHeader("ID").setAutoWidth(true);
        gridFila.addColumn(Carregamento::getDataProgramacao).setHeader("DATA PROG.").setAutoWidth(true);
        gridFila.addColumn(Carregamento::getTransportadora).setHeader("TRANSPORTADORA").setAutoWidth(true);
        gridFila.addColumn(Carregamento::getPlaca).setHeader("PLACA").setAutoWidth(true);
        gridFila.addColumn(Carregamento::getTipoVeiculo).setHeader("TIPO VEÍCULO").setAutoWidth(true);
        gridFila.addColumn(Carregamento::getViagem).setHeader("VIAGEM").setAutoWidth(true);
        
       gridFila.addColumn(c -> {
           if (c.getDataHoraApresentacao() != null) {
               return c.getDataHoraApresentacao().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
           }
           return "-";
       }).setHeader("HORA CHEGADA").setAutoWidth(true);

        gridFila.addColumn(Carregamento::getStatus).setHeader("STATUS").setAutoWidth(true);

        List<Carregamento> listaFila = excelService.listarCarregamentosParaMainView().stream()
            .filter(c -> c.getStatus() != null &&
                       c.getStatus().trim().equalsIgnoreCase("Apresentado"))
            .sorted((c1, c2) -> {
                if (c1.getDataHoraApresentacao() == null) return 1;
                if (c2.getDataHoraApresentacao() == null) return -1;
                return c1.getDataHoraApresentacao().compareTo(c2.getDataHoraApresentacao());
            })
            .toList();

        gridFila.setItems(listaFila);

        Button btnFechar = new Button("Fechar", e -> modalFila.close());
        btnFechar.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        HorizontalLayout footerLayout = new HorizontalLayout(new Span("Total na fila: " + listaFila.size() + " veículo(s)"), btnFechar);
        footerLayout.setJustifyContentMode(JustifyContentMode.BETWEEN);
        footerLayout.setWidthFull();
        footerLayout.setAlignItems(FlexComponent.Alignment.CENTER);

        modalFila.getFooter().add(footerLayout);
        modalFila.add(gridFila);
        modalFila.open();
    }

    @SuppressWarnings("null")
    private void configurarGrid() {
        grid.setSizeFull();
        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES, GridVariant.LUMO_NO_BORDER, GridVariant.LUMO_COMPACT);

        grid.getStyle()
            .set("--lumo-size-m", "36px")
            .set("--lumo-font-size-s", "12px")
            .set("--lumo-base-color", "#0f172a")
            .set("--lumo-body-text-color", "#f8fafc")
            .set("--lumo-contrast-5pct", "rgba(255, 255, 255, 0.05)")
            .set("--lumo-contrast-10pct", "rgba(255, 255, 255, 0.1)")
            .set("background-color", "#0f172a")
            .set("border", "1px solid #1e293b")
            .set("border-radius", "8px");

        mapaCheckboxesMain.clear();

        Checkbox masterCheckbox = new Checkbox();
        masterCheckbox.setValue(false);
        masterCheckbox.getStyle().set("border", "2px solid #3b82f6");
        masterCheckbox.getStyle().set("border-radius", "4px");
        masterCheckbox.addValueChangeListener(event -> {
            boolean masterValue = event.getValue();
            for (Checkbox cb : mapaCheckboxesMain.values()) {
                cb.setValue(masterValue);
            }
        });

        grid.addComponentColumn(carregamento -> {
            Checkbox checkbox = new Checkbox();
            checkbox.setValue(false);
            checkbox.getStyle().set("border", "2px solid #3b82f6");
            checkbox.getStyle().set("border-radius", "4px");
            checkbox.getStyle().set("padding", "2px");
            mapaCheckboxesMain.put(carregamento, checkbox);
            return checkbox;
        }).setHeader(masterCheckbox).setWidth("70px").setFlexGrow(0);

        grid.addColumn(Carregamento::getId).setHeader("ID").setAutoWidth(true).setFlexGrow(0);
        grid.addColumn(Carregamento::getDataProgramacao).setHeader("DATA PROG.").setAutoWidth(true);
        grid.addColumn(Carregamento::getTransportadora).setHeader("TRANSPORTADORA").setAutoWidth(true);
        
        grid.addComponentColumn(carregamento -> {
            Span spanPlaca = new Span(carregamento.getPlaca() != null ? carregamento.getPlaca() : "-");
            
            if (carregamento.getPlacaAntiga() != null && !carregamento.getPlacaAntiga().trim().isEmpty()) {
                spanPlaca.getElement().setAttribute("title", "Placa Anterior: " + carregamento.getPlacaAntiga());
                spanPlaca.addClassName("placa-alterada");
            }
            return spanPlaca;
        }).setHeader("PLACA").setAutoWidth(true);

        grid.addColumn(c -> {
            if (c.getMotoristaEntidade() != null && c.getMotoristaEntidade().getNome() != null) {
                return c.getMotoristaEntidade().getNome();
            }
            return c.getMotorista() != null ? c.getMotorista() : "-";
        }).setHeader("MOTORISTA").setAutoWidth(true);

        grid.addColumn(Carregamento::getTipoVeiculo).setHeader("TIPO DE VEÍCULO").setAutoWidth(true);
        grid.addColumn(Carregamento::getViagem).setHeader("VIAGEM").setAutoWidth(true);
        grid.addColumn(Carregamento::getOrdemCarga).setHeader("ORDEM DE CARGA").setAutoWidth(true);
        grid.addColumn(Carregamento::getPeso).setHeader("PESO").setAutoWidth(true);
        grid.addColumn(Carregamento::getEncaixe).setHeader("ENCAIXE").setAutoWidth(true);
        
        grid.addColumn(Carregamento::getConferente).setHeader("CONFERENTE");
        grid.addColumn(Carregamento::getDoca).setHeader("DOCA");

        grid.addComponentColumn(carregamento -> criarBotoesStatus(carregamento))
            .setHeader("STATUS")
            .setAutoWidth(true);
        
        grid.addColumn(Carregamento::getObservacao).setHeader("OBSERVAÇÃO").setAutoWidth(true);
        
        grid.addColumn(new ComponentRenderer<>(carregamento -> {
            HorizontalLayout acoes = new HorizontalLayout();
            acoes.setSpacing(true);
            acoes.setPadding(false);
            acoes.setMargin(false);

            Button btnEditar = new Button(VaadinIcon.EDIT.create());
            btnEditar.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
            btnEditar.getStyle()
                    .set("color", "#38bdf8")
                    .set("cursor", "pointer");
            btnEditar.setTooltipText("Editar Carregamento");
            btnEditar.addClickListener(e -> abrirFormularioModal(carregamento));

            Button btnEntregas = new Button("Entregas", VaadinIcon.PACKAGE.create());
            btnEntregas.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);
            btnEntregas.getStyle()
                    .set("background-color", "#2563eb")
                    .set("color", "#ffffff")
                    .set("font-weight", "600")
                    .set("border-radius", "4px")
                    .set("cursor", "pointer");

            btnEntregas.addClickListener(e -> {
                if (carregamento.getId() != null) {
                    UI.getCurrent().navigate("entregas/" + carregamento.getId());
                } else {
                    Notification.show("Salve o carregamento primeiro para gerenciar as entregas.", 3000, Notification.Position.MIDDLE);
                }
            });

            acoes.add(btnEditar, btnEntregas);
            return acoes;
        })).setHeader("AÇÕES").setAutoWidth(true);
    }

    private Component criarBotoesStatus(Carregamento carregamento) {
        HorizontalLayout layout = new HorizontalLayout();
        layout.setSpacing(true);
        layout.setPadding(false);
        layout.setAlignItems(FlexComponent.Alignment.CENTER);

        String statusAtual = carregamento.getStatus() != null ? carregamento.getStatus().trim() : "";

        Button btnApresentado = new Button("Apresentado");
        Button btnCarregando = new Button("Carregando");
        Button btnExpedido = new Button("Expedido");

        boolean isApresentado = "Apresentado".equalsIgnoreCase(statusAtual);
        boolean isCarregando = "Carregando".equalsIgnoreCase(statusAtual);
        boolean isExpedido = "Expedido".equalsIgnoreCase(statusAtual);

        aplicarEstiloBotao(btnApresentado, isApresentado, "#3b82f6");
        aplicarEstiloBotao(btnCarregando, isCarregando, "#f59e0b");
        aplicarEstiloBotao(btnExpedido, isExpedido, "#10b981");

      btnApresentado.addClickListener(e -> {
    carregamento.setStatus("Apresentado");
    
    // Usa o método correto que a Grid está consultando: getDataHoraApresentacao / setDataHoraApresentacao
    if (carregamento.getDataHoraApresentacao() == null) {
        carregamento.setDataHoraApresentacao(java.time.LocalDateTime.now());
    }

    repository.save(carregamento);
    atualizarGridEIndicators();
    UiBroadcaster.broadcast("STATUS_ATUALIZADO");
});

        btnCarregando.addClickListener(e -> {
            if (!"Apresentado".equalsIgnoreCase(carregamento.getStatus())) {
                Notification.show("⚠️ O veículo precisa estar como 'Apresentado' antes de iniciar o carregamento!", 
                    3000, Notification.Position.MIDDLE);
                return;
            }

            carregamento.setStatus("Carregando");
            if (carregamento.getHoraInicioCarregamento() == null) {
                carregamento.setHoraInicioCarregamento(LocalDateTime.now());
            }
            repository.save(carregamento);
            atualizarGridEIndicators();
            UiBroadcaster.broadcast("STATUS_ATUALIZADO");
        });

        btnExpedido.addClickListener(e -> {
            if (!"Carregando".equalsIgnoreCase(carregamento.getStatus())) {
                Notification.show("⚠️ O veículo precisa estar 'Carregando' antes de ser expedido!", 
                    3000, Notification.Position.MIDDLE);
                return;
            }

            carregamento.setStatus("Expedido");
            if (carregamento.getHoraFimCarregamento() == null) {
                carregamento.setHoraFimCarregamento(LocalDateTime.now());
            }
            repository.save(carregamento);
            atualizarGridEIndicators();
            UiBroadcaster.broadcast("STATUS_ATUALIZADO");
        });

        layout.add(btnApresentado, btnCarregando, btnExpedido);
        return layout;
    }

    private void aplicarEstiloBotao(Button botao, boolean ativo, String corAtivaHex) {
        botao.getStyle().set("font-size", "0.70rem");
        botao.getStyle().set("height", "26px");
        botao.getStyle().set("padding", "0 8px");
        botao.getStyle().set("border-radius", "4px");
        
        if (ativo) {
            botao.getStyle().set("background-color", corAtivaHex);
            botao.getStyle().set("color", "white");
            botao.getStyle().set("font-weight", "bold");
            botao.getStyle().set("opacity", "1.0");
        } else {
            botao.getStyle().set("background-color", "#1f2937");
            botao.getStyle().set("color", "#9ca3af");
            botao.getStyle().set("opacity", "0.5");
        }
    }

    private void abrirFormularioModal(Carregamento carregamento) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(carregamento.getId() == null ? "Novo Carregamento" : "Editar Carregamento #" + carregamento.getId());

        dialog.getElement().getStyle()
            .set("background-color", "#0f172a")
            .set("color", "#ffffff")
            .set("--lumo-base-color", "#0f172a")
            .set("--lumo-body-text-color", "#ffffff")
            .set("--lumo-header-text-color", "#ffffff")
            .set("--lumo-secondary-text-color", "#cbd5e1");

        FormLayout form = new FormLayout();

        TextField txtData = new TextField("Data Programação");
        txtData.setValue(carregamento.getDataProgramacao() != null ? carregamento.getDataProgramacao() : "");

        TextField txtTransp = new TextField("Transportadora");
        txtTransp.setValue(carregamento.getTransportadora() != null ? carregamento.getTransportadora() : "");

        TextField txtPlaca = new TextField("Placa Atual");
        txtPlaca.setValue(carregamento.getPlaca() != null ? carregamento.getPlaca() : "");
        txtPlaca.setReadOnly(true);

        TextField txtNovaPlaca = new TextField("Nova Placa (Substituição)");
        txtNovaPlaca.setPlaceholder("Digite a nova placa se houver troca");

        TextField txtTipoVeiculo = new TextField("Tipo de Veículo");
        txtTipoVeiculo.setValue(carregamento.getTipoVeiculo() != null ? carregamento.getTipoVeiculo() : "");

        TextField txtViagem = new TextField("Viagem");
        txtViagem.setValue(carregamento.getViagem() != null ? carregamento.getViagem() : "");

        TextField txtOrdemCarga = new TextField("Ordem de Carga");
        txtOrdemCarga.setValue(carregamento.getOrdemCarga() != null ? carregamento.getOrdemCarga() : "");

        TextField txtPeso = new TextField("Peso");
        txtPeso.setValue(carregamento.getPeso() != null ? carregamento.getPeso() : "");

        TextField txtEncaixe = new TextField("Encaixe");
        txtEncaixe.setValue(carregamento.getEncaixe() != null ? carregamento.getEncaixe() : "");

        ComboBox<String> cbConferente = new ComboBox<>("Conferente *");
        cbConferente.getElement().setAttribute("theme", "dark");
        List<String> nomesConferentes = conferenteRepository.findAll().stream()
                .map(Conferente::getNome)
                .collect(Collectors.toList());
        cbConferente.setItems(nomesConferentes);
        cbConferente.setValue(carregamento.getConferente() != null ? carregamento.getConferente() : "");

        boolean isNovoRegistro = (carregamento.getId() == null);
        boolean isAdmin = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication()
                .getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        
        if (!isNovoRegistro && !isAdmin) {
            cbConferente.setReadOnly(true);
        }

        ComboBox<String> cbDoca = new ComboBox<>("Doca *");
        cbDoca.getElement().setAttribute("theme", "dark");
        cbDoca.setItems("01", "02", "03", "04", "05", "06", "07", "08");
        cbDoca.setValue(carregamento.getDoca() != null ? carregamento.getDoca() : "");

        ComboBox<String> cbStatus = new ComboBox<>("Status");
        cbStatus.setItems("Pendente", "Apresentado", "Carregando", "Expedido");
        cbStatus.setValue(carregamento.getStatus() != null ? carregamento.getStatus() : "Pendente");

        TextField txtObs = new TextField("Observação");
        txtObs.setValue(carregamento.getObservacao() != null ? carregamento.getObservacao() : "");

        boolean isPcl = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication()
            .getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_PCL"));

        if (isPcl) {
            txtData.setReadOnly(true);
            txtTransp.setReadOnly(true);
            txtTipoVeiculo.setReadOnly(true);
            txtViagem.setReadOnly(true);
            txtOrdemCarga.setReadOnly(true);
            txtPeso.setReadOnly(true);
            txtEncaixe.setReadOnly(true);
            cbStatus.setReadOnly(true);
            
            // Garante que o PCL pode alterar o conferente e a doca
            cbConferente.setReadOnly(false);
            cbDoca.setReadOnly(false);
        }

        estilitarCampoEscuro(txtData);
        estilitarCampoEscuro(txtTransp);
        estilitarCampoEscuro(txtPlaca);
        estilitarCampoEscuro(txtNovaPlaca);
        estilitarCampoEscuro(txtTipoVeiculo);
        estilitarCampoEscuro(txtViagem);
        estilitarCampoEscuro(txtOrdemCarga);
        estilitarCampoEscuro(txtPeso);
        estilitarCampoEscuro(txtEncaixe);
        estilitarCampoEscuro(cbConferente);
        estilitarCampoEscuro(cbDoca);
        estilitarCampoEscuro(cbStatus);
        estilitarCampoEscuro(txtObs);

        form.add(txtData, txtTransp, txtPlaca, txtNovaPlaca, txtTipoVeiculo, txtViagem, txtOrdemCarga, txtPeso, txtEncaixe, cbConferente, cbDoca, cbStatus, txtObs);
        dialog.add(form);

        Button btnSalvar = new Button("Salvar", e -> {
            String conferenteSelecionado = cbConferente.getValue();
            String docaSelecionada = cbDoca.getValue();

            if (conferenteSelecionado == null || conferenteSelecionado.trim().isEmpty() ||
                docaSelecionada == null || docaSelecionada.trim().isEmpty()) {
                
                Notification.show("⚠️ Os campos Conferente e Doca são de preenchimento obrigatório!", 4000, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }

            carregamento.setDataProgramacao(txtData.getValue());
            carregamento.setTransportadora(txtTransp.getValue());
            
            String novaPlaca = txtNovaPlaca.getValue();
            if (novaPlaca != null && !novaPlaca.trim().isEmpty()) {
                if (carregamento.getPlacaAntiga() == null || carregamento.getPlacaAntiga().isEmpty()) {
                    carregamento.setPlacaAntiga(carregamento.getPlaca());
                }
                carregamento.setPlaca(novaPlaca.trim().toUpperCase());
            }

            carregamento.setTipoVeiculo(txtTipoVeiculo.getValue());
            carregamento.setViagem(txtViagem.getValue());
            carregamento.setOrdemCarga(txtOrdemCarga.getValue());
            carregamento.setPeso(txtPeso.getValue());
            carregamento.setEncaixe(txtEncaixe.getValue());
            
            if (isNovoRegistro || isAdmin || isPcl) {
                carregamento.setConferente(conferenteSelecionado);
            }
            
            carregamento.setDoca(docaSelecionada);
            carregamento.setStatus(cbStatus.getValue());
            carregamento.setObservacao(txtObs.getValue());

            repository.save(carregamento);
            atualizarGridEIndicators();
            UiBroadcaster.broadcast("STATUS_ATUALIZADO");

            Notification.show("Carregamento salvo com sucesso!", 3000, Notification.Position.TOP_END)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            dialog.close();
        });
        btnSalvar.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        Button btnCancelar = new Button("Cancelar", e -> dialog.close());

        dialog.getFooter().add(btnCancelar, btnSalvar);
        dialog.open();
    }

    private void atualizarGridEIndicators() {
        mapaCheckboxesMain.clear();
        
        List<Carregamento> listaAtivos = repository.findAll(org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "id"))
            .stream()
            .filter(c -> c.getArquivado() == null || !c.getArquivado())
            .toList();

        aplicarFiltroStatus(statusFiltroAtual);

        long total = listaAtivos.size();

        long apresentados = listaAtivos.stream()
            .filter(c -> c.getStatus() != null && c.getStatus().trim().equalsIgnoreCase("Apresentado"))
            .count();

        long carregando = listaAtivos.stream()
            .filter(c -> c.getStatus() != null && c.getStatus().trim().equalsIgnoreCase("Carregando"))
            .count();

        long expedidos = listaAtivos.stream()
            .filter(c -> c.getStatus() != null && c.getStatus().trim().equalsIgnoreCase("Expedido"))
            .count();

        long pendentes = total - (apresentados + carregando + expedidos);

        double pesoTotal = listaAtivos.stream()
            .mapToDouble(c -> converterPesoParaDouble(c.getPeso()))
            .sum();

        DecimalFormat df = new DecimalFormat("#,##0.00", new DecimalFormatSymbols(Locale.forLanguageTag("pt-BR")));

        txtTotal.setText(String.valueOf(total));
        txtPendentes.setText(String.valueOf(pendentes));
        txtApresentados.setText(String.valueOf(apresentados));
        txtCarregando.setText(String.valueOf(carregando));
        txtExpedidos.setText(String.valueOf(expedidos));
        txtPeso.setText(df.format(pesoTotal) + " kg");
    }

    private double converterPesoParaDouble(String pesoStr) {
        if (pesoStr == null || pesoStr.trim().isEmpty()) return 0.0;
        try {
            String limpo = pesoStr.replaceAll("[^0-9,. ]", "").trim();
            if (limpo.contains(",") && limpo.contains(".")) {
                if (limpo.lastIndexOf(",") > limpo.lastIndexOf(".")) {
                    limpo = limpo.replace(".", "").replace(",", ".");
                } else {
                    limpo = limpo.replace(",", "");
                }
            } else if (limpo.contains(",")) {
                limpo = limpo.replace(",", ".");
            } else if (limpo.contains(".")) {
                int dotIndex = limpo.indexOf(".");
                if (limpo.length() - dotIndex - 1 == 3 && limpo.indexOf(".", dotIndex + 1) == -1) {
                    limpo = limpo.replace(".", "");
                }
            }
            return Double.parseDouble(limpo);
        } catch (Exception e) {
            return 0.0;
        }
    }

    private void estilitarCampoEscuro(com.vaadin.flow.component.Component campo) {
        campo.getElement().getStyle()
            .set("--vaadin-input-field-label-color", "#90caf9")
            .set("--vaadin-input-field-value-color", "#ffffff")
            .set("--vaadin-input-field-background", "#1e293b")
            .set("--lumo-secondary-text-color", "#90caf9")
            .set("--lumo-body-text-color", "#ffffff")
            .set("--lumo-primary-text-color", "#90caf9")
            .set("--lumo-contrast-60pct", "#90caf9")
            .set("--lumo-contrast-70pct", "#90caf9");
    }
}