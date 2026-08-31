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
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
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

@Route("")
@PageTitle("Gestão Operacional de Carregamento | TMS")
@PermitAll
public class MainView extends VerticalLayout implements BeforeEnterObserver {

    private final CarregamentoRepository repository;
    private final EntregaRepository entregaRepository;
    private final ExcelService excelService;

    private final Grid<Carregamento> grid = new Grid<>(Carregamento.class, false);
    private final Map<Carregamento, Checkbox> mapaCheckboxesMain = new HashMap<>();

    private final Span txtTotal = new Span("0");
    private final Span txtApresentados = new Span("0");
    private final Span txtCarregando = new Span("0");
    private final Span txtExpedidos = new Span("0");
    private final Span txtPeso = new Span("0 kg");
    private final Span txtPendentes = new Span("0");

    private UiBroadcaster.Registration broadcasterRegistration;

    public MainView(CarregamentoRepository repository, EntregaRepository entregaRepository, ExcelService excelService) {
        this.repository = repository;
        this.entregaRepository = entregaRepository;
        this.excelService = excelService;

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

        H2 titulo = new H2("🚚 Gestão Operacional de Carregamento");
        titulo.getStyle()
                .set("color", "#ffffff")
                .set("margin", "0")
                .set("font-size", "1.6rem")
                .set("font-weight", "700");

        Div containerKPI = criarCardsKPIs();
        HorizontalLayout barraAcoes = criarBarraAcoes();
        configurarGrid();

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
                atualizarGridEIndicators();
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
            card.addClickListener(e -> aplicarFiltroStatus(statusFiltro));
        }

