package br.com.ivanildo.tms.repository;

import br.com.ivanildo.tms.model.Carregamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CarregamentoRepository extends JpaRepository<Carregamento, Long> {
    Optional<Carregamento> findByViagem(String viagem);
}   