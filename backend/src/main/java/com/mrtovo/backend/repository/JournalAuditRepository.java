package com.mrtovo.backend.repository;

import com.mrtovo.backend.entity.JournalAudit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JournalAuditRepository extends JpaRepository<JournalAudit, Long> {

    List<JournalAudit> findByEmployeIdOrderByDateActionDesc(Long employeId);
}
