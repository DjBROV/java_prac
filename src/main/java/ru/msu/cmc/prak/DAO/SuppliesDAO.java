package ru.msu.cmc.prak.DAO;

import lombok.Builder;
import lombok.Getter;
import ru.msu.cmc.prak.models.*;
import java.time.LocalDateTime;
import java.util.List;

public interface SuppliesDAO extends CommonDAO<Supplies, Long> {

    /**
     * Поиск поставок по продукту.
     */
    List<Supplies> getByProductId(Long productId);

    /**
     * Поиск поставок по поставщику.
     */
    List<Supplies> getByProviderId(Long providerId);

    /**
     * Поиск поставок по флагу завершённости.
     */
    List<Supplies> getByCompleted(Boolean completed);

    /**
     * Поиск поставок по временному диапазону.
     */
    List<Supplies> getByTimeRange(LocalDateTime from, LocalDateTime to);

    /**
     * Основной метод поиска поставок с фильтрами.
     * Используется на странице "Поставки".
     */
    List<Supplies> getByFilter(Filter filter);

    /**
     * Получение поставщика, связанного с поставкой.
     */
    Providers getProvider(Supplies supply);

    /**
     * Получение продукта, связанного с поставкой.
     */
    Products getProduct(Supplies supply);

    /**
     * Получение всех партий, созданных в рамках этой поставки.
     * Нужно для отображения списка товарных единиц на странице поставки,
     * а также для удаления поставки (тогда удаляются и связанные партии).
     */
    List<ProductUnits> getProductUnitsForSupply(Supplies supply);

    @Builder
    @Getter
    class Filter {
        private Long id;
        private Long productId;
        private Long providerId;
        private String productName;       // поиск по имени продукта (like)
        private String providerName;      // поиск по имени поставщика (like)
        private Integer amountFrom;
        private Integer amountTo;
        private LocalDateTime timeFrom;
        private LocalDateTime timeTo;
        private Boolean completed;
    }

    static Filter.FilterBuilder getFilterBuilder() {
        return Filter.builder();
    }
}