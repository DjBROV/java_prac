package ru.msu.cmc.prak.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import ru.msu.cmc.prak.DAO.ProductsDAO;
import ru.msu.cmc.prak.DAO.ProvidersDAO;
import ru.msu.cmc.prak.DAO.SuppliesDAO;
import ru.msu.cmc.prak.controllers.exceptions.BadRequestException;
import ru.msu.cmc.prak.controllers.exceptions.BusinessRuleException;
import ru.msu.cmc.prak.controllers.exceptions.EntityNotFoundException;
import ru.msu.cmc.prak.models.Products;
import ru.msu.cmc.prak.models.Providers;
import ru.msu.cmc.prak.models.ShelfsWorkload;
import ru.msu.cmc.prak.models.Supplies;
import ru.msu.cmc.prak.services.SuppliesService;

import java.time.LocalDateTime;

@Controller
@RequiredArgsConstructor
@RequestMapping("/supplies")
public class SuppliesController {
    private final SuppliesDAO suppliesDAO;
    private final ProvidersDAO providersDAO;
    private final ProductsDAO productsDAO;
    private final SuppliesService suppliesService;

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
        ensureSupplyIsEditable(supply);

        model.addAttribute("supply", supply);
        addSupplyFormAttributes(model);
        return "supplies/form";
    }

    @PostMapping("/save")
    public String save(@RequestParam(required = false) Long id,
                       @RequestParam Long providerId,
                       @RequestParam Long productId,
                       @RequestParam(required = false) Integer amount,
                       @RequestParam String time,
                       Model model) {
        Providers provider = getProviderOrThrow(providerId);
        Products product = getProductOrThrow(productId);

        if (amount == null || amount <= 0) {
            Supplies formSupply = suppliesService.buildSupply(id, provider, product, amount, time);
            return supplyFormError(model, formSupply, "Количество товара в поставке должно быть положительным");
        }

        Supplies existing = null;
        if (id != null) {
            existing = getSupplyOrThrow(id);
            ensureSupplyIsEditable(existing);
        }

        Supplies supply = suppliesService.buildSupply(id, provider, product, amount, time);
        supply.setCompleted(false);

        int requiredWorkload = suppliesService.calcRequiredWorkload(product, amount);
        ShelfsWorkload selectedShelf = suppliesService.findShelfForSupply(existing, requiredWorkload);

        if (selectedShelf == null) {
            int totalFree = suppliesService.calcTotalFreeWorkload(existing);
            return supplyFormError(
                    model,
                    supply,
                    "Недостаточно места: свободно " + totalFree + ", требуется " + requiredWorkload
            );
        }

        if (id == null) {
            suppliesService.createSupply(supply, amount, selectedShelf, requiredWorkload);
        } else {
            suppliesService.updateSupply(existing, supply, amount, selectedShelf, requiredWorkload);
        }

        return "redirect:/supplies";
    }

    @PostMapping("/{id}/complete")
    public String complete(@PathVariable Long id) {
        Supplies supply = getSupplyOrThrow(id);

        if (supply.isCompleted()) {
            throw new BusinessRuleException("Поставка уже выполнена");
        }

        suppliesService.completeSupply(supply);
        return "redirect:/supplies/" + id;
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id) {
        Supplies supply = getSupplyOrThrow(id);

        if (supply.isCompleted()) {
            throw new BusinessRuleException("Нельзя удалять выполненную поставку");
        }

        suppliesService.deleteSupply(supply);
        return "redirect:/supplies";
    }

    private Supplies getSupplyOrThrow(Long id) {
        Supplies supply = suppliesDAO.getById(id);
        if (supply == null) {
            throw new EntityNotFoundException("Поставка с id=" + id + " не найдена");
        }
        return supply;
    }

    private Providers getProviderOrThrow(Long id) {
        Providers provider = providersDAO.getById(id);
        if (provider == null) {
            throw new EntityNotFoundException("Поставщик с id=" + id + " не найден");
        }
        return provider;
    }

    private Products getProductOrThrow(Long id) {
        Products product = productsDAO.getById(id);
        if (product == null) {
            throw new EntityNotFoundException("Товар с id=" + id + " не найден");
        }
        return product;
    }

    private void ensureSupplyIsEditable(Supplies supply) {
        if (supply.isCompleted()) {
            throw new BusinessRuleException("Нельзя редактировать выполненную поставку");
        }
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
}