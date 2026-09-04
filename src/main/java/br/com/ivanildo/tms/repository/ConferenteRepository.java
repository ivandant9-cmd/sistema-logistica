package br.com.ivanildo.tms.repository;

import br.com.ivanildo.tms.model.Conferente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ConferenteRepository extends JpaRepository<Conferente, Long> {
}