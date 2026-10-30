package com.mrtovo.backend.controller;

import com.mrtovo.backend.dto.OffreDTO;
import com.mrtovo.backend.service.RecrutementService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/offres")
@RequiredArgsConstructor
public class OffreController {

    private final RecrutementService service;

    @GetMapping
    public List<OffreDTO> lister() {
        return service.listerOffres();
    }

    @GetMapping("/{id}")
    public OffreDTO get(@PathVariable Long id) {
        return service.getOffre(id);
    }
}
