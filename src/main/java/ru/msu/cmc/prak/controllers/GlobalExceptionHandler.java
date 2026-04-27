package ru.msu.cmc.prak.controllers;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import ru.msu.cmc.prak.controllers.exceptions.BadRequestException;
import ru.msu.cmc.prak.controllers.exceptions.BusinessRuleException;
import ru.msu.cmc.prak.controllers.exceptions.EntityNotFoundException;

import java.time.format.DateTimeParseException;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(EntityNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleNotFound(EntityNotFoundException ex,
                                 HttpServletRequest request,
                                 Model model) {
        model.addAttribute("errorTitle", "Объект не найден");
        model.addAttribute("errorMessage", ex.getMessage());
        model.addAttribute("path", request.getRequestURI());
        return "error";
    }

    @ExceptionHandler(BusinessRuleException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String handleBusinessRule(BusinessRuleException ex,
                                     HttpServletRequest request,
                                     Model model) {
        model.addAttribute("errorTitle", "Операция невозможна");
        model.addAttribute("errorMessage", ex.getMessage());
        model.addAttribute("path", request.getRequestURI());
        return "error";
    }

    @ExceptionHandler({
            BadRequestException.class,
            NumberFormatException.class,
            DateTimeParseException.class,
            MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class
    })
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String handleBadRequest(Exception ex,
                                   HttpServletRequest request,
                                   Model model) {
        model.addAttribute("errorTitle", "Некорректные данные");
        model.addAttribute("errorMessage", "Проверьте введённые значения: числа, даты и обязательные поля.");
        model.addAttribute("details", ex.getMessage());
        model.addAttribute("path", request.getRequestURI());
        return "error";
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public String handleDataIntegrity(DataIntegrityViolationException ex,
                                      HttpServletRequest request,
                                      Model model) {
        model.addAttribute("errorTitle", "Нарушение связей в базе данных");
        model.addAttribute("errorMessage",
                "Операцию нельзя выполнить: запись связана с другими данными.");
        model.addAttribute("path", request.getRequestURI());
        return "error";
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String handleUnexpected(Exception ex,
                                   HttpServletRequest request,
                                   Model model) {
        model.addAttribute("errorTitle", "Внутренняя ошибка");
        model.addAttribute("errorMessage", "Произошла непредвиденная ошибка.");
        model.addAttribute("details", ex.getMessage());
        model.addAttribute("path", request.getRequestURI());
        return "error";
    }
}