package br.com.ivanildo.tms.views;

import br.com.ivanildo.tms.model.Carregamento;
import br.com.ivanildo.tms.model.Entrega;
import br.com.ivanildo.tms.repository.CarregamentoRepository;
import br.com.ivanildo.tms.repository.EntregaRepository;
import jakarta.annotation.security.PermitAll;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Route(value = "relatorio-paletes")
@PageTitle("Relatório de Paletes | TMS")
@PermitAll
public class RelatorioPaletesView extends VerticalLayout {

    private final CarregamentoRepository repository;
    private final EntregaRepository entregaRepository;
    private Grid<Carregamento> grid;
    private List<Carregamento> itensAtuais = new ArrayList<>();
    private final Map<Carregamento, Checkbox> mapaCheckboxes = new HashMap<>();

    @SuppressWarnings("null")
    public RelatorioPaletesView(CarregamentoRepository repository, EntregaRepository entregaRepository) {
        this.repository = repository;
        this.entregaRepository = entregaRepository;
        setSizeFull();
        setPadding(true);

        H3 titulo = new H3("Seleção de Viagens para Relatório de Paletes");
        
        Button btnVoltar = new Button("Voltar ao Sistema", e -> UI.getCurrent().navigate(MainView.class));
        btnVoltar.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        Button btnImprimir = new Button("Gerar Relatório A4", e -> gerarRelatorioSelecionados());
        btnImprimir.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        btnImprimir.getStyle().set("background", "linear-gradient(135deg, #059669, #047857)");

        HorizontalLayout topoBotoes = new HorizontalLayout(btnVoltar, btnImprimir);
        topoBotoes.setAlignItems(Alignment.CENTER);

        HorizontalLayout topo = new HorizontalLayout(titulo, topoBotoes);
        topo.setWidthFull();
        topo.setJustifyContentMode(JustifyContentMode.BETWEEN);
        topo.setAlignItems(Alignment.CENTER);

        grid = new Grid<>(Carregamento.class, false);
        grid.setSizeFull();

        mapaCheckboxes.clear();

        // Checkbox Mestre no Cabeçalho
        Checkbox masterCheckbox = new Checkbox();
        masterCheckbox.setValue(true); // Começa marcado por padrão
        masterCheckbox.addValueChangeListener(event -> {
            boolean masterValue = event.getValue();
            for (Checkbox cb : mapaCheckboxes.values()) {
                cb.setValue(masterValue);
            }
        });

        // Coluna com o Checkbox mestre no cabeçalho
        grid.addComponentColumn(carregamento -> {
            Checkbox checkbox = new Checkbox();
            checkbox.setValue(true); // Selecionado por padrão
            mapaCheckboxes.put(carregamento, checkbox);
            return checkbox;
        }).setHeader(masterCheckbox).setWidth("110px").setFlexGrow(0);

        grid.addColumn(Carregamento::getDataProgramacao).setHeader("Data Operação");
        grid.addColumn(Carregamento::getViagem).setHeader("Nº Viagem");
        grid.addColumn(Carregamento::getTransportadora).setHeader("Transportadora");
        grid.addColumn(Carregamento::getPlaca).setHeader("Placa");
        grid.addColumn(Carregamento::getTipoVeiculo).setHeader("Perfil Veículo");
        grid.addColumn(Carregamento::getPaletes).setHeader("Paletes");

        atualizarGrid();

        add(topo, grid);
    }

    private void atualizarGrid() {
        mapaCheckboxes.clear();
        itensAtuais = repository.findAll().stream()
                .filter(c -> c.getPaletes() != null && c.getPaletes() > 0)
                .filter(c -> c.getTipoVeiculo() == null || !c.getTipoVeiculo().trim().equalsIgnoreCase("HR"))
                // Filtra para trazer APENAS cargas da programação (ignorando arquivadas)
                .filter(c -> c.getArquivado() == null || !c.getArquivado()) // Ajuste para 'getArquivado' ou o nome do campo booleano de arquivamento na sua entidade Carregamento
                .collect(Collectors.toList());

        grid.setItems(itensAtuais);
    }

