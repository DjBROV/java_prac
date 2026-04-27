package ru.msu.cmc.prak.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import ru.msu.cmc.prak.DAO.ProductCategoriesDAO;
import ru.msu.cmc.prak.DAO.ProductUnitsDAO;
import ru.msu.cmc.prak.DAO.ProductsDAO;
import ru.msu.cmc.prak.controllers.exceptions.BadRequestException;
import ru.msu.cmc.prak.controllers.exceptions.BusinessRuleException;
import ru.msu.cmc.prak.controllers.exceptions.EntityNotFoundException;
import ru.msu.cmc.prak.models.*;

import java.time.Duration;
import java.util.ArrayList;

@Controller
@RequiredArgsConstructor
@RequestMapping("/products")
public class ProductsController {
    private final ProductsDAO productsDAO;
    private final ProductCategoriesDAO categoriesDAO;
    private final ProductUnitsDAO productUnitsDAO;

    @GetMapping
    public String list(@RequestParam(required = false) String id,
                       @RequestParam(required = false) String name,
                       @RequestParam(required = false) String categoryId,
                       @RequestParam(required = false) String categoryName,
                       @RequestParam(required = false) UnitsType unit,
                       @RequestParam(required = false) SizeType size,
                       @RequestParam(required = false) String minStorageDays,
                       @RequestParam(required = false) String maxStorageDays,
                       @RequestParam(required = false) Boolean large,
                       Model model) {
        Duration minStorageLife = parseDays(minStorageDays);
        Duration maxStorageLife = parseDays(maxStorageDays);

        if (minStorageLife != null && maxStorageLife != null && minStorageLife.compareTo(maxStorageLife) > 0) {
            throw new BadRequestException("Минимальный срок хранения не может быть больше максимального");
        }

        ProductsDAO.Filter filter = ProductsDAO.getFilterBuilder()
                .id(ControllerUtils.parseLongOrNull(id))
                .name(ControllerUtils.blankToNull(name))
                .categoryId(ControllerUtils.parseLongOrNull(categoryId))
                .categoryName(ControllerUtils.blankToNull(categoryName))
                .unit(unit)
                .size(size)
                .minStorageLife(minStorageLife)
                .maxStorageLife(maxStorageLife)
                .large(large)
                .build();

        model.addAttribute("products", productsDAO.getByFilter(filter));
        model.addAttribute("units", UnitsType.values());
        model.addAttribute("sizes", SizeType.values());
        return "products/list";
    }

    @GetMapping("/{id}")
    public String details(@PathVariable Long id,
                          @RequestParam(required = false) String supplierId,
                          @RequestParam(required = false) String shelfNum,
                          @RequestParam(required = false) String roomNum,
                          @RequestParam(required = false) String minAmount,
                          @RequestParam(required = false) String maxAmount,
                          Model model) {
        Products product = getProductOrThrow(id);

        Integer parsedMinAmount = ControllerUtils.parseIntOrNull(minAmount);
        Integer parsedMaxAmount = ControllerUtils.parseIntOrNull(maxAmount);

        if (parsedMinAmount != null && parsedMaxAmount != null && parsedMinAmount > parsedMaxAmount) {
            throw new BadRequestException("Минимальное количество не может быть больше максимального");
        }

        ProductUnitsDAO.Filter unitsFilter = ProductUnitsDAO.getFilterBuilder()
                .productId(id)
                .supplierId(ControllerUtils.parseLongOrNull(supplierId))
                .shelfNum(ControllerUtils.parseLongOrNull(shelfNum))
                .roomNum(ControllerUtils.parseIntOrNull(roomNum))
                .minAmount(parsedMinAmount)
                .maxAmount(parsedMaxAmount)
                .build();

        model.addAttribute("product", product);
        model.addAttribute("units", productUnitsDAO.getByFilter(unitsFilter));
        return "products/details";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("product", new Products());
        addProductFormAttributes(model);
        return "products/form";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        Products product = getProductOrThrow(id);

        model.addAttribute("product", product);
        addProductFormAttributes(model);
        return "products/form";
    }

    @PostMapping("/save")
    public String save(@RequestParam(required = false) Long id,
                       @RequestParam Long categoryId,
                       @RequestParam String name,
                       @RequestParam(required = false) String description,
                       @RequestParam(required = false) UnitsType unit,
                       @RequestParam(required = false, name = "product_size") SizeType size,
                       @RequestParam(required = false) Integer unitsForOne,
                       @RequestParam(required = false) String storageLifeDays) {
        ProductCategories category = categoriesDAO.getById(categoryId);
        if (category == null) {
            throw new EntityNotFoundException("Категория с id=" + categoryId + " не найдена");
        }

        if (id != null) {
            getProductOrThrow(id);
        }

        if (unitsForOne != null && unitsForOne <= 0) {
            throw new BadRequestException("Количество единиц за одну позицию должно быть положительным");
        }

        Products entity = new Products();
        entity.setId(id == null ? ControllerUtils.nextId(new ArrayList<>(productsDAO.getAll()), 1000) : id);
        entity.setCategory(category);
        entity.setName(ControllerUtils.requireText(name, "Наименование"));
        entity.setDescription(ControllerUtils.blankToNull(description));
        entity.setUnit(unit);
        entity.setProduct_size(size);
        entity.setUnitsForOne(unitsForOne);
        entity.setStorageLife(parseDays(storageLifeDays));

        if (id == null) {
            productsDAO.save(entity);
        } else {
            productsDAO.update(entity);
        }

        return "redirect:/products";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id) {
        Products product = getProductOrThrow(id);

        if (!productsDAO.getUnitsForProduct(product).isEmpty()) {
            throw new BusinessRuleException("Нельзя удалить товар: на складе есть товарные единицы этого товара");
        }

        if (!productsDAO.getOrdersForProduct(product).isEmpty()) {
            throw new BusinessRuleException("Нельзя удалить товар: существуют связанные заказы");
        }

        if (!productsDAO.getSuppliesForProduct(product).isEmpty()) {
            throw new BusinessRuleException("Нельзя удалить товар: существуют связанные поставки");
        }

        productsDAO.deleteById(id);
        return "redirect:/products";
    }

    private Products getProductOrThrow(Long id) {
        Products product = productsDAO.getById(id);
        if (product == null) {
            throw new EntityNotFoundException("Товар с id=" + id + " не найден");
        }
        return product;
    }

    private void addProductFormAttributes(Model model) {
        model.addAttribute("categories", categoriesDAO.getAll());
        model.addAttribute("units", UnitsType.values());
        model.addAttribute("sizes", SizeType.values());
    }

    private Duration parseDays(String days) {
        if (days == null || days.isBlank()) {
            return null;
        }

        try {
            long parsedDays = Long.parseLong(days.trim());
            if (parsedDays < 0) {
                throw new BadRequestException("Срок хранения не может быть отрицательным");
            }
            return Duration.ofDays(parsedDays);
        } catch (NumberFormatException e) {
            throw new BadRequestException("Срок хранения должен быть числом дней");
        }
    }
}