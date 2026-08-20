package br.com.ivanildo.tms.repository;

import br.com.ivanildo.tms.model.Entrega;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EntregaRepository extends JpaRepository<Entrega, Long> {

    @Query("SELECT e FROM Entrega e WHERE e.carregamento.id = :carregamentoId")
    List<Entrega> findByCarregamentoId(@Param("carregamentoId") Long carregamentoId);
}