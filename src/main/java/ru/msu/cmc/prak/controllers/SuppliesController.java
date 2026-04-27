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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
@RequestMapping("/supplies")
public class SuppliesController {
    private static final int SHELF_CAPACITY = 500;

    private final SuppliesDAO suppliesDAO;
    private final ProvidersDAO providersDAO;
    private final ProductsDAO productsDAO;
    private final ProductUnitsDAO productUnitsDAO;
    private final ShelfsWorkloadDAO shelfsWorkloadDAO;

    @GetMapping
    public String list(@RequestParam(required = false) String providerId,
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

        SuppliesDAO.Filter filter = SuppliesDAO.getFilterBuilder()
                .providerId(ControllerUtils.parseLongOrNull(providerId))
                .productName(ControllerUtils.blankToNull(productName))
                .amountFrom(parsedAmountFrom)
                .amountTo(parsedAmountTo)
                .timeFrom(parsedTimeFrom)
                .timeTo(parsedTimeTo)
                .completed(completed)
                .build();

        model.addAttribute("supplies", suppliesDAO.getByFilter(filter));
        return "supplies/list";
    }

    @GetMapping("/{id}")
    public String details(@PathVariable Long id, Model model) {
        Supplies supply = getSupplyOrThrow(id);

        model.addAttribute("supply", supply);
        model.addAttribute("provider", suppliesDAO.getProvider(supply));
        model.addAttribute("units", suppliesDAO.getProductUnitsForSupply(supply));
        return "supplies/details";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("supply", new Supplies());
        addSupplyFormAttributes(model);
        return "supplies/form";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        Supplies supply = getSupplyOrThrow(id);

        model.addAttribute("supply", supply);
        addSupplyFormAttributes(model);
        return "supplies/form";
    }

    @PostMapping("/save")
    public String save(@RequestParam(required = false) Long id,
                       @RequestParam Long providerId,
                       @RequestParam Long productId,
                       @RequestParam Integer amount,
                       @RequestParam String time,
                       Model model) {
        Providers provider = providersDAO.getById(providerId);
        Products product = productsDAO.getById(productId);

        if (provider == null) {
            throw new EntityNotFoundException("Поставщик с id=" + providerId + " не найден");
        }

        if (product == null) {
            throw new EntityNotFoundException("Товар с id=" + productId + " не найден");
        }

        if (amount == null || amount <= 0) {
            return supplyFormError(model, buildSupply(id, provider, product, amount, time),
                    "Количество товара в поставке должно быть положительным");
        }

        Supplies supply = buildSupply(id, provider, product, amount, time);
        Supplies existing = null;

        if (id != null) {
            existing = getSupplyOrThrow(id);
            supply.setCompleted(existing.isCompleted());

            if (existing.isCompleted()) {
                suppliesDAO.update(supply);
                return "redirect:/supplies";
            }
        } else {
            supply.setCompleted(false);
        }

        int requiredWorkload = calcRequiredWorkload(product, amount);
        ShelfsWorkload selectedShelf = findShelfForSupply(existing, requiredWorkload);

        if (selectedShelf == null) {
            int totalFree = calcTotalFreeWorkload(existing);
            return supplyFormError(model, supply,
                    "Недостаточно места: свободно " + totalFree + ", требуется " + requiredWorkload);
        }

        if (id == null) {
            reserveSpaceOnShelf(selectedShelf, requiredWorkload);
            suppliesDAO.save(supply);
            createUnitForSupply(supply, amount, selectedShelf);
        } else {
            releaseReservedSpace(existing);

            ShelfsWorkload refreshedShelf = shelfsWorkloadDAO.getById(selectedShelf.getId());
            reserveSpaceOnShelf(refreshedShelf, requiredWorkload);

            suppliesDAO.update(supply);
            recreateUnitsForSupply(supply, amount, refreshedShelf);
        }

        return "redirect:/supplies";
    }

    @PostMapping("/{id}/complete")
    public String complete(@PathVariable Long id) {
        Supplies supply = getSupplyOrThrow(id);

        supply.setCompleted(true);
        suppliesDAO.update(supply);
        return "redirect:/supplies/" + id;
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id) {
        Supplies supply = getSupplyOrThrow(id);

        if (supply.isCompleted()) {
            throw new BusinessRuleException("Нельзя удалять выполненную поставку");
        }

        releaseReservedSpace(supply);
        suppliesDAO.deleteById(id);
        return "redirect:/supplies";
    }

    private Supplies getSupplyOrThrow(Long id) {
        Supplies supply = suppliesDAO.getById(id);
        if (supply == null) {
            throw new EntityNotFoundException("Поставка с id=" + id + " не найдена");
        }
        return supply;
    }

