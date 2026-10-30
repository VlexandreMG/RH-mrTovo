package com.mrtovo.backend.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.mrtovo.backend.StatutContrat.StatutContrat;
import com.mrtovo.backend.repository.StatutContratRepository;

@Service
public class StatutContratService {
    @Autowired
    private StatutContratRepository statutContratRepository;

    public List<StatutContrat> getAll() {
        return statutContratRepository.findAll();
    }

    public StatutContrat getById(Long id) {
        return statutContratRepository.findById(id).orElse(null);
    }

    public StatutContrat save(StatutContrat statutContrat) {
        return statutContratRepository.save(statutContrat);
    }

    public StatutContrat update(Long id, StatutContrat statutContrat) {
        StatutContrat existingStatutContrat = getById(id);
        if (existingStatutContrat == null) {
            return null;
        }

        existingStatutContrat.setLibelle(statutContrat.getLibelle());
        return statutContratRepository.save(existingStatutContrat);
    }

    public boolean delete(Long id) {
        if (!statutContratRepository.existsById(id)) {
            return false;
        }

        statutContratRepository.deleteById(id);
        return true;
    }
}