package ru.msu.cmc.prak.DAO;

import lombok.Builder;
import lombok.Getter;
import ru.msu.cmc.prak.models.*;
import java.time.Duration;
import java.util.List;

public interface ProductsDAO extends CommonDAO<Products, Long> {

    /**
     * Поиск продуктов по названию (like).
     */
    List<Products> getAllByName(String name);

    /**
     * Получение продукта по точному названию.
     */
    Products getSingleByName(String name);

    /**
     * Получение продуктов, принадлежащих определённой категории.
     */
    List<Products> getByCategoryId(Long categoryId);

    /**
     * Поиск продуктов по единице измерения (enum).
     */
    List<Products> getByUnit(UnitsType unit);

    /**
     * Поиск продуктов по размеру (enum).
     */
    List<Products> getBySize(SizeType size);

    /**
     * Основной метод поиска продуктов с расширенным набором фильтров.
     * Используется на странице "Товары" для отображения списка товаров
     * с учётом всех заданных пользователем критериев.
     */
    List<Products> getByFilter(Filter filter);

    /**
     * Получение всех поставок данного продукта.
     * Используется на странице товара для отображения истории поставок.
     */
    List<Supplies> getSuppliesForProduct(Products product);

    /**
     * Получение всех заказов данного продукта.
     */
    List<Orders> getOrdersForProduct(Products product);

    /**
     * Получение всех партий (товарных единиц) продукта.
     * Используется на странице товара в списке товарных единиц.
     */
    List<ProductUnits> getUnitsForProduct(Products product);

    /**
     * Получение категории, к которой относится продукт.
     */
    ProductCategories getCategory(Products product);

    @Builder
    @Getter
    class Filter {
        private Long id;                      // точное совпадение
        private String name;                 // поиск по имени (like)
        private Long categoryId;             // точное совпадение категории
        private String categoryName;         // поиск категории по имени (like)
        private UnitsType unit;              // точное совпадение
        private SizeType size;               // точное совпадение
        private Duration minStorageLife;     // нижняя граница срока хранения
        private Duration maxStorageLife;     // верхняя граница
        private Boolean large;               // флаг "крупногабаритный" (size == 'large')
    }

    static Filter.FilterBuilder getFilterBuilder() {
        return Filter.builder();
    }
}