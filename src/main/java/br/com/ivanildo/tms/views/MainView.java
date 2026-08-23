package br.com.ivanildo.tms.views;

import br.com.ivanildo.tms.model.Carregamento;
import br.com.ivanildo.tms.repository.CarregamentoRepository;
import br.com.ivanildo.tms.service.ExcelService;
import jakarta.annotation.security.PermitAll;
import com.vaadin.flow.component.HasElement;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dependency.CssImport;
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
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import java.util.Locale;



import java.io.InputStream;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;

@Route("")
@PageTitle("Gestão Operacional de Carregamento | TMS")
@CssImport("./styles/dashboard-styles.css")
@PermitAll
@CssImport(value = "./styles/vaadin-grid-custom.css", themeFor = "vaadin-grid")
@CssImport(value = "./styles/vaadin-form-fields-custom.css", themeFor = "vaadin-text-field")
@CssImport(value = "./styles/vaadin-form-fields-custom.css", themeFor = "vaadin-date-picker")
@CssImport(value = "./styles/vaadin-form-fields-custom.css", themeFor = "vaadin-select")
@CssImport(value = "./styles/vaadin-form-fields-custom.css", themeFor = "vaadin-combo-box")
@CssImport(value = "./styles/vaadin-form-fields-custom.css", themeFor = "vaadin-text-area")
@CssImport(value = "./styles/vaadin-dialog-custom.css", themeFor = "vaadin-dialog-overlay")
public class MainView extends VerticalLayout {

    private final CarregamentoRepository repository;
    private final ExcelService excelService;

    private final Grid<Carregamento> grid = new Grid<>(Carregamento.class, false);

    private final Span txtTotal = new Span("0");
    private final Span txtApresentados = new Span("0");
    private final Span txtCarregando = new Span("0");
    private final Span txtExpedidos = new Span("0");
    private final Span txtPeso = new Span("0 kg");
    private final Span txtPendentes = new Span("0");

    public MainView(CarregamentoRepository repository, ExcelService excelService) {
    this.repository = repository;
    this.excelService = excelService;

    setSizeFull();
    setPadding(true);
    setSpacing(true);

    // FIX DEFINITIVO: Define o fundo escuro E força as variáveis do Lumo diretamente no escopo global da View
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
        criarCardSingle("PESO PROGRAMADO", txtPeso, VaadinIcon.SCALE.create(), "peso", "kpi-icon-peso", null) // Sem filtro para Peso
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

        // Se houver um status associado, habilita a interatividade de filtro
        if (statusFiltro != null) {
            card.getStyle().set("cursor", "pointer");
            card.addClickListener(e -> aplicarFiltroStatus(statusFiltro));
        }

        return card;
    } // <-- Fecha o método criarCardSingle aqui!

    private void aplicarFiltroStatus(String status) {
        List<Carregamento> todos = repository.findAll();

        if (status == null) {
            return;
        }

        if ("TODOS".equalsIgnoreCase(status)) {
            grid.setItems(todos);
        } else if ("PENDENTE".equalsIgnoreCase(status) || "PENDENTES".equalsIgnoreCase(status)) {
            List<Carregamento> pendentes = todos.stream()
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
            List<Carregamento> filtrados = todos.stream()
                .filter(c -> c.getStatus() != null && c.getStatus().trim().equalsIgnoreCase(status))
                .toList();
            grid.setItems(filtrados);
        }
    } // Fechamento correto do método aplicarFiltroStatus

 // Fechamento da classe MainView

