package com.mrtovo.backend.service.typeContrat;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.mrtovo.backend.entity.TypeContrat.TypeContrat;
import com.mrtovo.backend.repository.typeContrat.TypeContratRepository;

@Service
public class TypeContratService {
    @Autowired
    private TypeContratRepository typeContratRepository;

    public List<TypeContrat> getAll() {
        return typeContratRepository.findAll();
    }

    public TypeContrat getById(Long id) {
        return typeContratRepository.findById(id).orElse(null);
    }

    public TypeContrat save(TypeContrat typeContrat) {
        return typeContratRepository.save(typeContrat);
    }

    public TypeContrat update(Long id, TypeContrat typeContrat) {
        TypeContrat existingTypeContrat = getById(id);
        if (existingTypeContrat == null) {
            return null;
        }

        existingTypeContrat.setLibelle(typeContrat.getLibelle());
        return typeContratRepository.save(existingTypeContrat);
    }

    public boolean delete(Long id) {
        if (!typeContratRepository.existsById(id)) {
            return false;
        }

        typeContratRepository.deleteById(id);
        return true;
    }
}