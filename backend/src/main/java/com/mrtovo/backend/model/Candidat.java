package com.mrtovo.backend.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Entity
@Data
public class Candidat {
    Long id;
    String nom;
    String prenom;
    String email;
    String telephone;
    String fichierCvUrl;
}