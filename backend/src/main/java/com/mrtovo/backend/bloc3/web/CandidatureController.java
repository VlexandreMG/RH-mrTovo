package com.mrtovo.backend.bloc3.web;

import com.mrtovo.backend.bloc3.service.CandidatureService;
import com.mrtovo.backend.bloc3.web.dto.CandidatureDto;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/bloc3/candidatures")
public class CandidatureController {

    private final CandidatureService candidatureService;

    public CandidatureController(CandidatureService candidatureService) {
        this.candidatureService = candidatureService;
    }

    @GetMapping
    public List<CandidatureDto> pipeline() {
        return candidatureService.listerPipeline();
    }

    @GetMapping("/retenues")
    public List<CandidatureDto> retenues() {
        return candidatureService.listerRetenues();
    }
}
