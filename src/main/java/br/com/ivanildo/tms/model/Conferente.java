package br.com.ivanildo.tms.model;

import jakarta.persistence.*;

@Entity
@Table(name = "conferentes")
public class Conferente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String nome;

    public Conferente() {}

    public Conferente(String nome) {
        this.nome = nome;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    // ADICIONE ESTE MÉTODO ABAIXO
    @Override
    public String toString() {
        return nome;
    }
}