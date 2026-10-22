package com.mrtovo.backend.controllers.statutContrat;

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

import com.mrtovo.backend.StatutContrat.StatutContrat;
import com.mrtovo.backend.service.statutContrat.StatutContratService;

@RestController
@RequestMapping("/api/statutContrat")
@CrossOrigin(origins = "http://localhost:5173")
public class StatutContratControllers {
    @Autowired
    private StatutContratService statutContratService;

    @GetMapping
    public List<StatutContrat> getAll() {
        return statutContratService.getAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<StatutContrat> getById(@PathVariable Long id) {
        StatutContrat statutContrat = statutContratService.getById(id);
        return statutContrat == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(statutContrat);
    }

    @PostMapping
    public ResponseEntity<StatutContrat> save(@RequestBody StatutContrat statutContrat) {
        if (statutContrat.getLibelle() == null || statutContrat.getLibelle().isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(statutContratService.save(statutContrat));
    }

    @PutMapping("/{id}")
    public ResponseEntity<StatutContrat> update(
            @PathVariable Long id,
            @RequestBody StatutContrat statutContrat) {
        if (statutContrat.getLibelle() == null || statutContrat.getLibelle().isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        StatutContrat updatedStatutContrat = statutContratService.update(id, statutContrat);
        return updatedStatutContrat == null
                ? ResponseEntity.notFound().build()
                : ResponseEntity.ok(updatedStatutContrat);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        return statutContratService.delete(id)
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }
}