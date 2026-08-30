package br.com.ivanildo.tms;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import java.util.Locale;
import java.util.TimeZone;

@SpringBootApplication
public class Application {

    @PostConstruct
    public void init() {
        Locale.setDefault(new Locale.Builder().setLanguage("pt").setRegion("BR").build());
        TimeZone.setDefault(TimeZone.getTimeZone("America/Bahia"));
    }

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}