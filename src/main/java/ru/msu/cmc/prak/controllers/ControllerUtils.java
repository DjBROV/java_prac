package ru.msu.cmc.prak.controllers;

import ru.msu.cmc.prak.models.CommonEntity;

import java.time.LocalDateTime;
import java.util.Collection;

public final class ControllerUtils {
    private ControllerUtils() {
    }

    public static Long parseLongOrNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return Long.valueOf(value.trim());
    }

    public static Integer parseIntOrNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return Integer.valueOf(value.trim());
    }

    public static Long nextId(Collection<? extends CommonEntity<Long>> entities, long fallbackStart) {
        return entities.stream().map(CommonEntity::getId).filter(id -> id != null).max(Long::compareTo).orElse(fallbackStart) + 1;
    }

    public static LocalDateTime parseDateTimeOrNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return LocalDateTime.parse(value.trim());
    }
}