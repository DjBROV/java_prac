package ru.msu.cmc.prak.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import ru.msu.cmc.prak.DAO.ConsumersDAO;
import ru.msu.cmc.prak.DAO.OrdersDAO;
import ru.msu.cmc.prak.DAO.ProductsDAO;
import ru.msu.cmc.prak.controllers.exceptions.BadRequestException;
import ru.msu.cmc.prak.controllers.exceptions.BusinessRuleException;
import ru.msu.cmc.prak.controllers.exceptions.EntityNotFoundException;
import ru.msu.cmc.prak.models.Consumers;
import ru.msu.cmc.prak.models.Orders;
import ru.msu.cmc.prak.models.Products;
import ru.msu.cmc.prak.services.OrdersService;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Controller
@RequiredArgsConstructor
@RequestMapping("/orders")
public class OrdersController {
    private final OrdersDAO ordersDAO;
    private final ConsumersDAO consumersDAO;
    private final ProductsDAO productsDAO;
    private final OrdersService ordersService;

    @GetMapping
    public String list(@RequestParam(required = false) String consumerId,
                       @RequestParam(required = false) String productName,
                       @RequestParam(required = false) String amountFrom,
                       @RequestParam(required = false) String amountTo,
                       @RequestParam(required = false) String timeFrom,
                       @RequestParam(required = false) String timeTo,
                       @RequestParam(required = false) Boolean completed,
                       Model model) {
        Integer parsedAmountFrom = ControllerUtils.parseIntOrNull(amountFrom);
        Integer parsedAmountTo = ControllerUtils.parseIntOrNull(amountTo);
        LocalDateTime parsedTimeFrom = ControllerUtils.parseDateTimeOrNull(timeFrom);
        LocalDateTime parsedTimeTo = ControllerUtils.parseDateTimeOrNull(timeTo);

        validateRange(parsedAmountFrom, parsedAmountTo, parsedTimeFrom, parsedTimeTo);

        OrdersDAO.Filter filter = OrdersDAO.getFilterBuilder()
                .consumerId(ControllerUtils.parseLongOrNull(consumerId))
                .productName(ControllerUtils.blankToNull(productName))
                .amountFrom(parsedAmountFrom)
                .amountTo(parsedAmountTo)
                .timeFrom(parsedTimeFrom)
                .timeTo(parsedTimeTo)
                .completed(completed)
                .build();

        model.addAttribute("orders", ordersDAO.getByFilter(filter));
        return "orders/list";
    }

    @GetMapping("/{id}")
    public String details(@PathVariable Long id, Model model) {
        Orders order = getOrderOrThrow(id);

        model.addAttribute("order", order);
        model.addAttribute("consumer", ordersDAO.getConsumer(order));
        model.addAttribute("units", ordersDAO.getProductUnitsForOrder(order));
        return "orders/details";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("order", new Orders());
        addOrderFormAttributes(model);
        return "orders/form";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        Orders order = getOrderOrThrow(id);
        ensureOrderIsEditable(order);

        model.addAttribute("order", order);
        addOrderFormAttributes(model);
        return "orders/form";
    }

    @PostMapping("/save")
    public String save(@RequestParam(required = false) Long id,
                       @RequestParam Long consumerId,
                       @RequestParam Long productId,
                       @RequestParam(required = false) Integer amount,
                       @RequestParam String time,
                       Model model) {
        Consumers consumer = getConsumerOrThrow(consumerId);
        Products product = getProductOrThrow(productId);

        if (amount == null || amount <= 0) {
            Orders formOrder = ordersService.buildOrder(id, consumer, product, amount, time);
            return orderFormError(model, formOrder, "Количество товара в заказе должно быть положительным");
        }

        Orders existing = null;
        if (id != null) {
            existing = getOrderOrThrow(id);
            ensureOrderIsEditable(existing);
        }

        Orders order = ordersService.buildOrder(id, consumer, product, amount, time);
        BigDecimal required = BigDecimal.valueOf(amount.longValue());
        BigDecimal available = ordersService.getAvailableAmountForOrder(existing, productId);

        if (available.compareTo(required) < 0) {
            return orderFormError(
                    model,
                    order,
                    "Недостаточно товара для заказа. Доступно: " + available + ", нужно: " + amount
            );
        }

        if (id == null) {
            ordersService.createOrder(order, productId, required);
        } else {
            ordersService.updateOrder(existing, order, productId, required);
        }

        return "redirect:/orders";
    }

    @PostMapping("/{id}/complete")
    public String complete(@PathVariable Long id) {
        Orders order = getOrderOrThrow(id);

        if (order.isCompleted()) {
            throw new BusinessRuleException("Заказ уже выполнен");
        }

        ordersService.completeOrder(order);
        return "redirect:/orders/" + id;
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id) {
        Orders order = getOrderOrThrow(id);
        ordersService.deleteOrder(order);
        return "redirect:/orders";
    }

    private Orders getOrderOrThrow(Long id) {
        Orders order = ordersDAO.getById(id);
        if (order == null) {
            throw new EntityNotFoundException("Заказ с id=" + id + " не найден");
        }
        return order;
    }

    private Consumers getConsumerOrThrow(Long id) {
        Consumers consumer = consumersDAO.getById(id);
        if (consumer == null) {
            throw new EntityNotFoundException("Потребитель с id=" + id + " не найден");
        }
        return consumer;
    }

    private Products getProductOrThrow(Long id) {
        Products product = productsDAO.getById(id);
        if (product == null) {
            throw new EntityNotFoundException("Товар с id=" + id + " не найден");
        }
        return product;
    }

    private void ensureOrderIsEditable(Orders order) {
        if (order.isCompleted()) {
            throw new BusinessRuleException("Нельзя редактировать выполненный заказ");
        }
    }

    private String orderFormError(Model model, Orders order, String message) {
        model.addAttribute("errorMessage", message);
        model.addAttribute("order", order);
        addOrderFormAttributes(model);
        return "orders/form";
    }

    private void addOrderFormAttributes(Model model) {
        model.addAttribute("consumers", consumersDAO.getAll());
        model.addAttribute("products", productsDAO.getAll());
    }

    private void validateRange(Integer amountFrom,
                               Integer amountTo,
                               LocalDateTime timeFrom,
                               LocalDateTime timeTo) {
        if (amountFrom != null && amountTo != null && amountFrom > amountTo) {
            throw new BadRequestException("Минимальное количество не может быть больше максимального");
        }

        if (timeFrom != null && timeTo != null && timeFrom.isAfter(timeTo)) {
            throw new BadRequestException("Начало периода не может быть позже конца периода");
        }
    }
}