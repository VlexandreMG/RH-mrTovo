package com.mrtovo.backend.repository;

import com.mrtovo.backend.entity.PosteRecrutement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository("bloc3PosteRepository")
public interface PosteRecrutementRepository extends JpaRepository<PosteRecrutement, Long> {

    List<PosteRecrutement> findByDepartementId(Long departementId);
}
