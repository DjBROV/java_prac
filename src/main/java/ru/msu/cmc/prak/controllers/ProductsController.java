package ru.msu.cmc.prak.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import ru.msu.cmc.prak.DAO.ProductCategoriesDAO;
import ru.msu.cmc.prak.DAO.ProductUnitsDAO;
import ru.msu.cmc.prak.DAO.ProductsDAO;
import ru.msu.cmc.prak.models.*;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

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
        ProductsDAO.Filter filter = ProductsDAO.getFilterBuilder()
                .id(ControllerUtils.parseLongOrNull(id))
                .name(name)
                .categoryId(ControllerUtils.parseLongOrNull(categoryId))
                .categoryName(categoryName)
                .unit(unit)
                .size(size)
                .minStorageLife(parseDays(minStorageDays))
                .maxStorageLife(parseDays(maxStorageDays))
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
        Products product = productsDAO.getById(id);
        if (product == null) {
            throw new IllegalArgumentException("Товар с id=" + id + " не найден");
        }
        ProductUnitsDAO.Filter unitsFilter = ProductUnitsDAO.getFilterBuilder()
                .productId(id)
                .supplierId(ControllerUtils.parseLongOrNull(supplierId))
                .shelfNum(ControllerUtils.parseLongOrNull(shelfNum))
                .roomNum(ControllerUtils.parseIntOrNull(roomNum))
                .minAmount(ControllerUtils.parseIntOrNull(minAmount))
                .maxAmount(ControllerUtils.parseIntOrNull(maxAmount))
                .build();

        model.addAttribute("product", product);
        model.addAttribute("units", productUnitsDAO.getByFilter(unitsFilter));
        return "products/details";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("product", new Products());
        model.addAttribute("categories", categoriesDAO.getAll());
        model.addAttribute("units", UnitsType.values());
        model.addAttribute("sizes", SizeType.values());
        return "products/form";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        Products product = productsDAO.getById(id);
        if (product == null) {
            throw new IllegalArgumentException("Товар с id=" + id + " не найден");
        }
        model.addAttribute("product", product);
        model.addAttribute("categories", categoriesDAO.getAll());
        model.addAttribute("units", UnitsType.values());
        model.addAttribute("sizes", SizeType.values());
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
            throw new IllegalArgumentException("Категория не найдена");
        }

        Products entity = new Products();
        entity.setId(id == null ? ControllerUtils.nextId(new ArrayList<>(productsDAO.getAll()), 1000) : id);
        entity.setCategory(category);
        entity.setName(name);
        entity.setDescription(description);
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
        productsDAO.deleteById(id);
        return "redirect:/products";
    }

    private Duration parseDays(String days) {
        if (days == null || days.isBlank()) {
            return null;
        }
        return Duration.ofDays(Long.parseLong(days.trim()));
    }
}