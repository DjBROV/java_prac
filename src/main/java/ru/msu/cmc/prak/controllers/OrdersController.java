package ru.msu.cmc.prak.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import ru.msu.cmc.prak.DAO.*;
import ru.msu.cmc.prak.controllers.exceptions.BadRequestException;
import ru.msu.cmc.prak.controllers.exceptions.BusinessRuleException;
import ru.msu.cmc.prak.controllers.exceptions.EntityNotFoundException;
import ru.msu.cmc.prak.models.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequiredArgsConstructor
@RequestMapping("/orders")
public class OrdersController {
    private final OrdersDAO ordersDAO;
    private final ConsumersDAO consumersDAO;
    private final ProductsDAO productsDAO;
    private final ProductUnitsDAO productUnitsDAO;

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

        if (order.isCompleted()) {
            throw new BusinessRuleException("Нельзя редактировать выполненный заказ");
        }

        model.addAttribute("order", order);
        addOrderFormAttributes(model);
        return "orders/form";
    }

    @PostMapping("/save")
    public String save(@RequestParam(required = false) Long id,
                       @RequestParam Long consumerId,
                       @RequestParam Long productId,
                       @RequestParam Integer amount,
                       @RequestParam String time,
                       Model model) {
        Consumers consumer = consumersDAO.getById(consumerId);
        Products product = productsDAO.getById(productId);

        if (consumer == null) {
            throw new EntityNotFoundException("Потребитель с id=" + consumerId + " не найден");
        }

        if (product == null) {
            throw new EntityNotFoundException("Товар с id=" + productId + " не найден");
        }

        if (amount == null || amount <= 0) {
            return orderFormError(model, buildOrder(id, consumer, product, amount, time),
                    "Количество товара в заказе должно быть положительным");
        }

        Orders existing = null;
        if (id != null) {
            existing = getOrderOrThrow(id);

            if (existing.isCompleted()) {
                throw new BusinessRuleException("Нельзя редактировать выполненный заказ");
            }
        }

        Orders order = buildOrder(id, consumer, product, amount, time);

        BigDecimal available = getAvailableAmountForOrder(existing, productId);
        BigDecimal required = BigDecimal.valueOf(amount.longValue());

        if (available.compareTo(required) < 0) {
            return orderFormError(model, order,
                    "Недостаточно товара для заказа. Доступно: " + available + ", нужно: " + amount);
        }

        if (id == null) {
            order.setCompleted(false);
            ordersDAO.save(order);

            List<ProductUnits> freeUnits = getFreeUnitsForProduct(productId);
            reserveUnitsForOrder(order, freeUnits, required);
        } else {
            releaseReservation(existing);

            order.setCompleted(false);
            ordersDAO.update(order);

            List<ProductUnits> freeUnits = getFreeUnitsForProduct(productId);
            reserveUnitsForOrder(order, freeUnits, required);
        }

        return "redirect:/orders";
    }

    @PostMapping("/{id}/complete")
    public String complete(@PathVariable Long id) {
        Orders order = getOrderOrThrow(id);

        if (order.isCompleted()) {
            throw new BusinessRuleException("Заказ уже выполнен");
        }

        order.setCompleted(true);
        ordersDAO.update(order);

        for (ProductUnits unit : ordersDAO.getProductUnitsForOrder(order)) {
            productUnitsDAO.deleteById(unit.getId());
        }

        return "redirect:/orders/" + id;
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id) {
        Orders order = getOrderOrThrow(id);

        if (!order.isCompleted()) {
            releaseReservation(order);
        }

        ordersDAO.deleteById(id);
        return "redirect:/orders";
    }

    private Orders getOrderOrThrow(Long id) {
        Orders order = ordersDAO.getById(id);
        if (order == null) {
            throw new EntityNotFoundException("Заказ с id=" + id + " не найден");
        }
        return order;
    }

    private Orders buildOrder(Long id, Consumers consumer, Products product, Integer amount, String time) {
        Orders order = new Orders();
        order.setId(id == null ? ControllerUtils.nextId(new ArrayList<>(ordersDAO.getAll()), 1000) : id);
        order.setConsumer(consumer);
        order.setProduct(product);
        order.setAmount(amount == null ? null : BigDecimal.valueOf(amount.longValue()));
        order.setTime(ControllerUtils.parseDateTimeOrNull(time));
        return order;
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

    private BigDecimal getAvailableAmountForOrder(Orders existingOrder, Long productId) {
        BigDecimal available = getFreeUnitsForProduct(productId).stream()
                .map(ProductUnits::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (existingOrder != null && !existingOrder.isCompleted()) {
            BigDecimal alreadyReservedForThisOrder = ordersDAO.getProductUnitsForOrder(existingOrder).stream()
                    .filter(unit -> unit.getProduct() != null)
                    .filter(unit -> productId.equals(unit.getProduct().getId()))
                    .map(ProductUnits::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            available = available.add(alreadyReservedForThisOrder);
        }

        return available;
    }

    private List<ProductUnits> getFreeUnitsForProduct(Long productId) {
        return productUnitsDAO.getByFilter(ProductUnitsDAO.getFilterBuilder()
                .productId(productId)
                .reserved(false)
                .build());
    }

    private void releaseReservation(Orders order) {
        List<ProductUnits> reservedUnits = ordersDAO.getProductUnitsForOrder(order);

        for (ProductUnits unit : reservedUnits) {
            if (unit.getSupply() == null || unit.getShelf() == null || unit.getProduct() == null) {
                unit.setOrder(null);
                productUnitsDAO.update(unit);
                continue;
            }

            List<ProductUnits> matchingFreeUnits = productUnitsDAO.getByFilter(ProductUnitsDAO.getFilterBuilder()
                    .productId(unit.getProduct().getId())
                    .supplyId(unit.getSupply().getId())
                    .shelfNum(unit.getShelf().getId())
                    .reserved(false)
                    .build());

            if (!matchingFreeUnits.isEmpty()) {
                ProductUnits freeUnit = matchingFreeUnits.getFirst();
                freeUnit.setAmount(freeUnit.getAmount().add(unit.getAmount()));
                productUnitsDAO.update(freeUnit);
                productUnitsDAO.deleteById(unit.getId());
            } else {
                unit.setOrder(null);
                productUnitsDAO.update(unit);
            }
        }
    }

    private void reserveUnitsForOrder(Orders order, List<ProductUnits> freeUnits, BigDecimal requiredAmount) {
        BigDecimal remaining = requiredAmount;
        long nextId = ControllerUtils.nextId(new ArrayList<>(productUnitsDAO.getAll()), 10000);

        for (ProductUnits unit : freeUnits) {
            if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }

            if (unit.getAmount().compareTo(remaining) <= 0) {
                unit.setOrder(order);
                productUnitsDAO.update(unit);
                remaining = remaining.subtract(unit.getAmount());
            } else {
                ProductUnits reservedPart = new ProductUnits();
                reservedPart.setId(nextId++);
                reservedPart.setProduct(unit.getProduct());
                reservedPart.setArrival(unit.getArrival());
                reservedPart.setAmount(remaining);
                reservedPart.setShelf(unit.getShelf());
                reservedPart.setSupply(unit.getSupply());
                reservedPart.setOrder(order);
                productUnitsDAO.save(reservedPart);

                unit.setAmount(unit.getAmount().subtract(remaining));
                productUnitsDAO.update(unit);
                remaining = BigDecimal.ZERO;
            }
        }
    }
}