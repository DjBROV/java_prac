package ru.msu.cmc.prak.controllers;

import ru.msu.cmc.prak.controllers.exceptions.BadRequestException;
import ru.msu.cmc.prak.models.CommonEntity;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Collection;

public final class ControllerUtils {
    private ControllerUtils() {
    }

    public static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    public static String requireText(String value, String fieldName) {
        String normalized = blankToNull(value);
        if (normalized == null) {
            throw new BadRequestException("Поле '" + fieldName + "' обязательно для заполнения");
        }
        return normalized;
    }

    public static Long parseLongOrNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return Long.valueOf(value.trim());
        } catch (NumberFormatException e) {
            throw new BadRequestException("Значение '" + value + "' должно быть целым числом");
        }
    }

    public static Integer parseIntOrNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return Integer.valueOf(value.trim());
        } catch (NumberFormatException e) {
            throw new BadRequestException("Значение '" + value + "' должно быть целым числом");
        }
    }

    public static LocalDateTime parseDateTimeOrNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return LocalDateTime.parse(value.trim());
        } catch (DateTimeParseException e) {
            throw new BadRequestException("Значение '" + value + "' должно быть датой и временем");
        }
    }

    public static Long nextId(Collection<? extends CommonEntity<Long>> entities, long fallbackStart) {
        return entities.stream()
                .map(CommonEntity::getId)
                .filter(id -> id != null)
                .max(Long::compareTo)
                .orElse(fallbackStart) + 1;
    }
}