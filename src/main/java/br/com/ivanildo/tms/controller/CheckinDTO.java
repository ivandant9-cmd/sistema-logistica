package br.com.ivanildo.tms.controller;

public class CheckinDTO {
    private String placa;
    private String cpf;
    private String nome;

    public String getPlaca() { return placa; }
    public void setPlaca(String placa) { this.placa = placa; }

    public String getCpf() { return cpf; }
    public void setCpf(String cpf) { this.cpf = cpf; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
}