        return card;
    }

    private void aplicarFiltroStatus(String status) {
        List<Carregamento> todosAtivos = repository.findAll().stream()
            .filter(c -> c.getArquivado() == null || !c.getArquivado())
            .toList();

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

        Button btnNovo = new Button("Novo Carregamento", VaadinIcon.PLUS.create());
        btnNovo.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        btnNovo.getStyle()
                .set("background", "linear-gradient(135deg, #2563eb, #1d4ed8)")
                .set("font-weight", "600")
                .set("border-radius", "6px")
                .set("box-shadow", "0 4px 12px rgba(37, 99, 235, 0.3)");
        btnNovo.addClickListener(e -> abrirFormularioModal(new Carregamento()));
        btnNovo.setVisible(false);

        Button btnArquivarExpedidas = new Button("Arquivar Expedidas", VaadinIcon.ARCHIVE.create());
        btnArquivarExpedidas.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);
        btnArquivarExpedidas.getStyle()
                .set("background", "linear-gradient(135deg, #059669, #047857)")
                .set("color", "#ffffff")
                .set("font-weight", "600");

     
        btnArquivarExpedidas.addClickListener(e -> {
            List<Carregamento> expedidosAtivos = repository.findAll().stream()
                .filter(c -> (c.getArquivado() == null || !c.getArquivado()) && 
                             c.getStatus() != null && 
                             c.getStatus().equalsIgnoreCase("Expedido"))
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
            Notification.show(expedidosAtivos.size() + " cargas expedidas foram arquivadas.", 3000, Notification.Position.BOTTOM_END);
        });

        Button btnExcluirSelecionadas = new Button("Excluir Selecionadas", VaadinIcon.TRASH.create());
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
                
                Notification.show(selecionados.size() + " carga(s) excluída(s) com sucesso!", 3000, Notification.Position.BOTTOM_END);
            } catch (Exception ex) {
                Notification.show("Erro ao excluir: " + ex.getMessage(), 5000, Notification.Position.MIDDLE);
            }
        }); 
        
        Button btnLimparCheckin = new Button("Limpar Checkin", VaadinIcon.REFRESH.create());
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
            
            Notification.show("Checkin e motorista limpos para " + selecionados.size() + " carga(s)!", 3000, Notification.Position.BOTTOM_END);
        });

        Button btnVerArquivados = new Button("Ver Arquivados", VaadinIcon.FOLDER_OPEN.create());
        btnVerArquivados.addThemeVariants(ButtonVariant.LUMO_SMALL);
        btnVerArquivados.addClickListener(e -> abrirModalArquivados());

        Button btnVerFila = new Button("Ver Fila", VaadinIcon.LIST.create());
        btnVerFila.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);
        btnVerFila.getStyle()
                .set("background", "linear-gradient(135deg, #3b82f6, #1d4ed8)")
                .set("color", "#ffffff")
                .set("font-weight", "600");
        btnVerFila.addClickListener(e -> abrirModalFila());

        Button btnRelatorioPaletes = new Button("Relatório Paletes", VaadinIcon.PRINT.create());
        btnRelatorioPaletes.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);
        btnRelatorioPaletes.getStyle()
                .set("background", "linear-gradient(135deg, #059669, #047857)")
                .set("color", "#ffffff")
                .set("font-weight", "600");
        btnRelatorioPaletes.addClickListener(e -> UI.getCurrent().navigate(RelatorioPaletesView.class));

        grupoEsquerda.add(btnNovo, btnArquivarExpedidas, btnExcluirSelecionadas, btnLimparCheckin, btnVerArquivados, btnVerFila, btnRelatorioPaletes);
        MemoryBuffer buffer = new MemoryBuffer();
        Upload uploadExcel = new Upload(buffer);
        uploadExcel.setAcceptedFileTypes(".xlsx", ".xls");
        uploadExcel.setDropLabel(new Span("Arraste o arquivo Excel (.xlsx) aqui"));
        uploadExcel.setUploadButton(new Button("Upload Excel", VaadinIcon.UPLOAD.create()));

        uploadExcel.addSucceededListener(event -> {
            try {
                InputStream is = buffer.getInputStream();
                excelService.processarExcel(is);

                getUI().ifPresent(ui -> ui.access(() -> {
                    atualizarGridEIndicators();
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

    gridArquivados.addColumn(c -> c.getHoraChegada() != null ? c.getHoraChegada().format(formatter) : "-")
        .setHeader("CHEGADA").setAutoWidth(true);

    gridArquivados.addColumn(c -> c.getHoraInicioCarregamento() != null ? c.getHoraInicioCarregamento().format(formatter) : "-")
        .setHeader("INÍCIO CARGA").setAutoWidth(true);

    gridArquivados.addColumn(c -> c.getHoraFimCarregamento() != null ? c.getHoraFimCarregamento().format(formatter) : "-")
        .setHeader("FIM CARGA").setAutoWidth(true);

        gridArquivados.addColumn(Carregamento::getTempoEsperaFilaFormatado).setHeader("ESPERA FILA").setAutoWidth(true);
    gridArquivados.addColumn(Carregamento::getTempoCarregamentoFormatado).setHeader("TEMPO CARGA").setAutoWidth(true);
    gridArquivados.addColumn(Carregamento::getLeadTimeTotalFormatado).setHeader("LEAD TIME TOTAL").setAutoWidth(true);

        gridArquivados.addColumn(new ComponentRenderer<>(carregamento -> {
            Button btnDesarquivar = new Button("Desarquivar", VaadinIcon.UPLOAD_ALT.create());
            btnDesarquivar.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_SUCCESS);
            btnDesarquivar.addClickListener(e -> {
                carregamento.setArquivado(false);
                repository.save(carregamento);
                

                // Adicione estas colunas no gridArquivados dentro do seu método abrirModalArquivados()
    
                   
                mapaCheckboxesArquivados.clear();
                List<Carregamento> listaArquivados = repository.findAll().stream()
                    .filter(c -> c.getArquivado() != null && c.getArquivado())
                    .toList();
                gridArquivados.setItems(listaArquivados);
                
                atualizarGridEIndicators();
                Notification.show("Viagem desarquivada com sucesso!", 3000, Notification.Position.BOTTOM_END);
            });
            return btnDesarquivar;
        })).setHeader("AÇÃO").setAutoWidth(true);

        List<Carregamento> listaArquivados = repository.findAll().stream()
            .filter(c -> c.getArquivado() != null && c.getArquivado())
            .toList();
        gridArquivados.setItems(listaArquivados);

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
            List<Carregamento> novaListaArquivados = repository.findAll().stream()
                .filter(c -> c.getArquivado() != null && c.getArquivado())
                .toList();
            gridArquivados.setItems(novaListaArquivados);

            atualizarGridEIndicators();
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
        if (c.getHoraChegada() != null) {
            return c.getHoraChegada().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
        }
        return "-";
    }).setHeader("HORA CHEGADA").setAutoWidth(true);

    gridFila.addColumn(Carregamento::getStatus).setHeader("STATUS").setAutoWidth(true);

    List<Carregamento> listaFila = repository.findAll().stream()
        .filter(c -> (c.getArquivado() == null || !c.getArquivado()) &&
                     c.getStatus() != null && 
                     c.getStatus().trim().equalsIgnoreCase("Apresentado"))
        .sorted((c1, c2) -> {
            if (c1.getHoraChegada() == null) return 1;
            if (c2.getHoraChegada() == null) return -1;
            return c1.getHoraChegada().compareTo(c2.getHoraChegada());
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
        grid.addColumn(Carregamento::getPlaca).setHeader("PLACA").setAutoWidth(true);

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
            if (carregamento.getHoraChegada() == null) {
                carregamento.setHoraChegada(LocalDateTime.now());
            }
            repository.save(carregamento);
            atualizarGridEIndicators();
            UiBroadcaster.broadcast("STATUS_ATUALIZADO");
        });

        btnCarregando.addClickListener(e -> {
            carregamento.setStatus("Carregando");
            carregamento.setHoraInicioCarregamento(LocalDateTime.now());
            repository.save(carregamento);
            atualizarGridEIndicators();
            UiBroadcaster.broadcast("STATUS_ATUALIZADO");
        });

        btnExpedido.addClickListener(e -> {
            carregamento.setStatus("Expedido");
            carregamento.setHoraFimCarregamento(LocalDateTime.now());
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

        TextField txtPlaca = new TextField("Placa");
        txtPlaca.setValue(carregamento.getPlaca() != null ? carregamento.getPlaca() : "");

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

        ComboBox<String> cbStatus = new ComboBox<>("Status");
        cbStatus.setItems("Pendente", "Apresentado", "Carregando", "Expedido");
        cbStatus.setValue(carregamento.getStatus() != null ? carregamento.getStatus() : "Pendente");

        TextField txtObs = new TextField("Observação");
        txtObs.setValue(carregamento.getObservacao() != null ? carregamento.getObservacao() : "");

        estilitarCampoEscuro(txtData);
        estilitarCampoEscuro(txtTransp);
        estilitarCampoEscuro(txtPlaca);
        estilitarCampoEscuro(txtTipoVeiculo);
        estilitarCampoEscuro(txtViagem);
        estilitarCampoEscuro(txtOrdemCarga);
        estilitarCampoEscuro(txtPeso);
        estilitarCampoEscuro(txtEncaixe);
        estilitarCampoEscuro(cbStatus);
        estilitarCampoEscuro(txtObs);

        form.add(txtData, txtTransp, txtPlaca, txtTipoVeiculo, txtViagem, txtOrdemCarga, txtPeso, txtEncaixe, cbStatus, txtObs);
        dialog.add(form);

        Button btnSalvar = new Button("Salvar", e -> {
            carregamento.setDataProgramacao(txtData.getValue());
            carregamento.setTransportadora(txtTransp.getValue());
            carregamento.setPlaca(txtPlaca.getValue());
            carregamento.setTipoVeiculo(txtTipoVeiculo.getValue());
            carregamento.setViagem(txtViagem.getValue());
            carregamento.setOrdemCarga(txtOrdemCarga.getValue());
            carregamento.setPeso(txtPeso.getValue());
            carregamento.setEncaixe(txtEncaixe.getValue());
            carregamento.setStatus(cbStatus.getValue());
            carregamento.setObservacao(txtObs.getValue());

            repository.save(carregamento);
            UiBroadcaster.broadcast("CARREGAMENTO_ATUALIZADO");

            Notification.show("Carregamento salvo com sucesso!", 3000, Notification.Position.TOP_END)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            atualizarGridEIndicators();
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

        grid.setItems(listaAtivos);

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