    private void gerarRelatorioSelecionados() {
        List<Carregamento> selecionados = itensAtuais.stream()
                .filter(c -> mapaCheckboxes.containsKey(c) && mapaCheckboxes.get(c).getValue())
                .collect(Collectors.toList());

        if (selecionados.isEmpty()) {
            Notification.show("Nenhuma viagem selecionada para impressão.", 3000, Notification.Position.MIDDLE);
            return;
        }

        StringBuilder htmlBuilder = new StringBuilder();
        htmlBuilder.append("<!DOCTYPE html><html><head><meta charset='UTF-8'><title>Relatório de Paletes</title>");
        htmlBuilder.append("<style>");
        htmlBuilder.append("@page { size: A4; margin: 8mm; }");
        htmlBuilder.append("body { font-family: Arial, sans-serif; font-size: 10px; color: #000; margin: 0; background: #fff; }");
        
        // Garante que cada bloco de carregamento ocupe exatamente uma página e o espaço total da folha
        htmlBuilder.append(".carregamento-pagina { page-break-after: always; break-after: page; height: 100vh; display: flex; flex-direction: column; justify-content: space-between; box-sizing: border-box; }");
        
        htmlBuilder.append(".via-container { border: 1px solid #000; margin-bottom: 6px; background: #fff; flex: 1; display: flex; flex-direction: column; justify-content: space-between; }");
        htmlBuilder.append(".header-bar { background: #333; color: #fff; padding: 4px 8mm; font-weight: bold; display: flex; justify-content: space-between; font-size: 11px; }");
        htmlBuilder.append("table { width: 100%; border-collapse: collapse; margin-top: 0; }");
        htmlBuilder.append("th, td { border: 1px solid #000; padding: 3px 5px; text-align: left; font-size: 10px; }");
        htmlBuilder.append("th { background: #f2f2f2; font-weight: bold; }");
        htmlBuilder.append(".center { text-align: center; }");
        htmlBuilder.append(".obs-box { border: 1px solid #000; border-top: none; padding: 4px; min-height: 20px; font-size: 9px; }");
        htmlBuilder.append(".assinaturas { display: flex; border-top: 1px solid #000; }");
        htmlBuilder.append(".assinatura-box { flex: 1; border-right: 1px solid #000; padding: 4px; height: 30px; position: relative; }");
        htmlBuilder.append(".assinatura-box:last-child { border-right: none; }");
        htmlBuilder.append(".assinatura-label { font-weight: bold; font-size: 9px; }");
        htmlBuilder.append(".assinatura-linha { position: absolute; bottom: 4px; font-size: 8px; color: #555; }");
        htmlBuilder.append("@media print { .no-print { display: none; } }");
        htmlBuilder.append("</style></head><body>");

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        String dataEmissao = LocalDateTime.now().format(dtf);

        for (Carregamento c : selecionados) {
            // Regra do Cliente: Se houver mais de 1 cliente distinto nas entregas -> "DIVERSOS", se houver 1 -> Nome do cliente, senão -> "-"
            List<Entrega> entregas = entregaRepository.findByCarregamentoId(c.getId());
            Set<String> clientesDistintos = entregas.stream()
                    .map(e -> e.getCliente())
                    .filter(cli -> cli != null && !cli.trim().isEmpty())
                    .collect(Collectors.toSet());

            String nomeClienteFinal;
            if (clientesDistintos.size() > 1) {
                nomeClienteFinal = "DIVERSOS";
            } else if (clientesDistintos.size() == 1) {
                nomeClienteFinal = clientesDistintos.iterator().next();
            } else {
                nomeClienteFinal = c.getTransportadora() != null ? c.getTransportadora() : "-";
            }

            String strPedidos = c.getOrdemCarga() != null ? c.getOrdemCarga() : "-";
            String strDestino = "-"; 
            int qtdPaletes = c.getPaletes() != null ? c.getPaletes() : 0;

            String[] tiposVias = {"VIA GUARDA", "VIA MOTORISTA", "VIA EMERGENT"};

            // Agrupa as 3 vias de um mesmo carregamento dentro de uma página dedicada
            htmlBuilder.append("<div class='carregamento-pagina'>");

            for (String tipoVia : tiposVias) {
                htmlBuilder.append("<div class='via-container'>");
                
                htmlBuilder.append("<div class='header-bar'>");
                htmlBuilder.append("<span>RELATÓRIO DE PALETES &mdash; TMS Logistics &ndash; Controle de Expedição</span>");
                htmlBuilder.append("<span>").append(tipoVia).append("</span>");
                htmlBuilder.append("</div>");

                htmlBuilder.append("<table>");
                htmlBuilder.append("<tr>");
                htmlBuilder.append("<th style='width: 30%;'>Nº VIAGEM</th>");
                htmlBuilder.append("<th style='width: 40%;'>TRANSPORTADORA</th>");
                htmlBuilder.append("<th style='width: 15%;'>PLACA</th>");
                htmlBuilder.append("<th style='width: 15%;'>DATA OPERAÇÃO</th>");
                htmlBuilder.append("</tr>");
                htmlBuilder.append("<tr>");
                htmlBuilder.append("<td><b>").append(c.getViagem() != null ? c.getViagem() : "-").append("</b></td>");
                htmlBuilder.append("<td>").append(c.getTransportadora() != null ? c.getTransportadora() : "-").append("</td>");
                htmlBuilder.append("<td><b>").append(c.getPlaca() != null ? c.getPlaca() : "-").append("</b></td>");
                htmlBuilder.append("<td>").append(c.getDataProgramacao() != null ? c.getDataProgramacao() : "-").append("</td>");
                htmlBuilder.append("</tr>");
                htmlBuilder.append("</table>");

                htmlBuilder.append("<table style='border-top: none;'>");
                htmlBuilder.append("<tr>");
                htmlBuilder.append("<th style='width: 50%;'>CARREGAMENTO</th>");
                htmlBuilder.append("<th style='width: 30%;'>PERFIL VEÍCULO</th>");
                htmlBuilder.append("<th style='width: 20%;'>EMISSÃO</th>");
                htmlBuilder.append("</tr>");
                htmlBuilder.append("<tr>");
                htmlBuilder.append("<td>&mdash;</td>");
                htmlBuilder.append("<td>").append(c.getTipoVeiculo() != null ? c.getTipoVeiculo() : "-").append("</td>");
                htmlBuilder.append("<td>").append(dataEmissao).append("</td>");
                htmlBuilder.append("</tr>");
                htmlBuilder.append("</table>");

                htmlBuilder.append("<table style='border-top: none;'>");
                htmlBuilder.append("<tr>");
                htmlBuilder.append("<th style='width: 35%;'>OC / Nº PEDIDO</th>");
                htmlBuilder.append("<th style='width: 35%;'>CLIENTE</th>");
                htmlBuilder.append("<th style='width: 18%;'>DESTINO</th>");
                htmlBuilder.append("<th style='width: 6%; text-align: center;'>PAL.</th>");
                htmlBuilder.append("<th style='width: 6%; text-align: center;'>EMC.</th>");
                htmlBuilder.append("</tr>");
                htmlBuilder.append("<tr>");
                htmlBuilder.append("<td>").append(strPedidos).append("</td>");
                htmlBuilder.append("<td><b>").append(nomeClienteFinal).append("</b></td>");
                htmlBuilder.append("<td>").append(strDestino).append("</td>");
                htmlBuilder.append("<td class='center'><b>").append(qtdPaletes).append("</b></td>");
                htmlBuilder.append("<td class='center'>&mdash;</td>");
                htmlBuilder.append("</tr>");

                htmlBuilder.append("<tr style='background: #f9f9f9;'>");
                htmlBuilder.append("<td colspan='3'><b>TOTAL &ndash; 1 OC(s)</b></td>");
                htmlBuilder.append("<td class='center'><b>").append(qtdPaletes).append("</b></td>");
                htmlBuilder.append("<td class='center'><b>&mdash;</b></td>");
                htmlBuilder.append("</tr>");
                htmlBuilder.append("</table>");

                htmlBuilder.append("<div class='obs-box'>");
                htmlBuilder.append("<span style='font-weight: bold; color: #444;'>OBS. QUANTIDADE DEVOLVIDA</span>");
                htmlBuilder.append("</div>");

                htmlBuilder.append("<div class='assinaturas'>");
                htmlBuilder.append("<div class='assinatura-box'>");
                htmlBuilder.append("<span class='assinatura-label'>ASSINATURA DO MOTORISTA</span>");
                htmlBuilder.append("<span class='assinatura-linha'>Nome / Data</span>");
                htmlBuilder.append("</div>");
                htmlBuilder.append("<div class='assinatura-box'>");
                htmlBuilder.append("<span class='assinatura-label'>CONFERENTE / RESPONSÁVEL</span>");
                htmlBuilder.append("<span class='assinatura-linha'>Nome / Data</span>");
                htmlBuilder.append("</div>");
                htmlBuilder.append("</div>");

                htmlBuilder.append("</div>"); // fim via-container
            }

            htmlBuilder.append("</div>"); // fim carregamento-pagina
        }

        htmlBuilder.append("<script>window.onload = function() { window.print(); }</script>");
        htmlBuilder.append("</body></html>");

        // Solução robusta e limpa para abrir a nova aba e injetar o HTML sem falhar em branco
        UI.getCurrent().getPage().executeJs(
            "var win = window.open('', '_blank');" +
            "if (win) {" +
            "  win.document.open();" +
            "  win.document.write(" + jsEscape(htmlBuilder.toString()) + ");" +
            "  win.document.close();" +
            "}"
        );

        Notification.show("Relatório gerado com sucesso!", 3000, Notification.Position.BOTTOM_END);
    }

    private String jsEscape(String content) {
        return "'" + content
                .replace("\\", "\\\\")
                .replace("'", "\\'")
                .replace("\n", "")
                .replace("\r", "") + "'";
    }
}