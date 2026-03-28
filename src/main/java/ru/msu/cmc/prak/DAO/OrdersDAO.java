package ru.msu.cmc.prak.DAO;

import lombok.Builder;
import lombok.Getter;
import ru.msu.cmc.prak.models.*;
import java.time.LocalDateTime;
import java.util.List;

public interface OrdersDAO extends CommonDAO<Orders, Long> {


    /**
     * Поиск заказов по фильтрам.
     * Используется на странице "Заказы".
     */
    List<Orders> getByFilter(Filter filter);

    /**
     * Получение потребителя, связанного с заказом.
     */
    Consumers getConsumer(Orders order);

    /**
     * Получение продукта, связанного с заказом.
     */
    Products getProduct(Orders order);

    /**
     * Получение всех партий, выделенных под этот заказ.
     * Используется при выполнении заказа (списание товарных единиц).
     */
    List<ProductUnits> getProductUnitsForOrder(Orders order);

    @Builder
    @Getter
    class Filter {
        private Long id;
        private Long productId;
        private Long consumerId;
        private String productName;       // like
        private String consumerName;      // like
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