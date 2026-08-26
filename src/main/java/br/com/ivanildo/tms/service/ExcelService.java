package br.com.ivanildo.tms.service;

import br.com.ivanildo.tms.model.Carregamento;
import br.com.ivanildo.tms.model.Entrega;
import br.com.ivanildo.tms.repository.CarregamentoRepository;
import br.com.ivanildo.tms.repository.EntregaRepository;
import com.github.pjfanning.xlsx.StreamingReader;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;


@Service
public class ExcelService {

    private final CarregamentoRepository carregamentoRepository;
    private final EntregaRepository entregaRepository;

  
    public ExcelService(CarregamentoRepository carregamentoRepository, EntregaRepository entregaRepository) {
        this.carregamentoRepository = carregamentoRepository;
        this.entregaRepository = entregaRepository;
    }

    @Transactional 
    public void processarExcel(InputStream inputStream) {
        try {
            entregaRepository.deleteAllInBatch();
            carregamentoRepository.deleteAllInBatch();

            Workbook workbook = StreamingReader.builder()
                    .rowCacheSize(100)
                    .bufferSize(4096)
                    .open(inputStream);

            DataFormatter formatter = new DataFormatter(new Locale.Builder().setLanguage("pt").setRegion("BR").build());
            
            // ==========================================
            // 1. LEITURA DA PRIMEIRA ABA (CARREGAMENTOS)
            // ==========================================
            Sheet sheetCarregamentos = workbook.getSheetAt(0);

            Row headerRow = null;
            int headerRowIndex = -1;
            int countRow = 0;

            for (Row row : sheetCarregamentos) {
                if (countRow > 15) break;
                if (row != null && !isLinhaVazia(row, formatter)) {
                    headerRow = row;
                    headerRowIndex = countRow;
                    break;
                }
                countRow++;
            }

            if (headerRow == null) {
                throw new IllegalArgumentException("A planilha está vazia ou não contém dados válidos.");
            }

            Map<String, Integer> colunasCarregamentos = mapearCabecalhos(headerRow, formatter);
            Map<String, Carregamento> carregamentosPorViagem = new HashMap<>();
            List<Carregamento> novosCarregamentos = new ArrayList<>();

            int currentRow = 0;
            for (Row row : sheetCarregamentos) {
                if (currentRow <= headerRowIndex) {
                    currentRow++;
                    continue;
                }
                currentRow++;

                if (row == null || isLinhaVazia(row, formatter)) continue;

                Carregamento c = new Carregamento();
                c.setDataProgramacao(getValorPorColuna(row, colunasCarregamentos, "DATAPROG", formatter));
                if (c.getDataProgramacao().isEmpty()) {
                    c.setDataProgramacao(getValorPorColuna(row, colunasCarregamentos, "DATA", formatter));
                }

                c.setTransportadora(getValorPorColuna(row, colunasCarregamentos, "TRANSPORTADORA", formatter));
                c.setPlaca(getValorPorColuna(row, colunasCarregamentos, "PLACA", formatter));
                c.setTipoVeiculo(getValorPorColuna(row, colunasCarregamentos, "TIPODEVEICULO", formatter));
                c.setViagem(getValorPorColuna(row, colunasCarregamentos, "VIAGEM", formatter));
                c.setOrdemCarga(getValorPorColuna(row, colunasCarregamentos, "ORDEMDECARGA", formatter));
                c.setPeso(tratarValorPeso(row, colunasCarregamentos, "PESO", formatter));
                c.setEncaixe(getValorPorColuna(row, colunasCarregamentos, "ENCAIXE", formatter));
                c.setStatus(getValorPorColuna(row, colunasCarregamentos, "STATUS", formatter));
                c.setObservacao(getValorPorColuna(row, colunasCarregamentos, "OBSERVACAO", formatter));

                String valPaletes = getValorPorColuna(row, colunasCarregamentos, "PALETES", formatter);
if (valPaletes.isEmpty()) {
    valPaletes = getValorPorColuna(row, colunasCarregamentos, "PALETE", formatter);
}
if (!valPaletes.isEmpty()) {
    try {
        // Remove pontos ou vírgulas caso venha formatado como número decimal
        String limpo = valPaletes.replaceAll("[^0-9]", "");
        c.setPaletes(limpo.isEmpty() ? 0 : Integer.parseInt(limpo));
    } catch (NumberFormatException e) {
        c.setPaletes(0);
    }
} else {
    c.setPaletes(0);
}

                if (!c.getViagem().isEmpty() || !c.getPlaca().isEmpty() || !c.getTransportadora().isEmpty()) {
                    novosCarregamentos.add(c);
                }

                if (novosCarregamentos.size() >= 100) {
                    salvarCarregamentosLote(novosCarregamentos, carregamentosPorViagem);
                    novosCarregamentos.clear();
                }
            }

            if (!novosCarregamentos.isEmpty()) {
                salvarCarregamentosLote(novosCarregamentos, carregamentosPorViagem);
                novosCarregamentos.clear();
            }

            // ============================================================
            // 2. EXTRAÇÃO DE CONJUNTOS DE REENTREGAS E COLOG / NUTRÍCIA
            // ============================================================
            Set<String> reentregasDeliveries = extrairValoresColunaAba(workbook, "ENCAIXE REENTREGAS", 1, formatter);
            Set<String> cologNutriciaNfs = extrairValoresColunaAba(workbook, "ENCAIXE NUTRICIA_COLOG", 0, formatter);

            // ============================================================
            // 3. LEITURA DE TODAS AS ABAS DE ENTREGAS
            // ============================================================
            int totalSheets = workbook.getNumberOfSheets();
            List<Entrega> novasEntregas = new ArrayList<>();

            for (int sheetIdx = 1; sheetIdx < totalSheets; sheetIdx++) {
                Sheet sheetAtual = workbook.getSheetAt(sheetIdx);
                String nomeAba = sheetAtual.getSheetName();

                String nomeAbaNorm = normalizarTexto(nomeAba);
                if (nomeAbaNorm.contains("ENCAIXEREENTREGAS") || nomeAbaNorm.contains("ENCAIXENUTRICIACOLOG")) {
                    continue;
                }

                Row headerEntregasRow = null;
                int startRowEntregasIndex = -1;
                int rowIdx = 0;

                for (Row r : sheetAtual) {
                    if (rowIdx > 10) break;
                    if (r != null && !isLinhaVazia(r, formatter)) {
                        headerEntregasRow = r;
                        startRowEntregasIndex = rowIdx;
                        break;
                    }
                    rowIdx++;
                }

                Map<String, Integer> colunasEntregas = headerEntregasRow != null ? mapearCabecalhos(headerEntregasRow, formatter) : new HashMap<>();

                int indexEntrega = 0;
                for (Row row : sheetAtual) {
                    if (indexEntrega <= startRowEntregasIndex) {
                        indexEntrega++;
                        continue;
                    }
                    indexEntrega++;

                    if (row == null || isLinhaVazia(row, formatter)) continue;

                    String viagemRef = getValorSeguro(row, colunasEntregas, "VIAGEM", formatter);
                    String codigoCliente = getValorSeguro(row, colunasEntregas, "CODIGOCLIENTE", formatter);
                    if (codigoCliente.isEmpty()) codigoCliente = getValorSeguro(row, colunasEntregas, "CODCLIENTE", formatter);

                    String delivery = getValorSeguro(row, colunasEntregas, "DELIVERY", formatter);
                    if (delivery.isEmpty()) delivery = getValorSeguro(row, colunasEntregas, "DELIVERY2", formatter);

                    String nf = getValorSeguro(row, colunasEntregas, "NOTAFISCAL", formatter);
                    if (nf.isEmpty()) nf = getValorSeguro(row, colunasEntregas, "NF", formatter);

                    String cliente = getValorSeguro(row, colunasEntregas, "CLIENTE", formatter);
                    String bairro = getValorSeguro(row, colunasEntregas, "BAIRRO", formatter);
                    String cidade = getValorSeguro(row, colunasEntregas, "CIDADE", formatter);
                    String peso = normalizarFormatoPeso(getValorSeguro(row, colunasEntregas, "PESO", formatter));

                    if (!isEntregaValida(delivery, nf, codigoCliente, cliente)) {
                        continue;
                    }

                    Carregamento carregamentoCorrespondente = carregamentosPorViagem.get(viagemRef.toUpperCase());

                    if (carregamentoCorrespondente == null && !viagemRef.isEmpty()) {
                        carregamentoCorrespondente = carregamentoRepository.findByViagem(viagemRef).orElse(null);
                        if (carregamentoCorrespondente != null) {
                            carregamentosPorViagem.put(viagemRef.toUpperCase(), carregamentoCorrespondente);
                        }
                    }

                    if (carregamentoCorrespondente != null) {
                        Entrega entrega = new Entrega();
                        entrega.setCodigoCliente(codigoCliente);
                        entrega.setDelivery(delivery);
                        entrega.setNf(nf);
                        entrega.setCliente(cliente);
                        entrega.setBairro(bairro);
                        entrega.setCidade(cidade);
                        entrega.setPeso(peso);

                        if (!delivery.isEmpty() && reentregasDeliveries.contains(delivery.trim().toUpperCase())) {
                            entrega.setOrigemSheet("REENTREGAS");
                        } else if (!nf.isEmpty() && cologNutriciaNfs.contains(nf.trim().toUpperCase())) {
                            entrega.setOrigemSheet("COLOG / NUTRÍCIA");
                        } else {
                            entrega.setOrigemSheet(nomeAba);
                        }

                        entrega.setCarregamento(carregamentoCorrespondente);
                        novasEntregas.add(entrega);
                    }

                    if (novasEntregas.size() >= 100) {
                        salvarEntregasLote(novasEntregas);
                        novasEntregas.clear();
                    }
                }
            }

            if (!novasEntregas.isEmpty()) {
                salvarEntregasLote(novasEntregas);
                novasEntregas.clear();
            }

            carregamentosPorViagem.clear();
            System.gc();

        } catch (Exception e) {
            throw new RuntimeException("Erro ao processar planilha Excel via streaming: " + e.getMessage(), e);
        }
    }

