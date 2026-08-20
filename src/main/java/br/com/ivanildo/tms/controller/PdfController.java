package br.com.ivanildo.tms.controller;

import br.com.ivanildo.tms.model.Carregamento;
import br.com.ivanildo.tms.model.Entrega;
import br.com.ivanildo.tms.repository.CarregamentoRepository;
import br.com.ivanildo.tms.repository.EntregaRepository;
import br.com.ivanildo.tms.service.PdfService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;

@RestController
public class PdfController {

    private final PdfService pdfService;
    private final CarregamentoRepository carregamentoRepository;
    private final EntregaRepository entregaRepository;

    public PdfController(PdfService pdfService, 
                         CarregamentoRepository carregamentoRepository, 
                         EntregaRepository entregaRepository) {
        this.pdfService = pdfService;
        this.carregamentoRepository = carregamentoRepository;
        this.entregaRepository = entregaRepository;
    }

    @GetMapping(value = "/api/pdf/carregamento/{id}")
    public ResponseEntity<byte[]> baixarPdfCarregamento(@PathVariable Long id) {
        try {
            Optional<Carregamento> carregamentoOpt = carregamentoRepository.findById(id);

            if (carregamentoOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            Carregamento carregamento = carregamentoOpt.get();
            List<Entrega> entregas = entregaRepository.findByCarregamentoId(id);

            byte[] pdfBytes = pdfService.gerarPdfCarregamento(carregamento, entregas);

            if (pdfBytes == null || pdfBytes.length == 0) {
                return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentLength(pdfBytes.length);
            headers.setContentDisposition(
                ContentDisposition.builder("attachment")
                    .filename("carregamento_" + id + ".pdf")
                    .build()
            );

            return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}