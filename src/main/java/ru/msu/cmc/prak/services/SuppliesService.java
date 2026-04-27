package ru.msu.cmc.prak.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.msu.cmc.prak.DAO.ProductUnitsDAO;
import ru.msu.cmc.prak.DAO.ShelfsWorkloadDAO;
import ru.msu.cmc.prak.DAO.SuppliesDAO;
import ru.msu.cmc.prak.controllers.ControllerUtils;
import ru.msu.cmc.prak.controllers.exceptions.BusinessRuleException;
import ru.msu.cmc.prak.models.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SuppliesService {
    private static final int SHELF_CAPACITY = 500;

    private final SuppliesDAO suppliesDAO;
    private final ProductUnitsDAO productUnitsDAO;
    private final ShelfsWorkloadDAO shelfsWorkloadDAO;

    public Supplies buildSupply(Long id,
                                Providers provider,
                                Products product,
                                Integer amount,
                                String time) {
        Supplies supply = new Supplies();
        supply.setId(id == null ? ControllerUtils.nextId(new ArrayList<>(suppliesDAO.getAll()), 1000) : id);
        supply.setProvider(provider);
        supply.setProduct(product);
        supply.setAmount(amount == null ? null : BigDecimal.valueOf(amount.longValue()));
        supply.setTime(ControllerUtils.parseDateTimeOrNull(time));
        return supply;
    }

    public int calcRequiredWorkload(Products product, int amount) {
        return amount * getSizeWeight(product);
    }

    public ShelfsWorkload findShelfForSupply(Supplies existingSupply, int requiredWorkload) {
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

    public int calcTotalFreeWorkload(Supplies existingSupply) {
        Map<Long, Integer> workloadToBeReleased = calcWorkloadToBeReleasedByShelf(existingSupply);

        return shelfsWorkloadDAO.getAll().stream()
                .mapToInt(shelf -> SHELF_CAPACITY
                        - shelf.getWorkloadCount()
                        + workloadToBeReleased.getOrDefault(shelf.getId(), 0))
                .sum();
    }

    public void createSupply(Supplies supply,
                             Integer amount,
                             ShelfsWorkload selectedShelf,
                             int requiredWorkload) {
        reserveSpaceOnShelf(selectedShelf, requiredWorkload);
        suppliesDAO.save(supply);
        createUnitForSupply(supply, amount, selectedShelf);
    }

    public void updateSupply(Supplies existingSupply,
                             Supplies newSupply,
                             Integer amount,
                             ShelfsWorkload selectedShelf,
                             int requiredWorkload) {
        releaseReservedSpace(existingSupply);

        ShelfsWorkload refreshedShelf = shelfsWorkloadDAO.getById(selectedShelf.getId());
        reserveSpaceOnShelf(refreshedShelf, requiredWorkload);

        suppliesDAO.update(newSupply);
        createUnitForSupply(newSupply, amount, refreshedShelf);
    }

    public void completeSupply(Supplies supply) {
        supply.setCompleted(true);
        suppliesDAO.update(supply);
    }

    public void deleteSupply(Supplies supply) {
        releaseReservedSpace(supply);
        suppliesDAO.deleteById(supply.getId());
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