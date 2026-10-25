package com.mrtovo.backend.controller;

import com.mrtovo.backend.service.CandidatureService;
import com.mrtovo.backend.dto.CandidatureSelectionDto;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController("bloc3CandidatureController")
@RequestMapping("/api/bloc3/candidatures")
public class WorkflowCandidatureController {

    private final CandidatureService candidatureService;

    public WorkflowCandidatureController(CandidatureService candidatureService) {
        this.candidatureService = candidatureService;
    }

    @GetMapping
    public List<CandidatureSelectionDto> pipeline() {
        return candidatureService.listerPipeline();
    }

    @GetMapping("/retenues")
    public List<CandidatureSelectionDto> retenues() {
        return candidatureService.listerRetenues();
    }
}
