package br.com.ivanildo.tms.views;

import br.com.ivanildo.tms.model.Entrega;
import br.com.ivanildo.tms.repository.CarregamentoRepository;
import br.com.ivanildo.tms.repository.EntregaRepository;
import br.com.ivanildo.tms.service.PdfService; // 1. IMPORT ADICIONADO
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.StreamResource; // IMPORT PARA O STREAM RESOURCE
import jakarta.annotation.security.PermitAll;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.*;

@Route("entregas")
@PermitAll
public class EntregasView extends VerticalLayout implements HasUrlParameter<Long> {

    private final CarregamentoRepository carregamentoRepository;
    private final EntregaRepository entregaRepository;
    private final PdfService pdfService; // 2. ATRIBUTO ADICIONADO

    private final H3 titulo = new H3("📦 Gestão de Entregas");
    private final Span txtMotorista = new Span("-");
    private final Span txtPlaca = new Span("-");
    private final Span txtViagem = new Span("-");
    private final Span txtTransportadora = new Span("-");
    private final Span txtOrdemCarga = new Span("-");
    private final Span txtTotalNfs = new Span("0");
    private final Span txtTotalEntregas = new Span("0");
    private final Span txtPesoTotal = new Span("0,00 kg");

    private final Grid<ItemGridEntrega> grid = new Grid<>(ItemGridEntrega.class, false);
    private final HorizontalLayout containerBotoesAcao = new HorizontalLayout();

