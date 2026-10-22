package com.mrtovo.backend.service.contrat;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.mrtovo.backend.entity.contrat.Contrat;
import com.mrtovo.backend.repository.contrat.ContratRepository;

@Service
public class ContratService {
    @Autowired
    private ContratRepository contratRepository;

    public List<Contrat> getAll() {
        return contratRepository.findAll();
    }

    public Contrat save(Contrat contrat) {
        return contratRepository.save(contrat);
    }

    public Contrat getById(Long id) {
        return contratRepository.findById(id).orElse(null);
    }

    public boolean delete(Long id) {
        if (!contratRepository.existsById(id)) {
            return false;
        }

        contratRepository.deleteById(id);
        return true;
    }

    public Contrat update(Long id, Contrat contrat) {
        Contrat existingContrat = getById(id);
        if (existingContrat == null) {
            return null;
        }

        existingContrat.setDateDebut(contrat.getDateDebut());
        existingContrat.setDateFin(contrat.getDateFin());
        existingContrat.setTypeContrat(contrat.getTypeContrat());
        existingContrat.setSalaire(contrat.getSalaire());
        existingContrat.setStatutContrat(contrat.getStatutContrat());
        existingContrat.setEmploye(contrat.getEmploye());
        return contratRepository.save(existingContrat);
    }
}