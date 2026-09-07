package br.com.ivanildo.tms.controller;

import br.com.ivanildo.tms.model.Carregamento;
import br.com.ivanildo.tms.service.ExcelService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/carregamentos")
public class CarregamentoController {

    private final ExcelService excelService;

    public CarregamentoController(ExcelService excelService) {
        this.excelService = excelService;
    }

    @GetMapping
    public List<Carregamento> listarAtivos() {
        return excelService.listarCarregamentosParaMainView();
    }
}