    @SuppressWarnings("null")
    // 3. INJEÇÃO NO CONSTRUTOR
    public EntregasView(CarregamentoRepository carregamentoRepository, 
                        EntregaRepository entregaRepository, 
                        PdfService pdfService) {
        this.carregamentoRepository = carregamentoRepository;
        this.entregaRepository = entregaRepository;
        this.pdfService = pdfService;

        setWidthFull();
        setHeight(null);
        setPadding(true);
        setSpacing(true);

        // Fundo escuro da tela principal
        getStyle()
            .set("background-color", "#0f172a")
            .set("color", "#f8fafc")
            .set("min-height", "100vh");

   // Injeção CSS para Tela (Tema Dark + Formulários) + Impressão (@media print)
UI.getCurrent().getPage().executeJs(
    "var style = document.createElement('style');" +
    "style.innerHTML = '" +
    "  /* VARIÁVEIS DE TEMA DO VAADIN (DARK) */ " +
    "  vaadin-grid { " +
    "    width: 100% !important; " +
    "    background-color: #1e293b !important; " +
    "    border: 1px solid #334155 !important; " +
    "    border-radius: 8px !important; " +
    "  } " +
    "  vaadin-grid::part(cell) { " +
    "    background-color: #1e293b !important; " +
    "    color: #f8fafc !important; " +
    "    border-bottom: 1px solid #334155 !important; " +
    "  } " +
    "  vaadin-grid::part(header-cell) { " +
    "    background-color: #0f172a !important; " +
    "    color: #94a3b8 !important; " +
    "    font-weight: 700 !important; " +
    "    border-bottom: 2px solid #334155 !important; " +
    "  } " +
    "  vaadin-grid-cell-content { " +
    "    color: #f8fafc !important; " +
    "    font-size: 12px !important; " +
    "    white-space: nowrap !important; " +
    "    text-overflow: ellipsis !important; " +
    "    overflow: hidden !important; " +
    "  } " +

    "  /* CORREÇÃO DOS INPUTS E CAMPOS DO FORMULÁRIO DE EDIÇÃO */ " +
    "  vaadin-text-field::part(input-field), " +
    "  vaadin-date-picker::part(input-field), " +
    "  vaadin-select::part(input-field), " +
    "  vaadin-combo-box::part(input-field), " +
    "  vaadin-text-area::part(input-field) { " +
    "    background-color: #1e293b !important; " + // Fundo escuro combinando com a interface
    "    border: 1px solid #334155 !important; " +
    "    border-radius: 6px !important; " +
    "  } " +
    "  vaadin-text-field input, " +
    "  vaadin-date-picker input, " +
    "  vaadin-select input, " +
    "  vaadin-combo-box input, " +
    "  vaadin-text-area textarea { " +
    "    color: #ffffff !important; " + // Texto digitado totalmente branco e legível
    "    font-weight: 600 !important; " +
    "  } " +
    "  vaadin-text-field [part=\"label\"], " +
    "  vaadin-date-picker [part=\"label\"], " +
    "  vaadin-select [part=\"label\"], " +
    "  vaadin-combo-box [part=\"label\"], " +
    "  vaadin-text-area [part=\"label\"] { " +
    "    color: #94a3b8 !important; " + // Rótulo do campo em cinza claro
    "    font-weight: 600 !important; " +
    "  } " +

    "  /* LINHAS DE SUBTOTAL */ " +
    "  .subtotal-row::part(cell) { " +
    "    background-color: #334155 !important; " +
    "  } " +
    "  .subtotal-row vaadin-grid-cell-content { " +
    "    color: #38bdf8 !important; " +
    "    font-weight: bold !important; " +
    "  } " +

    "  /* REGRAS DE IMPRESSÃO */ " +
    "  @media print { " +
    "    @page { size: landscape; margin: 5mm; } " +
    "    .no-print { display: none !important; } " +
    "    body { background-color: #ffffff !important; color: #000000 !important; } " +
    "    vaadin-grid { width: 100% !important; height: auto !important; max-height: none !important; overflow: visible !important; border: none !important; } " +
    "    vaadin-grid, vaadin-grid *, vaadin-grid::part(row), vaadin-grid::part(cell) { " +
    "      transform: none !important; " +
    "      position: static !important; " +
    "      background-color: #ffffff !important; " +
    "      color: #000000 !important; " +
    "    } " +
    "    vaadin-grid-cell-content { " +
    "      padding: 3px 4px !important; " +
    "      font-size: 9px !important; " +
    "      color: #000000 !important; " +
    "    } " +
    "    .subtotal-row::part(cell) { " +
    "      background-color: #e5e7eb !important; " +
    "    } " +
    "    .subtotal-row vaadin-grid-cell-content { color: #000000 !important; font-weight: bold !important; } " +
    "  } " +
    "';" +
    "document.head.appendChild(style);"
);
        // Botão Voltar
        Button btnVoltar = new Button("← Voltar para Carregamentos", e -> UI.getCurrent().navigate(""));
        btnVoltar.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        btnVoltar.addClassName("no-print");

        // Botão Imprimir (Corrigido o contraste para não ficar ofuscado)
        Button btnImprimir = new Button("🖨️ Imprimir / Visualizar", e -> UI.getCurrent().getPage().executeJs("window.print();"));
        btnImprimir.getStyle()
                .set("background-color", "#334155")
                .set("color", "#ffffff")
                .set("border", "1px solid #475569")
                .set("font-weight", "600")
                .set("cursor", "pointer");
        btnImprimir.addClassName("no-print");

        containerBotoesAcao.addClassName("no-print");

        titulo.getStyle().set("color", "#f8fafc").set("margin", "0");

        HorizontalLayout topo = new HorizontalLayout(btnVoltar, titulo, containerBotoesAcao, btnImprimir);
        topo.setWidthFull();
        topo.setAlignItems(Alignment.CENTER);
        topo.setFlexGrow(1, titulo);

        // Cards Superiores (Incluindo TOTAL NF's e ENTREGAS)
        HorizontalLayout cardsLayout = new HorizontalLayout(
                criarCard("MOTORISTA", txtMotorista, "#06b6d4"),
                criarCard("PLACA", txtPlaca, "#f59e0b"),
                criarCard("VIAGEM", txtViagem, "#10b981"),
                criarCard("TRANSPORTADORA", txtTransportadora, "#8b5cf6"),
                criarCard("ORDEM DE CARGA", txtOrdemCarga, "#ef4444"),
                criarCard("TOTAL NF'S", txtTotalNfs, "#3b82f6"),
                criarCard("ENTREGAS", txtTotalEntregas, "#a855f7"),
                criarCard("PESO TOTAL", txtPesoTotal, "#10b981", true)
        );
        cardsLayout.setWidthFull();

        // Configuração das Colunas do Grid
        grid.addColumn(ItemGridEntrega::getDelivery)
            .setHeader("DELIVERY")
            .setWidth("130px")
            .setFlexGrow(0);

        grid.addColumn(ItemGridEntrega::getNf)
            .setHeader("NOTA FISCAL")
            .setWidth("110px")
            .setFlexGrow(0);

        grid.addColumn(ItemGridEntrega::getCliente)
            .setHeader("CLIENTE")
            .setWidth("200px")
            .setFlexGrow(3);

        grid.addColumn(ItemGridEntrega::getBairro)
            .setHeader("BAIRRO")
            .setWidth("130px")
            .setFlexGrow(2);

        grid.addColumn(ItemGridEntrega::getCidade)
            .setHeader("CIDADE")
            .setWidth("130px")
            .setFlexGrow(2);

        grid.addColumn(ItemGridEntrega::getPeso)
            .setHeader("PESO (KG)")
            .setTextAlign(ColumnTextAlign.END)
            .setWidth("110px")
            .setFlexGrow(0);

        grid.setClassNameGenerator(item -> item.isSubtotal() ? "subtotal-row" : null);

        grid.setWidthFull();
        grid.setAllRowsVisible(true);

        add(topo, cardsLayout, grid);
    }

