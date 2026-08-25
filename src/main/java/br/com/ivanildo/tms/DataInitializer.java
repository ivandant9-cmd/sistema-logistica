package br.com.ivanildo.tms;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    @Override
    public void run(String... args) throws Exception {
        // Mantido vazio para evitar apagar os dados salvos no Render ao reiniciar
    }
}