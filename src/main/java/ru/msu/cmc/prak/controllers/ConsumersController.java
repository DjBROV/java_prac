package ru.msu.cmc.prak.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import ru.msu.cmc.prak.DAO.ConsumersDAO;
import ru.msu.cmc.prak.controllers.exceptions.BusinessRuleException;
import ru.msu.cmc.prak.controllers.exceptions.EntityNotFoundException;
import ru.msu.cmc.prak.models.Consumers;

import java.util.ArrayList;

@Controller
@RequiredArgsConstructor
@RequestMapping("/consumers")
public class ConsumersController {
    private final ConsumersDAO consumersDAO;

    @GetMapping
    public String list(@RequestParam(required = false) String id,
                       @RequestParam(required = false) String name,
                       @RequestParam(required = false) String address,
                       @RequestParam(required = false) String phoneNum,
                       @RequestParam(required = false) String email,
                       Model model) {
        ConsumersDAO.Filter filter = ConsumersDAO.getFilterBuilder()
                .id(ControllerUtils.parseLongOrNull(id))
                .name(ControllerUtils.blankToNull(name))
                .address(ControllerUtils.blankToNull(address))
                .phoneNum(ControllerUtils.blankToNull(phoneNum))
                .email(ControllerUtils.blankToNull(email))
                .build();

        model.addAttribute("consumers", consumersDAO.getByFilter(filter));
        return "consumers/list";
    }

    @GetMapping("/{id}")
    public String details(@PathVariable Long id, Model model) {
        Consumers consumer = getConsumerOrThrow(id);

        model.addAttribute("consumer", consumer);
        model.addAttribute("orders", consumersDAO.getOrdersByConsumer(consumer));
        return "consumers/details";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("consumer", new Consumers());
        return "consumers/form";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        Consumers consumer = getConsumerOrThrow(id);

        model.addAttribute("consumer", consumer);
        return "consumers/form";
    }

    @PostMapping("/save")
    public String save(@RequestParam(required = false) Long id,
                       @RequestParam String name,
                       @RequestParam(required = false) String description,
                       @RequestParam String phoneNum,
                       @RequestParam String email,
                       @RequestParam(required = false) String address) {
        Consumers entity = new Consumers();
        entity.setId(id == null ? ControllerUtils.nextId(new ArrayList<>(consumersDAO.getAll()), 100) : id);
        entity.setName(ControllerUtils.requireText(name, "Наименование"));
        entity.setDescription(ControllerUtils.blankToNull(description));
        entity.setPhoneNum(ControllerUtils.requireText(phoneNum, "Телефон"));
        entity.setEmail(ControllerUtils.requireText(email, "Email"));
        entity.setAddress(ControllerUtils.blankToNull(address));

        if (id == null) {
            consumersDAO.save(entity);
        } else {
            getConsumerOrThrow(id);
            consumersDAO.update(entity);
        }

        return "redirect:/consumers";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id) {
        Consumers consumer = getConsumerOrThrow(id);

        if (!consumersDAO.getOrdersByConsumer(consumer).isEmpty()) {
            throw new BusinessRuleException("Нельзя удалить потребителя: у него есть связанные заказы");
        }

        consumersDAO.deleteById(id);
        return "redirect:/consumers";
    }

    private Consumers getConsumerOrThrow(Long id) {
        Consumers consumer = consumersDAO.getById(id);
        if (consumer == null) {
            throw new EntityNotFoundException("Потребитель с id=" + id + " не найден");
        }
        return consumer;
    }
}