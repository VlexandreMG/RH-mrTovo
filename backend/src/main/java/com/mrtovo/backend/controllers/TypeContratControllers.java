package com.mrtovo.backend.controllers;

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

import com.mrtovo.backend.entity.TypeContrat.TypeContrat;
import com.mrtovo.backend.service.TypeContratService;

@RestController
@RequestMapping("/api/typeContrat")
@CrossOrigin(origins = "http://localhost:5173")
public class TypeContratControllers {
    @Autowired
    private TypeContratService typeContratService;

    @GetMapping
    public List<TypeContrat> getAll() {
        return typeContratService.getAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<TypeContrat> getById(@PathVariable Long id) {
        TypeContrat typeContrat = typeContratService.getById(id);
        return typeContrat == null
                ? ResponseEntity.notFound().build()
                : ResponseEntity.ok(typeContrat);
    }

    @PostMapping
    public ResponseEntity<TypeContrat> save(@RequestBody TypeContrat typeContrat) {
        if (typeContrat.getLibelle() == null || typeContrat.getLibelle().isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(typeContratService.save(typeContrat));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TypeContrat> update(
            @PathVariable Long id,
            @RequestBody TypeContrat typeContrat) {
        if (typeContrat.getLibelle() == null || typeContrat.getLibelle().isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        TypeContrat updatedTypeContrat = typeContratService.update(id, typeContrat);
        return updatedTypeContrat == null
                ? ResponseEntity.notFound().build()
                : ResponseEntity.ok(updatedTypeContrat);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        return typeContratService.delete(id)
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }
}