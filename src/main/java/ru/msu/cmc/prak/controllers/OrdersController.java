package ru.msu.cmc.prak.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import ru.msu.cmc.prak.DAO.*;
import ru.msu.cmc.prak.models.*;

import java.math.BigDecimal;
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
        OrdersDAO.Filter filter = OrdersDAO.getFilterBuilder()
                .consumerId(ControllerUtils.parseLongOrNull(consumerId))
                .productName(productName)
                .amountFrom(ControllerUtils.parseIntOrNull(amountFrom))
                .amountTo(ControllerUtils.parseIntOrNull(amountTo))
                .timeFrom(ControllerUtils.parseDateTimeOrNull(timeFrom))
                .timeTo(ControllerUtils.parseDateTimeOrNull(timeTo))
                .completed(completed)
                .build();
        model.addAttribute("orders", ordersDAO.getByFilter(filter));
        return "orders/list";
    }

    @GetMapping("/{id}")
    public String details(@PathVariable Long id, Model model) {
        Orders order = ordersDAO.getById(id);
        if (order == null) {
            throw new IllegalArgumentException("Заказ с id=" + id + " не найден");
        }
        model.addAttribute("order", order);
        model.addAttribute("consumer", ordersDAO.getConsumer(order));
        model.addAttribute("units", ordersDAO.getProductUnitsForOrder(order));
        return "orders/details";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("order", new Orders());
        model.addAttribute("consumers", consumersDAO.getAll());
        model.addAttribute("products", productsDAO.getAll());
        return "orders/form";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        Orders order = ordersDAO.getById(id);
        if (order == null) {
            throw new IllegalArgumentException("Заказ с id=" + id + " не найден");
        }
        model.addAttribute("order", order);
        model.addAttribute("consumers", consumersDAO.getAll());
        model.addAttribute("products", productsDAO.getAll());
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
        if (consumer == null || product == null) {
            throw new IllegalArgumentException("Потребитель или товар не найден");
        }

        Orders order = new Orders();
        order.setId(id == null ? ControllerUtils.nextId(new ArrayList<>(ordersDAO.getAll()), 1000) : id);
        order.setConsumer(consumer);
        order.setProduct(product);
        order.setAmount(BigDecimal.valueOf(amount.longValue()));
        order.setTime(ControllerUtils.parseDateTimeOrNull(time));

        if (id == null) {
            ensureEnoughFreeProduct(productId, amount, model);
            order.setCompleted(false);
            ordersDAO.save(order);

            List<ProductUnits> freeUnits = getFreeUnitsForProduct(productId);
            reserveUnitsForOrder(order, freeUnits, BigDecimal.valueOf(amount.longValue()));
        } else {
            Orders existing = ordersDAO.getById(id);
            if (existing == null) {
                throw new IllegalArgumentException("Заказ с id=" + id + " не найден");
            }

            // Снимаем старый резерв, если заказ ещё не выполнен
            if (!existing.isCompleted()) {
                releaseReservation(existing);
                ensureEnoughFreeProduct(productId, amount, model);
            }

            order.setCompleted(existing.isCompleted());
            ordersDAO.update(order);

            if (!order.isCompleted()) {
                List<ProductUnits> freeUnits = getFreeUnitsForProduct(productId);
                reserveUnitsForOrder(order, freeUnits, BigDecimal.valueOf(amount.longValue()));
            }
        }
        return "redirect:/orders";
    }

    @PostMapping("/{id}/complete")
    public String complete(@PathVariable Long id) {
        Orders order = ordersDAO.getById(id);
        if (order == null) {
            throw new IllegalArgumentException("Заказ с id=" + id + " не найден");
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
        Orders order = ordersDAO.getById(id);
        if (order == null) {
            return "redirect:/orders";
        }

        if (!order.isCompleted()) {
            releaseReservation(order);
        }

        ordersDAO.deleteById(id);
        return "redirect:/orders";
    }

    private void ensureEnoughFreeProduct(Long productId, Integer amount, Model model) {
        List<ProductUnits> freeUnits = getFreeUnitsForProduct(productId);
        BigDecimal available = freeUnits.stream()
                .map(ProductUnits::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (available.compareTo(BigDecimal.valueOf(amount.longValue())) < 0) {
            model.addAttribute("errorMessage",
                    "Недостаточно товара для заказа. Доступно: " + available + ", нужно: " + amount);
            throw new IllegalArgumentException("Недостаточно товара для заказа");
        }
    }

    private List<ProductUnits> getFreeUnitsForProduct(Long productId) {
        return productUnitsDAO.getByFilter(ProductUnitsDAO.getFilterBuilder()
                .productId(productId)
                .reserved(false)
                .build());
    }

    private void releaseReservation(Orders order) {
        List<ProductUnits> reservedUnits = ordersDAO.getProductUnitsForOrder(order);
        long nextId = ControllerUtils.nextId(new ArrayList<>(productUnitsDAO.getAll()), 10000);

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
                if (unit.getId() == null) {
                    unit.setId(nextId++);
                }
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