package ru.msu.cmc.prak.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import ru.msu.cmc.prak.DAO.ProvidersDAO;
import ru.msu.cmc.prak.controllers.exceptions.BusinessRuleException;
import ru.msu.cmc.prak.controllers.exceptions.EntityNotFoundException;
import ru.msu.cmc.prak.models.Providers;

import java.util.ArrayList;

@Controller
@RequiredArgsConstructor
@RequestMapping("/providers")
public class ProvidersController {
    private final ProvidersDAO providersDAO;

    @GetMapping
    public String list(@RequestParam(required = false) String id,
                       @RequestParam(required = false) String name,
                       @RequestParam(required = false) String address,
                       @RequestParam(required = false) String phoneNum,
                       @RequestParam(required = false) String email,
                       Model model) {
        ProvidersDAO.Filter filter = ProvidersDAO.getFilterBuilder()
                .id(ControllerUtils.parseLongOrNull(id))
                .name(ControllerUtils.blankToNull(name))
                .address(ControllerUtils.blankToNull(address))
                .phoneNum(ControllerUtils.blankToNull(phoneNum))
                .email(ControllerUtils.blankToNull(email))
                .build();

        model.addAttribute("providers", providersDAO.getByFilter(filter));
        return "providers/list";
    }

    @GetMapping("/{id}")
    public String details(@PathVariable Long id, Model model) {
        Providers provider = getProviderOrThrow(id);

        model.addAttribute("provider", provider);
        model.addAttribute("supplies", providersDAO.getSuppliesFromProvider(provider));
        return "providers/details";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("provider", new Providers());
        return "providers/form";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        Providers provider = getProviderOrThrow(id);

        model.addAttribute("provider", provider);
        return "providers/form";
    }

    @PostMapping("/save")
    public String save(@RequestParam(required = false) Long id,
                       @RequestParam String name,
                       @RequestParam(required = false) String description,
                       @RequestParam String phoneNum,
                       @RequestParam String email,
                       @RequestParam(required = false) String address) {
        Providers entity = new Providers();
        entity.setId(id == null ? ControllerUtils.nextId(new ArrayList<>(providersDAO.getAll()), 100) : id);
        entity.setName(ControllerUtils.requireText(name, "Наименование"));
        entity.setDescription(ControllerUtils.blankToNull(description));
        entity.setPhoneNum(ControllerUtils.requireText(phoneNum, "Телефон"));
        entity.setEmail(ControllerUtils.requireText(email, "Email"));
        entity.setAddress(ControllerUtils.blankToNull(address));

        if (id == null) {
            providersDAO.save(entity);
        } else {
            getProviderOrThrow(id);
            providersDAO.update(entity);
        }

        return "redirect:/providers";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id) {
        Providers provider = getProviderOrThrow(id);

        if (!providersDAO.getSuppliesFromProvider(provider).isEmpty()) {
            throw new BusinessRuleException("Нельзя удалить поставщика: у него есть связанные поставки");
        }

        providersDAO.deleteById(id);
        return "redirect:/providers";
    }

    private Providers getProviderOrThrow(Long id) {
        Providers provider = providersDAO.getById(id);
        if (provider == null) {
            throw new EntityNotFoundException("Поставщик с id=" + id + " не найден");
        }
        return provider;
    }
}