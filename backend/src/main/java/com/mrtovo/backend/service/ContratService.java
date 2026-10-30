package com.mrtovo.backend.service;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.mrtovo.backend.entity.contrat.Contrat;
import com.mrtovo.backend.repository.ContratRepository;
@Service
public class ContratService {
    @Autowired 
    ContratRepository contratRepository;
    public List<Contrat> getAll() {
        return contratRepository.findAll();
    }
    public Contrat save(Contrat contrat) {
        return contratRepository.save(contrat);
    }
    public Contrat getById(Long id) {
        return contratRepository.findById(id).orElse(null);
    }
    public void delete(Long id) {
        contratRepository.deleteById(id);
    }
    public void update(Long id, Contrat contrat) {
        Contrat existingContrat = contratRepository.findById(id).orElse(null);
        if (existingContrat != null) {
            existingContrat.setDateDebut(contrat.getDateDebut());
            existingContrat.setDateFin(contrat.getDateFin());
            existingContrat.setTypeContrat(contrat.getTypeContrat());
            existingContrat.setSalaire(contrat.getSalaire());
            existingContrat.setStatutContrat(contrat.getStatutContrat());
            existingContrat.setEmploye(contrat.getEmploye());
            contratRepository.save(existingContrat);
        }
    }

    public List<Contrat> getByEmployeId(Long employeId) {
        return contratRepository.findByEmployeId(employeId);
    }

    public List<Contrat> getByStatutContratId(Long statutContratId) {
        return contratRepository.findByStatutContratId(statutContratId);
    }

    public List<Contrat> getByEmployeIdAndStatutContratId(Long employeId, Long statutContratId) {
        return contratRepository.findByEmployeIdAndStatutContratId(employeId, statutContratId);
    }

    public List<Contrat> getFiltered(Long employeId, Long statutContratId) {
        if (employeId != null && statutContratId != null) {
            return getByEmployeIdAndStatutContratId(employeId, statutContratId);
        } else if (employeId != null) {
            return getByEmployeId(employeId);
        } else if (statutContratId != null) {
            return getByStatutContratId(statutContratId);
        }
        return getAll();
    }
}