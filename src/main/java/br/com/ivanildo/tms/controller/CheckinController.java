package br.com.ivanildo.tms.controller;

import br.com.ivanildo.tms.model.Carregamento;
import br.com.ivanildo.tms.model.Motorista;
import br.com.ivanildo.tms.repository.CarregamentoRepository;
import br.com.ivanildo.tms.repository.MotoristaRepository;
import br.com.ivanildo.tms.util.UiBroadcaster; // ajuste se necessário
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;


@RestController
@RequestMapping("/api")

public class CheckinController {

    @Autowired
    private CarregamentoRepository carregamentoRepository;

    @Autowired
    private MotoristaRepository motoristaRepository;

    @PostMapping("/checkin")
    public ResponseEntity<?> realizarCheckin(@RequestBody CheckinDTO request) {
        String placaInformada = request.getPlaca().replaceAll("[^a-zA-Z0-9]", "").toUpperCase();

        List<Carregamento> carregamentos = carregamentoRepository.findByPlacaIgnoreCase(placaInformada);

        if (carregamentos.isEmpty()) {
            return ResponseEntity.badRequest().body("Nenhum agendamento encontrado para a placa: " + request.getPlaca());
        }

        Motorista motorista = motoristaRepository.findByCpf(request.getCpf())
                .orElseGet(() -> {
                    Motorista novo = new Motorista();
                    novo.setCpf(request.getCpf());
                    novo.setNome(request.getNome());
                    return motoristaRepository.save(novo);
                });

        LocalDateTime agora = LocalDateTime.now();
        for (Carregamento c : carregamentos) {
            c.setStatus("Apresentado");
            c.setMotorista(motorista.getNome());
            c.setMotoristaEntidade(motorista);
            c.setDataHoraApresentacao(agora);
            carregamentoRepository.save(c);
        }

        UiBroadcaster.broadcast("CHECKIN_REALIZADO");

        return ResponseEntity.ok().body("Check-in realizado com sucesso para " + carregamentos.size() + " viagem(ns)!");
    }
}