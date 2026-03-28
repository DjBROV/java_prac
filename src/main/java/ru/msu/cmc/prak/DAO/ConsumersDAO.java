package ru.msu.cmc.prak.DAO;

import lombok.Builder;
import lombok.Getter;
import ru.msu.cmc.prak.models.Consumers;
import ru.msu.cmc.prak.models.Orders;
import java.util.List;

public interface ConsumersDAO extends CommonDAO<Consumers, Long> {



    /**
     * Поиск потребителей по фильтрам.
     */
    List<Consumers> getByFilter(Filter filter);

    /**
     * Получение всех заказов данного потребителя.
     * Используется на странице потребителя для ссылки на страницу "Заказы".
     */
    List<Orders> getOrdersByConsumer(Consumers consumer);

    @Builder
    @Getter
    class Filter {
        private Long id;
        private String name;
        private String address;
        private String phoneNum;
        private String email;
    }

    static Filter.FilterBuilder getFilterBuilder() {
        return Filter.builder();
    }
}