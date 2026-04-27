package ru.msu.cmc.prak.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.msu.cmc.prak.DAO.OrdersDAO;
import ru.msu.cmc.prak.DAO.ProductUnitsDAO;
import ru.msu.cmc.prak.controllers.ControllerUtils;
import ru.msu.cmc.prak.models.Consumers;
import ru.msu.cmc.prak.models.Orders;
import ru.msu.cmc.prak.models.ProductUnits;
import ru.msu.cmc.prak.models.Products;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrdersService {
    private final OrdersDAO ordersDAO;
    private final ProductUnitsDAO productUnitsDAO;

    public Orders buildOrder(Long id,
                             Consumers consumer,
                             Products product,
                             Integer amount,
                             String time) {
        Orders order = new Orders();
        order.setId(id == null ? ControllerUtils.nextId(new ArrayList<>(ordersDAO.getAll()), 1000) : id);
        order.setConsumer(consumer);
        order.setProduct(product);
        order.setAmount(amount == null ? null : BigDecimal.valueOf(amount.longValue()));
        order.setTime(ControllerUtils.parseDateTimeOrNull(time));
        return order;
    }

    public BigDecimal getAvailableAmountForOrder(Orders existingOrder, Long productId) {
        BigDecimal available = getFreeUnitsForProduct(productId).stream()
                .map(ProductUnits::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (existingOrder != null && !existingOrder.isCompleted()) {
            BigDecimal alreadyReservedForThisOrder = ordersDAO.getProductUnitsForOrder(existingOrder).stream()
                    .filter(unit -> unit.getProduct() != null)
                    .filter(unit -> productId.equals(unit.getProduct().getId()))
                    .map(ProductUnits::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            available = available.add(alreadyReservedForThisOrder);
        }

        return available;
    }

    public void createOrder(Orders order, Long productId, BigDecimal requiredAmount) {
        order.setCompleted(false);
        ordersDAO.save(order);

        List<ProductUnits> freeUnits = getFreeUnitsForProduct(productId);
        reserveUnitsForOrder(order, freeUnits, requiredAmount);
    }

    public void updateOrder(Orders existingOrder,
                            Orders newOrder,
                            Long productId,
                            BigDecimal requiredAmount) {
        releaseReservation(existingOrder);

        newOrder.setCompleted(false);
        ordersDAO.update(newOrder);

        List<ProductUnits> freeUnits = getFreeUnitsForProduct(productId);
        reserveUnitsForOrder(newOrder, freeUnits, requiredAmount);
    }

    public void completeOrder(Orders order) {
        order.setCompleted(true);
        ordersDAO.update(order);

        for (ProductUnits unit : ordersDAO.getProductUnitsForOrder(order)) {
            productUnitsDAO.deleteById(unit.getId());
        }
    }

    public void deleteOrder(Orders order) {
        if (!order.isCompleted()) {
            releaseReservation(order);
        }

        ordersDAO.deleteById(order.getId());
    }

    private List<ProductUnits> getFreeUnitsForProduct(Long productId) {
        return productUnitsDAO.getByFilter(ProductUnitsDAO.getFilterBuilder()
                .productId(productId)
                .reserved(false)
                .build());
    }

    private void releaseReservation(Orders order) {
        List<ProductUnits> reservedUnits = ordersDAO.getProductUnitsForOrder(order);

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
            }
        }
    }

    private void reserveUnitsForOrder(Orders order,
                                      List<ProductUnits> freeUnits,
                                      BigDecimal requiredAmount) {
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