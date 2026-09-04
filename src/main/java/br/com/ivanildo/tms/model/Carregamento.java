package br.com.ivanildo.tms.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "carregamentos")
public class Carregamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String viagem;
    private String transportadora;
    private String ordemCarga;
    private String motorista; // Mantido exatamente como estava
    private String status;
    private String dataProgramacao;
    private String placa;
    private String placaAnterior;
    private String tipoVeiculo;
    private String peso;
    private String encaixe;
    @Column(length = 2000)
    private String notasFiscais;
    @Column(length = 1000)
    private String observacao;
    private LocalDateTime horaChegada;
private LocalDateTime horaInicioCarregamento;
private LocalDateTime horaFimCarregamento;



    // --- NOVOS CAMPOS PARA O CHECK-IN SELF-SERVICE ---
    @ManyToOne
    @JoinColumn(name = "motorista_entidade_id")
    private Motorista motoristaEntidade;

    private LocalDateTime dataHoraApresentacao;

    // --- CAMPO PARA ARQUIVAMENTO ---
    private Boolean arquivado = false;

    public Carregamento() {}

    // Getters e Setters Originais
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getViagem() { return viagem; }
    public void setViagem(String viagem) { this.viagem = viagem; }

    public String getTransportadora() { return transportadora; }
    public void setTransportadora(String transportadora) { this.transportadora = transportadora; }

    public String getOrdemCarga() { return ordemCarga; }
    public void setOrdemCarga(String ordemCarga) { this.ordemCarga = ordemCarga; }

    public String getMotorista() { return motorista; }
    public void setMotorista(String motorista) { this.motorista = motorista; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getDataProgramacao() { return dataProgramacao; }
    public void setDataProgramacao(String dataProgramacao) { this.dataProgramacao = dataProgramacao; }

    public String getPlaca() { return placa; }
    public void setPlaca(String placa) { this.placa = placa; }

    public String getPlacaAnterior() { return placaAnterior; }
    public void setPlacaAnterior(String placaAnterior) { this.placaAnterior = placaAnterior; }

    public String getTipoVeiculo() { return tipoVeiculo; }
    public void setTipoVeiculo(String tipoVeiculo) { this.tipoVeiculo = tipoVeiculo; }

    public String getPeso() { return peso; }
    public void setPeso(String peso) { this.peso = peso; }

    public String getEncaixe() { return encaixe; }
    public void setEncaixe(String encaixe) { this.encaixe = encaixe; }

    public String getNotasFiscais() { return notasFiscais; }
    public void setNotasFiscais(String notasFiscais) { this.notasFiscais = notasFiscais; }

    public String getObservacao() { return observacao; }
    public void setObservacao(String observacao) { this.observacao = observacao; }

    // Getters e Setters dos novos campos
    public Motorista getMotoristaEntidade() { return motoristaEntidade; }
    public void setMotoristaEntidade(Motorista motoristaEntidade) { this.motoristaEntidade = motoristaEntidade; }

    public LocalDateTime getDataHoraApresentacao() { return dataHoraApresentacao; }
    public void setDataHoraApresentacao(LocalDateTime dataHoraApresentacao) { this.dataHoraApresentacao = dataHoraApresentacao; }

    // Getters e Setters do Arquivamento
    public Boolean getArquivado() { return arquivado != null ? arquivado : false; }
    public void setArquivado(Boolean arquivado) { this.arquivado = arquivado; }

    public LocalDateTime getHoraChegada() {
    return horaChegada;
    }

    private String conferente;
private String doca;

// Getters e Setters
public String getConferente() {
    return conferente;
}

public void setConferente(String conferente) {
    this.conferente = conferente;
}

public String getDoca() {
    return doca;
}

public void setDoca(String doca) {
    this.doca = doca;

}

public void setHoraChegada(LocalDateTime horaChegada) {
    this.horaChegada = horaChegada;
}

public LocalDateTime getHoraInicioCarregamento() {
    return horaInicioCarregamento;
}

public void setHoraInicioCarregamento(LocalDateTime horaInicioCarregamento) {
    this.horaInicioCarregamento = horaInicioCarregamento;
}

public LocalDateTime getHoraFimCarregamento() {
    return horaFimCarregamento;
}

public void setHoraFimCarregamento(LocalDateTime horaFimCarregamento) {
    this.horaFimCarregamento = horaFimCarregamento;
}

// Retorna o tempo de espera na fila (da Chegada até o Início do Carregamento)
    public String getTempoEsperaFilaFormatado() {
        if (horaChegada != null && horaInicioCarregamento != null) {
            java.time.Duration duracao = java.time.Duration.between(horaChegada, horaInicioCarregamento);
            long horas = duracao.toHours();
            long minutos = duracao.toMinutesPart();
            return horas + "h " + minutos + "m";
        }
        return "-";
    }

    // Retorna o tempo de execução do carregamento (do Início até o Fim)
    public String getTempoCarregamentoFormatado() {
        if (horaInicioCarregamento != null && horaFimCarregamento != null) {
            java.time.Duration duracao = java.time.Duration.between(horaInicioCarregamento, horaFimCarregamento);
            long horas = duracao.toHours();
            long minutos = duracao.toMinutesPart();
            return horas + "h " + minutos + "m";
        }
        return "-";
    }

    // Retorna o Lead Time Total (da Chegada até a Expedição/Fim)
    public String getLeadTimeTotalFormatado() {
        if (horaChegada != null && horaFimCarregamento != null) {
            java.time.Duration duracao = java.time.Duration.between(horaChegada, horaFimCarregamento);
            long horas = duracao.toHours();
            long minutos = duracao.toMinutesPart();
            return horas + "h " + minutos + "m";
        }
        return "-";
    }


    // Novo campo para quantidade de paletes
    private Integer paletes;

    public Integer getPaletes() {
        return paletes;
    }

    public void setPaletes(Integer paletes) {
        this.paletes = paletes;
    }
}