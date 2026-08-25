package br.com.ivanildo.tms.repository;

import br.com.ivanildo.tms.model.Carregamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CarregamentoRepository extends JpaRepository<Carregamento, Long> {

    // Busca todos os carregamentos atrelados a uma placa específica (ignorando maiúsculas/minúsculas)
    List<Carregamento> findByPlacaIgnoreCase(String placa);

    // Opcional: buscar por placa e status específico (ex: apenas agendados / não apresentados)
    List<Carregamento> findByPlacaIgnoreCaseAndStatusNot(String placa, String status);
    
    Optional<Carregamento> findByViagem(String viagem);

    // --- NOVOS MÉTODOS PARA O ARQUIVAMENTO ---
    
    // Busca cargas que estão arquivadas (true)
    List<Carregamento> findByArquivadoTrue();

    // Busca cargas que NÃO estão arquivadas (false ou nulo)
    List<Carregamento> findByArquivadoFalseOrArquivadoIsNull();
}