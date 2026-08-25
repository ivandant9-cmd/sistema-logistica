package br.com.ivanildo.tms;

import br.com.ivanildo.tms.repository.CarregamentoRepository;
import br.com.ivanildo.tms.repository.EntregaRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private final CarregamentoRepository carregamentoRepository;
    private final EntregaRepository entregaRepository;

    public DataInitializer(CarregamentoRepository carregamentoRepository, EntregaRepository entregaRepository) {
        this.carregamentoRepository = carregamentoRepository;
        this.entregaRepository = entregaRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        // Exclui em lote via instrução SQL direta, evitando StaleObjectStateException e falhas de lock
        entregaRepository.deleteAllInBatch();
        carregamentoRepository.deleteAllInBatch();
    }
}