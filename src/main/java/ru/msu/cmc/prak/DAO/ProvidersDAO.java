package ru.msu.cmc.prak.DAO;

import lombok.Builder;
import lombok.Getter;
import ru.msu.cmc.prak.models.Providers;
import ru.msu.cmc.prak.models.Supplies;
import java.util.List;

public interface ProvidersDAO extends CommonDAO<Providers, Long> {


    /**
     * Основной метод поиска поставщиков с фильтрами.
     * Используется на странице "Поставщики".
     */
    List<Providers> getByFilter(Filter filter);

    /**
     * Получение всех поставок данного поставщика.
     * Используется на странице поставщика для ссылки на страницу "Поставки".
     */
    List<Supplies> getSuppliesFromProvider(Providers provider);

    @Builder
    @Getter
    class Filter {
        private Long id;
        private String name;       // like
        private String address;    // like
        private String phoneNum;   // like
        private String email;      // like
    }

    static Filter.FilterBuilder getFilterBuilder() {
        return Filter.builder();
    }
}