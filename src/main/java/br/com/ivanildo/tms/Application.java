package br.com.ivanildo.tms;

import com.vaadin.flow.component.page.AppShellConfigurator;
import jakarta.annotation.PostConstruct;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.Locale;
import java.util.TimeZone;

@SpringBootApplication
public class Application implements AppShellConfigurator {

    @PostConstruct
    public void init() {
        // Define o Locale padrão como Português (Brasil)
        Locale.setDefault(Locale.of("pt", "BR"));
        // Define o Fuso Horário padrão (Brasília/Bahia)
        TimeZone.setDefault(TimeZone.getTimeZone("America/Bahia"));
    }

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}