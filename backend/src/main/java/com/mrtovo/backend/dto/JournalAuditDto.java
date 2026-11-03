package com.mrtovo.backend.dto;

import com.mrtovo.backend.entity.ActionAudit;

import java.time.LocalDateTime;

public record JournalAuditDto(
        Long id,
        ActionAudit action,
        String actionLibelle,
        String auteur,
        String details,
        LocalDateTime dateAction) {
}
