package com.mrtovo.backend.controllers.contrat;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
import com.mrtovo.backend.service.contrat.ContratService;

@RestController
@RequestMapping("/api/contrat")
@CrossOrigin(origins = "http://localhost:5173")
public class ContratControllers {
    @Autowired
    private ContratService contratService;

    @GetMapping
    public List<Contrat> getAll() {
        return contratService.getAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Contrat> getById(@PathVariable Long id) {
        Contrat contrat = contratService.getById(id);
        return contrat == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(contrat);
    }

    @PostMapping
    public ResponseEntity<Contrat> save(@RequestBody Contrat contrat) {
        if (contrat.getDateDebut() == null || contrat.getDateFin() == null
                || contrat.getTypeContrat() == null || contrat.getSalaire() <= 0
                || contrat.getStatutContrat() == null || contrat.getEmploye() == null) {
            return ResponseEntity.badRequest().build();
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(contratService.save(contrat));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Contrat> update(@PathVariable Long id, @RequestBody Contrat contrat) {
        if (contrat.getDateDebut() == null || contrat.getDateFin() == null
                || contrat.getTypeContrat() == null || contrat.getSalaire() <= 0
                || contrat.getStatutContrat() == null || contrat.getEmploye() == null) {
            return ResponseEntity.badRequest().build();
        }

        Contrat updatedContrat = contratService.update(id, contrat);
        return updatedContrat == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(updatedContrat);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        return contratService.delete(id)
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }
}