    @Override
    public void setParameter(BeforeEvent event, Long carregamentoId) {
        if (carregamentoId != null) {
            carregamentoRepository.findById(carregamentoId).ifPresent(carregamento -> {
                titulo.setText("📦 Gestão de Entregas - Carregamento #" + carregamento.getId());
                txtMotorista.setText(carregamento.getMotorista() != null ? carregamento.getMotorista() : "Não informado");
                txtPlaca.setText(carregamento.getPlaca() != null ? carregamento.getPlaca() : "Não informada");
                txtViagem.setText(carregamento.getViagem() != null ? carregamento.getViagem() : "-");
                txtTransportadora.setText(carregamento.getTransportadora() != null ? carregamento.getTransportadora() : "-");
                txtOrdemCarga.setText(carregamento.getOrdemCarga() != null ? carregamento.getOrdemCarga() : "-");

                containerBotoesAcao.removeAll();
                Button pdfButton = new Button("📄 Baixar PDF");
                pdfButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_ERROR);

                // Define o StreamResource diretamente no Vaadin
StreamResource resource = new StreamResource("carregamento_" + carregamento.getId() + ".pdf", () -> {
    try {
        List<Entrega> entregas = entregaRepository.findByCarregamentoId(carregamento.getId());
        byte[] pdfBytes = pdfService.gerarPdfCarregamento(carregamento, entregas);

        if (pdfBytes == null || pdfBytes.length == 0) {
            System.err.println("⚠ PDF gerado com byte array vazio/nulo.");
            return new ByteArrayInputStream(new byte[0]);
        }

        return new ByteArrayInputStream(pdfBytes);
    } catch (Exception e) {
        System.err.println("❌ Erro durante a geração do PDF no StreamResource:");
        e.printStackTrace();
        return new ByteArrayInputStream(new byte[0]);
    }
});

// Garanta a especificação do MIME Type de PDF
resource.setContentType("application/pdf");
resource.setCacheTime(0);

Anchor btnPdf = new Anchor(resource, "");
btnPdf.getElement().setAttribute("download", true);
btnPdf.add(pdfButton);
            });

            List<Entrega> entregas = entregaRepository.findByCarregamentoId(carregamentoId);
            List<ItemGridEntrega> itensExibicao = processarEntregasComSubtotais(entregas);

            grid.setItems(itensExibicao);
        }
    }

    private List<ItemGridEntrega> processarEntregasComSubtotais(List<Entrega> entregas) {
        List<ItemGridEntrega> lista = new ArrayList<>();
        if (entregas.isEmpty()) {
            txtPesoTotal.setText("0,00 kg");
            txtTotalNfs.setText("0");
            txtTotalEntregas.setText("0");
            return lista;
        }

        Map<String, List<Entrega>> entregasPorGrupo = new LinkedHashMap<>();
        double pesoGeral = 0.0;

        for (Entrega e : entregas) {
            String nome = (e.getCliente() != null && !e.getCliente().trim().isEmpty()) ? e.getCliente().trim() : "OUTROS";
            String cidade = (e.getCidade() != null && !e.getCidade().trim().isEmpty()) ? e.getCidade().trim() : "";
            String bairro = (e.getBairro() != null && !e.getBairro().trim().isEmpty()) ? e.getBairro().trim() : "";
            String cnpjLimpo = normalizarCnpjCodigo(e.getCodigoCliente());

            String chaveGrupo = nome.toLowerCase() + "||" + cidade.toLowerCase() + "||" + bairro.toLowerCase() + "||" + cnpjLimpo;

            entregasPorGrupo.computeIfAbsent(chaveGrupo, k -> new ArrayList<>()).add(e);
            pesoGeral += converterPesoParaDouble(e.getPeso());
        }

        DecimalFormat df = new DecimalFormat("#,##0.00", new DecimalFormatSymbols(Locale.forLanguageTag("pt-BR")));
        
        // Atualiza os contadores gerais dos cards
        txtPesoTotal.setText(df.format(pesoGeral) + " kg");
        txtTotalNfs.setText(String.valueOf(entregas.size()));
        txtTotalEntregas.setText(String.valueOf(entregasPorGrupo.size())); // Cada grupo/cliente = 1 entrega

        for (Map.Entry<String, List<Entrega>> entry : entregasPorGrupo.entrySet()) {
            List<Entrega> listaDoGrupo = entry.getValue();
            double pesoSubtotalGrupo = 0.0;
            String nomeExibicao = "";

            for (Entrega ent : listaDoGrupo) {
                pesoSubtotalGrupo += converterPesoParaDouble(ent.getPeso());
                if (nomeExibicao.isEmpty() && ent.getCliente() != null) {
                    nomeExibicao = ent.getCliente().trim();
                }

                ItemGridEntrega item = new ItemGridEntrega();
                item.setDelivery(ent.getDelivery());
                item.setNf(ent.getNf());
                item.setCliente(ent.getCliente() != null ? ent.getCliente() : "-");
                item.setBairro(ent.getBairro());
                item.setCidade(ent.getCidade());
                item.setPeso(ent.getPeso());
                item.setSubtotal(false);

                lista.add(item);
            }

            ItemGridEntrega subtotalItem = new ItemGridEntrega();
            subtotalItem.setDelivery("---");
            subtotalItem.setNf("SUBTOTAL CLIENTE");
            subtotalItem.setCliente("SUBTOTAL (" + nomeExibicao + ") - " + listaDoGrupo.size() + " entrega(s)");
            subtotalItem.setBairro("---");
            subtotalItem.setCidade("---");
            subtotalItem.setPeso(df.format(pesoSubtotalGrupo) + " kg");
            subtotalItem.setSubtotal(true);

            lista.add(subtotalItem);
        }

        return lista;
    }

    private String normalizarCnpjCodigo(String raw) {
        if (raw == null || raw.trim().isEmpty()) return "";
        try {
            String temp = raw.trim().replace(",", ".");
            if (temp.toLowerCase().contains("e")) {
                BigDecimal bd = new BigDecimal(temp);
                return bd.toPlainString().replaceAll("\\.0+$", "");
            }
            return raw.trim();
        } catch (Exception e) {
            return raw.trim();
        }
    }

    private double converterPesoParaDouble(String pesoStr) {
    if (pesoStr == null || pesoStr.trim().isEmpty()) return 0.0;
    try {
        // Mantém apenas dígitos, vírgula e ponto
        String limpo = pesoStr.replaceAll("[^0-9,. ]", "").trim();

        // Se tem ponto e vírgula (ex: "2.577,68" ou "2,577.68")
        if (limpo.contains(",") && limpo.contains(".")) {
            if (limpo.lastIndexOf(",") > limpo.lastIndexOf(".")) {
                limpo = limpo.replace(".", "").replace(",", ".");
            } else {
                limpo = limpo.replace(",", "");
            }
        } 
        // Se tem apenas vírgula (ex: "2577,68")
        else if (limpo.contains(",")) {
            limpo = limpo.replace(",", ".");
        } 
        // Se tem apenas ponto (ex: "2577.679" do Excel) -> O ponto É O DECIMAL, não deve ser removido!

        return Double.parseDouble(limpo);
    } catch (Exception e) {
        return 0.0;
    }
}
    private Div criarCard(String titulo, Span valor, String corBorda) {
        return criarCard(titulo, valor, corBorda, false);
    }

    private Div criarCard(String titulo, Span valor, String corBorda, boolean destaque) {
        Span lblTitulo = new Span(titulo);
        lblTitulo.getStyle().set("font-size", "0.7rem").set("color", "#9ca3af").set("font-weight", "bold");

        valor.getStyle()
                .set("font-size", destaque ? "1.15rem" : "0.95rem")
                .set("font-weight", "bold")
                .set("color", destaque ? "#10b981" : "#ffffff");

        VerticalLayout cardBody = new VerticalLayout(lblTitulo, valor);
        cardBody.setPadding(false);
        cardBody.setSpacing(false);

        Div card = new Div(cardBody);
        card.getStyle()
                .set("background-color", "#1e293b")
                .set("padding", "12px 16px")
                .set("border-radius", "8px")
                .set("border-left", "4px solid " + corBorda)
                .set("width", "100%");
        return card;
    }

    public static class ItemGridEntrega {
        private String delivery;
        private String nf;
        private String cliente;
        private String bairro;
        private String cidade;
        private String peso;
        private boolean subtotal;

        public String getDelivery() { return delivery; }
        public void setDelivery(String delivery) { this.delivery = delivery; }

        public String getNf() { return nf; }
        public void setNf(String nf) { this.nf = nf; }

        public String getCliente() { return cliente; }
        public void setCliente(String cliente) { this.cliente = cliente; }

        public String getBairro() { return bairro; }
        public void setBairro(String bairro) { this.bairro = bairro; }

        public String getCidade() { return cidade; }
        public void setCidade(String cidade) { this.cidade = cidade; }

        public String getPeso() { return peso; }
        public void setPeso(String peso) { this.peso = peso; }

        public boolean isSubtotal() { return subtotal; }
        public void setSubtotal(boolean subtotal) { this.subtotal = subtotal; }
    }
}