    private Supplies buildSupply(Long id, Providers provider, Products product, Integer amount, String time) {
        Supplies supply = new Supplies();
        supply.setId(id == null ? ControllerUtils.nextId(new ArrayList<>(suppliesDAO.getAll()), 1000) : id);
        supply.setProvider(provider);
        supply.setProduct(product);
        supply.setAmount(amount == null ? null : BigDecimal.valueOf(amount.longValue()));
        supply.setTime(ControllerUtils.parseDateTimeOrNull(time));
        return supply;
    }

    private String supplyFormError(Model model, Supplies supply, String message) {
        model.addAttribute("errorMessage", message);
        model.addAttribute("supply", supply);
        addSupplyFormAttributes(model);
        return "supplies/form";
    }

    private void addSupplyFormAttributes(Model model) {
        model.addAttribute("providers", providersDAO.getAll());
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

    private void releaseReservedSpace(Supplies supply) {
        for (ProductUnits unit : suppliesDAO.getProductUnitsForSupply(supply)) {
            ShelfsWorkload shelf = unit.getShelf();

            if (shelf != null && unit.getProduct() != null && unit.getAmount() != null) {
                int reservedLoad = calcRequiredWorkload(unit.getProduct(), unit.getAmount().intValue());
                int workloadAfterDelete = Math.max(0, shelf.getWorkloadCount() - reservedLoad);
                shelfsWorkloadDAO.updateWorkload(shelf, workloadAfterDelete);
            }

            productUnitsDAO.deleteById(unit.getId());
        }
    }

    private void recreateUnitsForSupply(Supplies supply, Integer amount, ShelfsWorkload shelf) {
        createUnitForSupply(supply, amount, shelf);
    }

    private int calcRequiredWorkload(Products product, int amount) {
        return amount * getSizeWeight(product);
    }

    private int getSizeWeight(Products product) {
        if (product == null || product.getProduct_size() == null) {
            return 2;
        }

        return switch (product.getProduct_size()) {
            case small -> 2;
            case middle -> 10;
            case large -> 50;
        };
    }

    private ShelfsWorkload findShelfForSupply(Supplies existingSupply, int requiredWorkload) {
        Map<Long, Integer> workloadToBeReleased = calcWorkloadToBeReleasedByShelf(existingSupply);

        for (ShelfsWorkload shelf : shelfsWorkloadDAO.getAll()) {
            int currentFree = SHELF_CAPACITY - shelf.getWorkloadCount();
            int additionalFreeAfterEditing = workloadToBeReleased.getOrDefault(shelf.getId(), 0);
            int available = currentFree + additionalFreeAfterEditing;

            if (available >= requiredWorkload) {
                return shelf;
            }
        }

        return null;
    }

    private int calcTotalFreeWorkload(Supplies existingSupply) {
        Map<Long, Integer> workloadToBeReleased = calcWorkloadToBeReleasedByShelf(existingSupply);

        return shelfsWorkloadDAO.getAll().stream()
                .mapToInt(shelf -> SHELF_CAPACITY
                        - shelf.getWorkloadCount()
                        + workloadToBeReleased.getOrDefault(shelf.getId(), 0))
                .sum();
    }

    private Map<Long, Integer> calcWorkloadToBeReleasedByShelf(Supplies existingSupply) {
        Map<Long, Integer> result = new HashMap<>();

        if (existingSupply == null || existingSupply.isCompleted()) {
            return result;
        }

        for (ProductUnits unit : suppliesDAO.getProductUnitsForSupply(existingSupply)) {
            if (unit.getShelf() == null || unit.getProduct() == null || unit.getAmount() == null) {
                continue;
            }

            Long shelfId = unit.getShelf().getId();
            int workload = calcRequiredWorkload(unit.getProduct(), unit.getAmount().intValue());

            result.merge(shelfId, workload, Integer::sum);
        }

        return result;
    }

    private void reserveSpaceOnShelf(ShelfsWorkload selected, int requiredWorkload) {
        if (selected == null) {
            throw new BusinessRuleException("Не удалось выбрать полку для поставки");
        }

        shelfsWorkloadDAO.updateWorkload(selected, selected.getWorkloadCount() + requiredWorkload);
    }

    private void createUnitForSupply(Supplies supply, Integer amount, ShelfsWorkload selected) {
        ProductUnits unit = new ProductUnits();
        unit.setId(ControllerUtils.nextId(new ArrayList<>(productUnitsDAO.getAll()), 10000));
        unit.setProduct(supply.getProduct());
        unit.setArrival(supply.getTime() == null ? LocalDateTime.now() : supply.getTime());
        unit.setAmount(BigDecimal.valueOf(amount.longValue()));
        unit.setShelf(selected);
        unit.setSupply(supply);
        unit.setOrder(null);

        productUnitsDAO.save(unit);
    }
}