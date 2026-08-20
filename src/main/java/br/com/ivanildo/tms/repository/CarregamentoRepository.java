package br.com.ivanildo.tms.repository;

import br.com.ivanildo.tms.model.Carregamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CarregamentoRepository extends JpaRepository<Carregamento, Long> {
}