package br.com.ivanildo.tms.controller;

import br.com.ivanildo.tms.model.Carregamento;
import br.com.ivanildo.tms.repository.CarregamentoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/carregamentos")
public class CarregamentoController {

    @Autowired
    private CarregamentoRepository carregamentoRepository;

    @GetMapping
    public List<Carregamento> listarTodos() {
        return carregamentoRepository.findAll();
    }
}