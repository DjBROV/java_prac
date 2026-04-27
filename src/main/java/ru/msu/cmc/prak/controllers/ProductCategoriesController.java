package ru.msu.cmc.prak.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import ru.msu.cmc.prak.DAO.ProductCategoriesDAO;
import ru.msu.cmc.prak.controllers.exceptions.BusinessRuleException;
import ru.msu.cmc.prak.controllers.exceptions.EntityNotFoundException;
import ru.msu.cmc.prak.models.ProductCategories;

import java.util.ArrayList;
import java.util.List;

@Controller
@RequiredArgsConstructor
@RequestMapping("/categories")
public class ProductCategoriesController {
    private final ProductCategoriesDAO categoriesDAO;

    @GetMapping
    public String list(@RequestParam(required = false) String id,
                       @RequestParam(required = false) String name,
                       Model model) {
        Long parsedId = ControllerUtils.parseLongOrNull(id);

        List<ProductCategories> categories = categoriesDAO.getByFilter(
                ProductCategoriesDAO.getFilterBuilder()
                        .name(ControllerUtils.blankToNull(name))
                        .build()
        );

        if (parsedId != null) {
            categories = categories.stream()
                    .filter(c -> parsedId.equals(c.getId()))
                    .toList();
        }

        model.addAttribute("categories", categories);
        return "categories/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("category", new ProductCategories());
        return "categories/form";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        ProductCategories category = getCategoryOrThrow(id);

        model.addAttribute("category", category);
        return "categories/form";
    }

    @PostMapping("/save")
    public String save(@RequestParam(required = false) Long id,
                       @RequestParam String name) {
        ProductCategories entity = new ProductCategories();
        entity.setId(id == null ? ControllerUtils.nextId(new ArrayList<>(categoriesDAO.getAll()), 10) : id);
        entity.setName(ControllerUtils.requireText(name, "Наименование"));

        if (id == null) {
            categoriesDAO.save(entity);
        } else {
            getCategoryOrThrow(id);
            categoriesDAO.update(entity);
        }

        return "redirect:/categories";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id) {
        ProductCategories category = getCategoryOrThrow(id);

        long count = categoriesDAO.countProductsInCategory(category);
        if (count > 0) {
            throw new BusinessRuleException("Нельзя удалить категорию: в ней есть товары (" + count + ")");
        }

        categoriesDAO.deleteById(id);
        return "redirect:/categories";
    }

    private ProductCategories getCategoryOrThrow(Long id) {
        ProductCategories category = categoriesDAO.getById(id);
        if (category == null) {
            throw new EntityNotFoundException("Категория с id=" + id + " не найдена");
        }
        return category;
    }
}