    private HorizontalLayout criarBarraAcoes() {
        HorizontalLayout layout = new HorizontalLayout();
        layout.setWidthFull();
        layout.setAlignItems(Alignment.CENTER);
        layout.setJustifyContentMode(JustifyContentMode.BETWEEN);

        Button btnNovo = new Button("Novo Carregamento", VaadinIcon.PLUS.create());
        btnNovo.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        btnNovo.getStyle()
                .set("background", "linear-gradient(135deg, #2563eb, #1d4ed8)")
                .set("font-weight", "600")
                .set("border-radius", "6px")
                .set("box-shadow", "0 4px 12px rgba(37, 99, 235, 0.3)");
        btnNovo.addClickListener(e -> abrirFormularioModal(new Carregamento()));
        // Oculta o botão na interface preservando toda a estrutura existente:
        btnNovo.setVisible(false);

        MemoryBuffer buffer = new MemoryBuffer();
Upload uploadExcel = new Upload(buffer);
uploadExcel.setAcceptedFileTypes(".xlsx", ".xls");
uploadExcel.setDropLabel(new Span("Arraste o arquivo Excel (.xlsx) aqui"));
uploadExcel.setUploadButton(new Button("Upload Excel", VaadinIcon.UPLOAD.create()));

uploadExcel.addSucceededListener(event -> {
    try (InputStream is = buffer.getInputStream()) {
        excelService.processarExcel(is);
        Notification n = Notification.show("Planilha importada com sucesso!", 3000, Notification.Position.BOTTOM_END);
        n.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        atualizarGridEIndicators();
    } catch (Exception ex) {
        ex.printStackTrace(); // <--- IMPRIME O ERRO DETALHADO NO LOG DO RENDER
        Notification n = Notification.show("Erro ao processar: " + ex.getMessage(), 5000, Notification.Position.MIDDLE);
        n.addThemeVariants(NotificationVariant.LUMO_ERROR);
    }
});

// <--- ADICIONE ESTE LISTENER LOGO ABAIXO DO SUCCEEDED LISTENER
uploadExcel.addFailedListener(event -> {
    System.err.println("Erro na transferência do arquivo pelo browser: " + event.getReason().getMessage());
});

        layout.add(btnNovo, uploadExcel);
        return layout;
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

    grid.addColumn(Carregamento::getId).setHeader("ID").setAutoWidth(true).setFlexGrow(0);
    grid.addColumn(Carregamento::getDataProgramacao).setHeader("DATA PROG.").setAutoWidth(true);
    grid.addColumn(Carregamento::getTransportadora).setHeader("TRANSPORTADORA").setAutoWidth(true);
    grid.addColumn(Carregamento::getPlaca).setHeader("PLACA").setAutoWidth(true);
    grid.addColumn(Carregamento::getTipoVeiculo).setHeader("TIPO DE VEÍCULO").setAutoWidth(true);
    grid.addColumn(Carregamento::getViagem).setHeader("VIAGEM").setAutoWidth(true);
    grid.addColumn(Carregamento::getOrdemCarga).setHeader("ORDEM DE CARGA").setAutoWidth(true);
    grid.addColumn(Carregamento::getPeso).setHeader("PESO").setAutoWidth(true);
    grid.addColumn(Carregamento::getEncaixe).setHeader("ENCAIXE").setAutoWidth(true);

    grid.addColumn(new ComponentRenderer<>(this::criarBadgeStatus))
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
    private Span criarBadgeStatus(Carregamento c) {
        String statusTxt = c.getStatus() != null && !c.getStatus().isEmpty() ? c.getStatus() : "Pendente";
        Span badge = new Span(statusTxt);

        badge.getStyle()
            .set("padding", "0.3rem 0.75rem")
            .set("border-radius", "20px")
            .set("font-size", "0.75rem")
            .set("font-weight", "700")
            .set("display", "inline-block")
            .set("text-align", "center");

        String status = statusTxt.toUpperCase();
        switch (status) {
            case "APRESENTADO":
                badge.getStyle().set("background-color", "rgba(30, 58, 138, 0.6)").set("color", "#93c5fd").set("border", "1px solid #1e40af");
                break;
            case "CARREGANDO":
                badge.getStyle().set("background-color", "rgba(120, 53, 15, 0.6)").set("color", "#fde047").set("border", "1px solid #854d0e");
                break;
            case "EXPEDIDO":
                badge.getStyle().set("background-color", "rgba(6, 95, 70, 0.6)").set("color", "#6ee7b7").set("border", "1px solid #065f46");
                break;
            default:
                badge.getStyle().set("background-color", "rgba(51, 65, 85, 0.6)").set("color", "#cbd5e1").set("border", "1px solid #475569");
                break;
        }

        return badge;
    }

    private void abrirFormularioModal(Carregamento carregamento) {
    Dialog dialog = new Dialog();
    dialog.setHeaderTitle(carregamento.getId() == null ? "Novo Carregamento" : "Editar Carregamento #" + carregamento.getId());

    // Define as variáveis globais do Dialog para forçar dark mode no cabeçalho e corpo
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

    // Aplica a estilização dos rótulos
    estilizarCampoEscuro(txtData);
    estilizarCampoEscuro(txtTransp);
    estilizarCampoEscuro(txtPlaca);
    estilizarCampoEscuro(txtTipoVeiculo);
    estilizarCampoEscuro(txtViagem);
    estilizarCampoEscuro(txtOrdemCarga);
    estilizarCampoEscuro(txtPeso);
    estilizarCampoEscuro(txtEncaixe);
    estilizarCampoEscuro(cbStatus);
    estilizarCampoEscuro(txtObs);

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
    List<Carregamento> lista = repository.findAll();
    grid.setItems(lista);

    long total = lista.size();

    long apresentados = lista.stream()
        .filter(c -> c.getStatus() != null && c.getStatus().equalsIgnoreCase("Apresentado"))
        .count();

    long carregando = lista.stream()
        .filter(c -> c.getStatus() != null && c.getStatus().equalsIgnoreCase("Carregando"))
        .count();

    long expedidos = lista.stream()
        .filter(c -> c.getStatus() != null && c.getStatus().equalsIgnoreCase("Expedido"))
        .count();

    // Cálculo do Pendentes: Total subtraído dos demais status
    long pendentes = total - (apresentados + carregando + expedidos);

    double pesoTotal = lista.stream()
        .mapToDouble(c -> converterPesoParaDouble(c.getPeso()))
        .sum();

    DecimalFormat df = new DecimalFormat("#,##0.00", new DecimalFormatSymbols(Locale.forLanguageTag("pt-BR")));

    // Atualização dos indicadores na tela:
    txtTotal.setText(String.valueOf(total));
    txtPendentes.setText(String.valueOf(pendentes)); // Adicionado o set do Pendentes
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

    private void estilizarCampoEscuro(com.vaadin.flow.component.Component campo) {
    campo.getElement().getStyle()
        .set("--lumo-secondary-text-color", "#cbd5e1") // Cor do LABEL (Cinza claro/Azul)
        .set("--lumo-body-text-color", "#f8fafc")      // Cor do texto digitado
        .set("--lumo-primary-text-color", "#f8fafc")   // Cor do texto selecionado
        .set("--lumo-contrast-60pct", "#cbd5e1")       // Fallback de contraste do label
        .set("--lumo-contrast-70pct", "#cbd5e1");
}
}