    private Set<String> extrairValoresColunaAba(Workbook workbook, String nomeAba, int indiceColuna, DataFormatter formatter) {
        Set<String> valores = new HashSet<>();
        Sheet sheet = workbook.getSheet(nomeAba);
        if (sheet == null) {
            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                if (normalizarTexto(workbook.getSheetName(i)).equals(normalizarTexto(nomeAba))) {
                    sheet = workbook.getSheetAt(i);
                    break;
                }
            }
        }
        if (sheet != null) {
            for (Row row : sheet) {
                if (row == null) continue;
                Cell cell = row.getCell(indiceColuna);
                String val = getValorCelula(cell, formatter);
                if (!val.isEmpty()) {
                    valores.add(val.trim().toUpperCase());
                }
            }
        }
        return valores;
    }

    @Transactional
    public void salvarCarregamentosLote(List<Carregamento> lista, Map<String, Carregamento> mapaCache) {
        List<Carregamento> salvos = carregamentoRepository.saveAll(lista);
        carregamentoRepository.flush(); 
        for (Carregamento c : salvos) {
            if (c.getViagem() != null && !c.getViagem().trim().isEmpty()) {
                mapaCache.put(c.getViagem().trim().toUpperCase(), c);
            }
        }
    }

    @Transactional
    public void salvarEntregasLote(List<Entrega> lista) {
        entregaRepository.saveAll(lista);
        entregaRepository.flush(); 
    }

    private boolean isEntregaValida(String delivery, String nf, String codigoCliente, String cliente) {
        if (delivery.isEmpty() && nf.isEmpty() && codigoCliente.isEmpty()) return false;

        String nfTrim = nf.trim().toUpperCase();
        String clienteTrim = cliente.trim().toUpperCase();

        if (nfTrim.contains("SUBTOTAL") || nfTrim.contains("TOTAL")) return false;

        if (clienteTrim.equals("HR") ||
            clienteTrim.equals("TRUCK") ||
            clienteTrim.equals("CARRETA") ||
            clienteTrim.equals("TOCO") ||
            clienteTrim.equals("3/4") ||
            clienteTrim.equals("34") ||
            clienteTrim.equals("VAN") ||
            clienteTrim.equals("BONGO") ||
            clienteTrim.equals("IVECO") ||
            clienteTrim.equals("SPRINTER") ||
            clienteTrim.equals("F4000") ||
            clienteTrim.contains("CARREGAR NO")) {
            return false;
        }

        return true;
    }

    private Map<String, Integer> mapearCabecalhos(Row row, DataFormatter formatter) {
        Map<String, Integer> map = new HashMap<>();
        for (Cell cell : row) {
            String valOriginal = getValorCelula(cell, formatter);
            String val = normalizarTexto(valOriginal);
            if (!val.isEmpty()) {
                map.put(val, cell.getColumnIndex());
            }
        }
        return map;
    }

    private String getValorSeguro(Row row, Map<String, Integer> colunas, String nomeColuna, DataFormatter formatter) {
        Integer index = colunas.get(normalizarTexto(nomeColuna));
        if (index == null) return "";
        return getValorCelula(row.getCell(index), formatter);
    }

    private String getValorPorColuna(Row row, Map<String, Integer> colunas, String nomeColuna, DataFormatter formatter) {
        Integer index = colunas.get(normalizarTexto(nomeColuna));
        if (index == null) return "";
        return getValorCelula(row.getCell(index), formatter);
    }

    private String tratarValorPeso(Row row, Map<String, Integer> colunas, String nomeColuna, DataFormatter formatter) {
        Integer index = colunas.get(normalizarTexto(nomeColuna));
        if (index == null) return "0";
        return normalizarFormatoPeso(getValorCelula(row.getCell(index), formatter));
    }

    private String normalizarFormatoPeso(String valorRaw) {
        if (valorRaw == null || valorRaw.trim().isEmpty()) return "0";
        String v = valorRaw.trim();

        if (v.contains(",") && !v.contains(".")) {
            v = v.replace(",", ".");
        }
        return v;
    }

    private String getValorCelula(Cell cell, DataFormatter formatter) {
        if (cell == null) return "";
        try {
            return formatter.formatCellValue(cell).trim();
        } catch (Exception e) {
            return "";
        }
    }

    private String normalizarTexto(String texto) {
        if (texto == null) return "";
        String semAcento = Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        return semAcento.replaceAll("[^a-zA-Z0-9]", "").toUpperCase().trim();
    }

    private boolean isLinhaVazia(Row row, DataFormatter formatter) {
        if (row == null) return true;
        for (Cell cell : row) {
            if (cell != null && !getValorCelula(cell, formatter).isEmpty()) {
                return false;
            }
        }
        return true;
    }
}