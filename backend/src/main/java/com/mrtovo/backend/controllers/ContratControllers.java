package com.mrtovo.backend.controllers;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mrtovo.backend.entity.contrat.Contrat;
import com.mrtovo.backend.service.ContratService;
@RestController
@RequestMapping("/api/contrat")
@CrossOrigin(origins = "http://localhost:5173")
public class ContratControllers {
    @Autowired
    private ContratService contratService;
    @GetMapping("")
    public List<Contrat> getAll(Model model){
        return contratService.getAll();
    }
    @GetMapping("/{id}")
    public Contrat getById(@PathVariable Long id){
        if(id != null){
            return contratService.getById(id);
        }
        return null;
    }
    @PostMapping("")
    public ResponseEntity<Contrat> save(@RequestBody Contrat contrat){
        if(contrat.getId() != null && contratService.getById(contrat.getId()) != null) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(null);
        }
        if(contrat.getDateDebut() != null && contrat.getDateFin() != null && contrat.getTypeContrat() != null && contrat.getSalaire() > 0 && contrat.getStatutContrat() != null && contrat.getEmploye() != null) {
            Contrat savedContrat = contratService.save(contrat);
            return ResponseEntity.status(HttpStatus.CREATED).body(savedContrat);
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
    }
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id){
        try{
            if(id != null){
                contratService.delete(id);
            } else {
                return ResponseEntity.status(HttpStatus.NOT_MODIFIED).build();
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
        return ResponseEntity.noContent().build();
    }
    @PutMapping("/{id}")
    public ResponseEntity<Void> update(@PathVariable Long id, @RequestBody Contrat contrat){
        if(id != null && contrat.getDateDebut() != null && contrat.getDateFin() != null && contrat.getTypeContrat() != null && contrat.getSalaire() > 0 && contrat.getStatutContrat() != null && contrat.getEmploye() != null) {
            contratService.update(id, contrat);
            return ResponseEntity.ok().build();
        }
        else{
           return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
        }
    }


}