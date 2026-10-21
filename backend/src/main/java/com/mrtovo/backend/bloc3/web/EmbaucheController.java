package com.mrtovo.backend.bloc3.web;

import com.mrtovo.backend.bloc3.service.EmployeService;
import com.mrtovo.backend.bloc3.web.dto.ActionRequest;
import com.mrtovo.backend.bloc3.web.dto.ContratDto;
import com.mrtovo.backend.bloc3.web.dto.EmployeDto;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/bloc3")
public class EmbaucheController {

    private final EmployeService employeService;

    public EmbaucheController(EmployeService employeService) {
        this.employeService = employeService;
    }

    @GetMapping("/employes")
    public List<EmployeDto> employes() {
        return employeService.lister();
    }

    @GetMapping("/employes/{id}")
    public EmployeDto employe(@PathVariable Long id) {
        return employeService.detail(id);
    }

    @GetMapping("/contrats")
    public List<ContratDto> contrats() {
        return employeService.listerContrats();
    }

    @GetMapping("/contrats/{id}")
    public ContratDto contrat(@PathVariable Long id) {
        return employeService.detailContrat(id);
    }

    @PostMapping("/contrats/{id}/signer")
    public ContratDto signer(@PathVariable Long id, @RequestBody(required = false) ActionRequest request) {
        ActionRequest requete = request != null ? request : new ActionRequest(null, null);
        return employeService.signerContrat(id, requete);
    }

    @PostMapping("/contrats/{id}/activer")
    public ContratDto activer(@PathVariable Long id, @RequestBody(required = false) ActionRequest request) {
        ActionRequest requete = request != null ? request : new ActionRequest(null, null);
        return employeService.activerContrat(id, requete);
    }
}
