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
import java.time.LocalDateTime;
import java.util.*;




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
            // ATENÇÃO: Removemos o deleteAllInBatch() para preservar os dados existentes e não apagar nada construído!

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

               String valPaletes = "";

// Tenta pegar pelo nome da coluna primeiro
valPaletes = getValorPorColuna(row, colunasCarregamentos, "PALETES", formatter);
if (valPaletes.isEmpty()) {
    valPaletes = getValorPorColuna(row, colunasCarregamentos, "PALETE", formatter);
}

// Fallback direto para a coluna 14 ou 15 (índice 14 se for a coluna O, ou 15 se estiver ajustando)
if (valPaletes.isEmpty()) {
    try {
        org.apache.poi.ss.usermodel.Cell cellPaletes = row.getCell(14); // ou 15 conforme sua contagem
        if (cellPaletes != null) {
            if (cellPaletes.getCellType() == org.apache.poi.ss.usermodel.CellType.NUMERIC) {
                int qtd = (int) cellPaletes.getNumericCellValue();
                valPaletes = String.valueOf(qtd);
            } else {
                valPaletes = cellPaletes.toString();
            }
        }
    } catch (Exception ignored) {}
}

if (!valPaletes.isEmpty()) {
    try {
        double numeroParsed = Double.parseDouble(valPaletes.replace(",", "."));
        c.setPaletes((int) numeroParsed);
    } catch (Exception e) {
        c.setPaletes(0);
    }
} else {
    c.setPaletes(0);
}

                if (!c.getViagem().isEmpty() || !c.getPlaca().isEmpty() || !c.getTransportadora().isEmpty()) {
                    novosCarregamentos.add(c);
                }

                if (novosCarregamentos.size() >= 100) {
                    salvarCarregamentosLoteInteligente(novosCarregamentos, carregamentosPorViagem);
                    novosCarregamentos.clear();
                }
            }

            if (!novosCarregamentos.isEmpty()) {
                salvarCarregamentosLoteInteligente(novosCarregamentos, carregamentosPorViagem);
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
                        salvarEntregasLoteInteligente(novasEntregas);
                        novasEntregas.clear();
                    }
                }
            }

            if (!novasEntregas.isEmpty()) {
                salvarEntregasLoteInteligente(novasEntregas);
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
    public void salvarCarregamentosLoteInteligente(List<Carregamento> lista, Map<String, Carregamento> mapaCache) {
        List<Carregamento> paraSalvarOrAtualizar = new ArrayList<>();

        for (Carregamento cNovo : lista) {
            if (cNovo.getViagem() != null && !cNovo.getViagem().trim().isEmpty()) {
                Optional<Carregamento> existenteOpt = carregamentoRepository.findByViagem(cNovo.getViagem().trim());
                if (existenteOpt.isPresent()) {
                    Carregamento existente = existenteOpt.get();
                    // Mesclagem Inteligente: preenche apenas campos vazios/nulos do banco com os dados novos
                    if (isNullOrEmpty(existente.getDataProgramacao()) && !isNullOrEmpty(cNovo.getDataProgramacao())) existente.setDataProgramacao(cNovo.getDataProgramacao());
                    if (isNullOrEmpty(existente.getTransportadora()) && !isNullOrEmpty(cNovo.getTransportadora())) existente.setTransportadora(cNovo.getTransportadora());
                    if (isNullOrEmpty(existente.getPlaca()) && !isNullOrEmpty(cNovo.getPlaca())) existente.setPlaca(cNovo.getPlaca());
                    if (isNullOrEmpty(existente.getTipoVeiculo()) && !isNullOrEmpty(cNovo.getTipoVeiculo())) existente.setTipoVeiculo(cNovo.getTipoVeiculo());
                    if (isNullOrEmpty(existente.getOrdemCarga()) && !isNullOrEmpty(cNovo.getOrdemCarga())) existente.setOrdemCarga(cNovo.getOrdemCarga());
                    if (isNullOrEmpty(existente.getPeso()) || existente.getPeso().equals("0")) if (!isNullOrEmpty(cNovo.getPeso())) existente.setPeso(cNovo.getPeso());
                    if (isNullOrEmpty(existente.getEncaixe()) && !isNullOrEmpty(cNovo.getEncaixe())) existente.setEncaixe(cNovo.getEncaixe());
                    if (isNullOrEmpty(existente.getStatus()) && !isNullOrEmpty(cNovo.getStatus())) existente.setStatus(cNovo.getStatus());
                    if (isNullOrEmpty(existente.getObservacao()) && !isNullOrEmpty(cNovo.getObservacao())) existente.setObservacao(cNovo.getObservacao());
                    if ((existente.getPaletes() == null || existente.getPaletes() == 0) && cNovo.getPaletes() != null && cNovo.getPaletes() > 0) existente.setPaletes(cNovo.getPaletes());

                    paraSalvarOrAtualizar.add(existente);
                } else {
                    paraSalvarOrAtualizar.add(cNovo);
                }
            } else {
                paraSalvarOrAtualizar.add(cNovo);
            }
        }

        List<Carregamento> salvos = carregamentoRepository.saveAll(paraSalvarOrAtualizar);
        carregamentoRepository.flush(); 
        for (Carregamento c : salvos) {
            if (c.getViagem() != null && !c.getViagem().trim().isEmpty()) {
                mapaCache.put(c.getViagem().trim().toUpperCase(), c);
            }
        }
    }

    @Transactional
    public void salvarEntregasLoteInteligente(List<Entrega> lista) {
        List<Entrega> paraSalvarOrAtualizar = new ArrayList<>();

        for (Entrega eNovo : lista) {
            boolean jaExiste = false;
            if (eNovo.getCarregamento() != null && eNovo.getCarregamento().getId() != null) {
                // Busca as entregas existentes daquele carregamento pelo ID do carregamento
                List<Entrega> existentes = entregaRepository.findByCarregamentoId(eNovo.getCarregamento().getId());
                for (Entrega ex : existentes) {
                    boolean matchDelivery = !isNullOrEmpty(eNovo.getDelivery()) && eNovo.getDelivery().equalsIgnoreCase(ex.getDelivery());
                    boolean matchNf = !isNullOrEmpty(eNovo.getNf()) && eNovo.getNf().equalsIgnoreCase(ex.getNf());

                    if (matchDelivery || matchNf) {
                        jaExiste = true;
                        // Merge inteligente nos campos da entrega
                        if (isNullOrEmpty(ex.getCodigoCliente()) && !isNullOrEmpty(eNovo.getCodigoCliente())) ex.setCodigoCliente(eNovo.getCodigoCliente());
                        if (isNullOrEmpty(ex.getCliente()) && !isNullOrEmpty(eNovo.getCliente())) ex.setCliente(eNovo.getCliente());
                        if (isNullOrEmpty(ex.getBairro()) && !isNullOrEmpty(eNovo.getBairro())) ex.setBairro(eNovo.getBairro());
                        if (isNullOrEmpty(ex.getCidade()) && !isNullOrEmpty(eNovo.getCidade())) ex.setCidade(eNovo.getCidade());
                        if (isNullOrEmpty(ex.getPeso()) || ex.getPeso().equals("0")) if (!isNullOrEmpty(eNovo.getPeso())) ex.setPeso(eNovo.getPeso());
                        if (isNullOrEmpty(ex.getOrigemSheet()) && !isNullOrEmpty(eNovo.getOrigemSheet())) ex.setOrigemSheet(eNovo.getOrigemSheet());
                        
                        paraSalvarOrAtualizar.add(ex);
                        break;
                    }
                }
            }

            if (!jaExiste) {
                paraSalvarOrAtualizar.add(eNovo);
            }
        }

        entregaRepository.saveAll(paraSalvarOrAtualizar);
        entregaRepository.flush(); 
    }

    private boolean isNullOrEmpty(String str) {
        return str == null || str.trim().isEmpty();
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
        // Se for célula de data do Excel, lê de forma exata evitando perda de fuso
        if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
            LocalDateTime date = cell.getLocalDateTimeCellValue();
            if (date != null) {
                return date.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            }
        }
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