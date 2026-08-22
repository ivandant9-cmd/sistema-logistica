package br.com.ivanildo.tms.service;

import br.com.ivanildo.tms.model.Carregamento;
import br.com.ivanildo.tms.model.Entrega;
import br.com.ivanildo.tms.repository.CarregamentoRepository;
import br.com.ivanildo.tms.repository.EntregaRepository;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ExcelService {

    private final CarregamentoRepository carregamentoRepository;
    private final EntregaRepository entregaRepository;

    public ExcelService(CarregamentoRepository carregamentoRepository, EntregaRepository entregaRepository) {
        this.carregamentoRepository = carregamentoRepository;
        this.entregaRepository = entregaRepository;
    }

    public void processarExcel(InputStream inputStream) {
        try (Workbook workbook = WorkbookFactory.create(inputStream)) {
            DataFormatter formatter = new DataFormatter();

            // ==========================================
            // 1. LEITURA DA PRIMEIRA ABA (CARREGAMENTOS)
            // ==========================================
            Sheet sheetCarregamentos = workbook.getSheetAt(0);

            Row headerRow = null;
            int headerRowIndex = -1;

            for (int i = 0; i <= Math.min(15, sheetCarregamentos.getLastRowNum()); i++) {
                Row row = sheetCarregamentos.getRow(i);
                if (row != null && !isLinhaVazia(row, formatter)) {
                    headerRow = row;
                    headerRowIndex = i;
                    break;
                }
            }

            if (headerRow == null) {
                throw new IllegalArgumentException("A planilha está vazia ou não contém dados válidos.");
            }

            Map<String, Integer> colunasCarregamentos = mapearCabecalhos(headerRow, formatter);
            Map<String, Carregamento> carregamentosPorViagem = new HashMap<>();
            List<Carregamento> novosCarregamentos = new ArrayList<>();

            for (int r = headerRowIndex + 1; r <= sheetCarregamentos.getLastRowNum(); r++) {
                Row row = sheetCarregamentos.getRow(r);
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
                c.setPeso(getValorPorColuna(row, colunasCarregamentos, "PESO", formatter));
                c.setEncaixe(getValorPorColuna(row, colunasCarregamentos, "ENCAIXE", formatter));
                c.setStatus(getValorPorColuna(row, colunasCarregamentos, "STATUS", formatter));
                c.setObservacao(getValorPorColuna(row, colunasCarregamentos, "OBSERVACAO", formatter));

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

            // ==========================================
            // 2. LEITURA DA ABA "ENTREGAS"
            // ==========================================
            Sheet sheetEntregas = workbook.getSheet("ENTREGAS");
            if (sheetEntregas == null && workbook.getNumberOfSheets() > 1) {
                sheetEntregas = workbook.getSheetAt(1);
            }

            if (sheetEntregas != null) {
                List<Entrega> novasEntregas = new ArrayList<>();

                Row headerEntregasRow = null;
                int startRowEntregas = 1;

                for (int i = 0; i <= Math.min(10, sheetEntregas.getLastRowNum()); i++) {
                    Row r = sheetEntregas.getRow(i);
                    if (r != null && !isLinhaVazia(r, formatter)) {
                        headerEntregasRow = r;
                        startRowEntregas = i + 1;
                        break;
                    }
                }

                Map<String, Integer> colunasEntregas = headerEntregasRow != null ? mapearCabecalhos(headerEntregasRow, formatter) : new HashMap<>();

                for (int i = startRowEntregas; i <= sheetEntregas.getLastRowNum(); i++) {
                    Row row = sheetEntregas.getRow(i);
                    if (row == null || isLinhaVazia(row, formatter)) continue;

                    String viagemRef = getValorFlexivel(row, colunasEntregas, "VIAGEM", 1, formatter);
                    String codigoCliente = getValorFlexivel(row, colunasEntregas, "CODIGOCLIENTE", 2, formatter);
                    if (codigoCliente.isEmpty()) codigoCliente = getValorFlexivel(row, colunasEntregas, "CODCLIENTE", 2, formatter);

                    String delivery = getValorFlexivel(row, colunasEntregas, "DELIVERY", 5, formatter);
                    String nf = getValorFlexivel(row, colunasEntregas, "NOTAFISCAL", 6, formatter);
                    if (nf.isEmpty()) nf = getValorFlexivel(row, colunasEntregas, "NF", 6, formatter);

                    String cliente = getValorFlexivel(row, colunasEntregas, "CLIENTE", 7, formatter);
                    String bairro = getValorFlexivel(row, colunasEntregas, "BAIRRO", 9, formatter);
                    String cidade = getValorFlexivel(row, colunasEntregas, "CIDADE", 10, formatter);
                    String peso = getValorFlexivel(row, colunasEntregas, "PESO", 14, formatter);

                    if (delivery.isEmpty() && nf.isEmpty()) continue;

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
                        entrega.setCarregamento(carregamentoCorrespondente);

                        novasEntregas.add(entrega);
                    }

                    if (novasEntregas.size() >= 100) {
                        salvarEntregasLote(novasEntregas);
                        novasEntregas.clear();
                    }
                }

                if (!novasEntregas.isEmpty()) {
                    salvarEntregasLote(novasEntregas);
                    novasEntregas.clear();
                }
            }

            carregamentosPorViagem.clear();
            System.gc();

        } catch (Exception e) {
            throw new RuntimeException("Erro ao processar planilha Excel: " + e.getMessage(), e);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void salvarCarregamentosLote(List<Carregamento> lista, Map<String, Carregamento> mapaCache) {
        List<Carregamento> salvos = carregamentoRepository.saveAll(lista);
        for (Carregamento c : salvos) {
            if (c.getViagem() != null && !c.getViagem().trim().isEmpty()) {
                mapaCache.put(c.getViagem().trim().toUpperCase(), c);
            }
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void salvarEntregasLote(List<Entrega> lista) {
        entregaRepository.saveAll(lista);
    }

    private Map<String, Integer> mapearCabecalhos(Row row, DataFormatter formatter) {
        Map<String, Integer> map = new HashMap<>();
        for (Cell cell : row) {
            String val = normalizarTexto(getValorCelula(cell, formatter));
            if (!val.isEmpty()) {
                map.put(val, cell.getColumnIndex());
            }
        }
        return map;
    }

    private String getValorFlexivel(Row row, Map<String, Integer> colunas, String nomeColuna, int indiceFallback, DataFormatter formatter) {
        Integer idx = colunas.get(normalizarTexto(nomeColuna));
        if (idx != null) {
            String valor = getValorCelula(row.getCell(idx), formatter);
            if (!valor.isEmpty()) return valor;
        }
        return getValorCelula(row.getCell(indiceFallback), formatter);
    }

    private String getValorPorColuna(Row row, Map<String, Integer> colunas, String nomeColuna, DataFormatter formatter) {
        Integer index = colunas.get(normalizarTexto(nomeColuna));
        if (index == null) return "";
        return getValorCelula(row.getCell(index), formatter);
    }

    private String getValorCelula(Cell cell, DataFormatter formatter) {
        if (cell == null) return "";
        try {
            if (cell.getCellType() == CellType.FORMULA) {
                switch (cell.getCachedFormulaResultType()) {
                    case STRING:
                        return cell.getStringCellValue().trim();
                    case NUMERIC:
                        return formatter.formatCellValue(cell).trim();
                    case BOOLEAN:
                        return String.valueOf(cell.getBooleanCellValue());
                    default:
                        return cell.getCellFormula();
                }
            }
            return formatter.formatCellValue(cell).trim();
        } catch (Exception e) {
            return formatter.formatCellValue(cell).trim();
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
        for (int c = row.getFirstCellNum(); c < row.getLastCellNum(); c++) {
            Cell cell = row.getCell(c);
            if (cell != null && !getValorCelula(cell, formatter).isEmpty()) {
                return false;
            }
        }
        return true;
    }
}