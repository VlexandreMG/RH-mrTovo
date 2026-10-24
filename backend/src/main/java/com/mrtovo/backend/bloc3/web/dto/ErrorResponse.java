package com.mrtovo.backend.bloc3.web.dto;

import java.time.LocalDateTime;
import java.util.List;

public record ErrorResponse(
        LocalDateTime horodatage,
        int statut,
        String code,
        String message,
        List<String> details) {
}
