package ru.msu.cmc.prak.controllers;

import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public String handleAnyException(Exception e, Model model) {
        if (!model.containsAttribute("errorMessage")) {
            model.addAttribute("errorMessage", e.getMessage());
        }
        return "error";
    }
}