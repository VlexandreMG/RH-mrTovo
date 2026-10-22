package com.mrtovo.backend.controllers;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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
    @GetMapping("/all")
    public List<Contrat> getAll(Model model){
        return contratService.getAll();
    }
    @GetMapping("/{id}")
    public Contrat getById(@PathVariable Long id){
        return contratService.getById(id);
    }
    @PostMapping("/save")
    public Contrat save(@RequestBody Contrat contrat){
        return contratService.save(contrat);
    }
    @PostMapping("/delete/{id}")
    public void delete(@PathVariable Long id){
        contratService.delete(id);
    }
    @PostMapping("/update/{id}")
    public void update(@PathVariable Long id, @RequestBody Contrat contrat){
        contratService.update(id, contrat);
    }


}