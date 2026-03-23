package ru.msu.cmc.prak.DAO;

import lombok.Builder;
import lombok.Getter;
import ru.msu.cmc.prak.models.*;
import java.time.LocalDateTime;
import java.util.List;

public interface ProductUnitsDAO extends CommonDAO<ProductUnits, Long> {

    /**
     * Поиск партий по продукту.
     */
    List<ProductUnits> getByProductId(Long productId);

    /**
     * Поиск партий по полке.
     */
    List<ProductUnits> getByShelfNum(Long shelfNum);

    /**
     * Поиск партий по поставке.
     */
    List<ProductUnits> getBySupplyId(Long supplyId);

    /**
     * Поиск партий по заказу (зарезервированные).
     */
    List<ProductUnits> getByOrderId(Long orderId);

    /**
     * Поиск партий по дате поступления.
     */
    List<ProductUnits> getByArrivalRange(LocalDateTime from, LocalDateTime to);

    /**
     * Основной метод поиска партий с фильтрами.
     * Используется на странице товара для фильтрации товарных единиц
     * (поставщик, местоположение, объём).
     */
    List<ProductUnits> getByFilter(Filter filter);

    /**
     * Получение продукта, связанного с партией.
     */
    Products getProduct(ProductUnits unit);

    /**
     * Получение полки, на которой хранится партия.
     */
    ShelfsWorkload getShelf(ProductUnits unit);

    /**
     * Получение поставки, в рамках которой создана партия.
     */
    Supplies getSupply(ProductUnits unit);

    /**
     * Получение заказа, на который зарезервирована партия (может быть null).
     */
    Orders getOrder(ProductUnits unit);

    /**
     * Получение всех партий, которые ещё не зарезервированы (order_id IS NULL).
     * Используется для поиска свободного товара.
     */
    List<ProductUnits> getFreeUnits();

    /**
     * Получение всех партий, зарезервированных под заказы (order_id NOT NULL).
     */
    List<ProductUnits> getReservedUnits();

    @Builder
    @Getter
    class Filter {
        private Long productId;
        private Long supplyId;           // поставка, из которой получен товар
        private Long supplierId;         // поставщик (через supply)
        private Long shelfNum;
        private Integer roomNum;         // комната (через shelf)
        private Integer minAmount;
        private Integer maxAmount;
        private LocalDateTime arrivalFrom;
        private LocalDateTime arrivalTo;
        private Boolean reserved;        // true – только зарезервированные, false – только свободные
    }

    static Filter.FilterBuilder getFilterBuilder() {
        return Filter.builder();
    }
}