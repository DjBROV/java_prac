package ru.msu.cmc.prak.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import ru.msu.cmc.prak.DAO.*;
import ru.msu.cmc.prak.models.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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
        SuppliesDAO.Filter filter = SuppliesDAO.getFilterBuilder()
                .providerId(ControllerUtils.parseLongOrNull(providerId))
                .productName(productName)
                .amountFrom(ControllerUtils.parseIntOrNull(amountFrom))
                .amountTo(ControllerUtils.parseIntOrNull(amountTo))
                .timeFrom(ControllerUtils.parseDateTimeOrNull(timeFrom))
                .timeTo(ControllerUtils.parseDateTimeOrNull(timeTo))
                .completed(completed)
                .build();
        model.addAttribute("supplies", suppliesDAO.getByFilter(filter));
        return "supplies/list";
    }

    @GetMapping("/{id}")
    public String details(@PathVariable Long id, Model model) {
        Supplies supply = suppliesDAO.getById(id);
        if (supply == null) {
            throw new IllegalArgumentException("Поставка с id=" + id + " не найдена");
        }
        model.addAttribute("supply", supply);
        model.addAttribute("provider", suppliesDAO.getProvider(supply));
        model.addAttribute("units", suppliesDAO.getProductUnitsForSupply(supply));
        return "supplies/details";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("supply", new Supplies());
        model.addAttribute("providers", providersDAO.getAll());
        model.addAttribute("products", productsDAO.getAll());
        return "supplies/form";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        Supplies supply = suppliesDAO.getById(id);
        if (supply == null) {
            throw new IllegalArgumentException("Поставка с id=" + id + " не найдена");
        }
        model.addAttribute("supply", supply);
        model.addAttribute("providers", providersDAO.getAll());
        model.addAttribute("products", productsDAO.getAll());
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
        if (provider == null || product == null) {
            throw new IllegalArgumentException("Поставщик или товар не найден");
        }

        Supplies supply = new Supplies();
        supply.setId(id == null ? ControllerUtils.nextId(new ArrayList<>(suppliesDAO.getAll()), 1000) : id);
        supply.setProvider(provider);
        supply.setProduct(product);
        supply.setAmount(BigDecimal.valueOf(amount.longValue()));
        supply.setTime(ControllerUtils.parseDateTimeOrNull(time));
        supply.setCompleted(false);

        if (id == null) {
            int requiredWorkload = calcRequiredWorkload(product, amount);
            ShelfsWorkload shelf = reserveSpace(requiredWorkload, model);
            suppliesDAO.save(supply);
            createUnitForSupply(supply, amount, shelf);
        } else {
            Supplies existing = suppliesDAO.getById(id);
            if (existing == null) {
                throw new IllegalArgumentException("Поставка с id=" + id + " не найдена");
            }

            supply.setCompleted(existing.isCompleted());

            if (!existing.isCompleted()) {
                releaseReservedSpace(existing);
                int requiredWorkload = calcRequiredWorkload(product, amount);
                ShelfsWorkload shelf = reserveSpace(requiredWorkload, model);
                suppliesDAO.update(supply);
                recreateUnitsForSupply(supply, amount, shelf);
            } else {
                suppliesDAO.update(supply);
            }
        }
        return "redirect:/supplies";
    }

    @PostMapping("/{id}/complete")
    public String complete(@PathVariable Long id) {
        Supplies supply = suppliesDAO.getById(id);
        if (supply == null) {
            throw new IllegalArgumentException("Поставка с id=" + id + " не найдена");
        }
        supply.setCompleted(true);
        suppliesDAO.update(supply);
        return "redirect:/supplies/" + id;
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id) {
        Supplies supply = suppliesDAO.getById(id);
        if (supply == null) {
            return "redirect:/supplies";
        }
        if (supply.isCompleted()) {
            throw new IllegalArgumentException("Нельзя удалять выполненную поставку");
        }

        releaseReservedSpace(supply);
        suppliesDAO.deleteById(id);
        return "redirect:/supplies";
    }

    private void releaseReservedSpace(Supplies supply) {
        for (ProductUnits unit : suppliesDAO.getProductUnitsForSupply(supply)) {
            ShelfsWorkload shelf = unit.getShelf();
            int reservedLoad = calcRequiredWorkload(unit.getProduct(), unit.getAmount().intValue());
            int workloadAfterDelete = Math.max(0, shelf.getWorkloadCount() - reservedLoad);
            shelfsWorkloadDAO.updateWorkload(shelf, workloadAfterDelete);
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

    private ShelfsWorkload reserveSpace(int requiredWorkload, Model model) {
        List<ShelfsWorkload> shelves = shelfsWorkloadDAO.getShelvesWithFreeSpace(requiredWorkload);
        if (shelves.isEmpty()) {
            int totalFree = shelfsWorkloadDAO.getAll().stream()
                    .mapToInt(s -> SHELF_CAPACITY - s.getWorkloadCount())
                    .sum();

            model.addAttribute(
                    "errorMessage",
                    "Недостаточно места: свободно " + totalFree + ", требуется " + requiredWorkload
            );
            throw new IllegalArgumentException("Недостаточно свободного места для поставки");
        }

        ShelfsWorkload selected = shelves.getFirst();
        shelfsWorkloadDAO.updateWorkload(selected, selected.getWorkloadCount() + requiredWorkload);
        return selected;
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