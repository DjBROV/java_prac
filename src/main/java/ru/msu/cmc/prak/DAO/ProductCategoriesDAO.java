package ru.msu.cmc.prak.DAO;

import lombok.Builder;
import lombok.Getter;
import ru.msu.cmc.prak.models.ProductCategories;
import ru.msu.cmc.prak.models.Products;
import java.util.List;

public interface ProductCategoriesDAO extends CommonDAO<ProductCategories, Long> {

    /**
     * Поиск категорий по набору фильтров (id, имя).
     */
    List<ProductCategories> getByFilter(Filter filter);

    /**
     * Получение всех товаров, относящихся к данной категории.
     * Используется на странице "Категории товаров" при нажатии кнопки "Товары",
     * а также для проверки, пуста ли категория перед удалением.
     */
    List<Products> getProductsInCategory(ProductCategories category);

    /**
     * Количество товаров в категории. Нужно для валидации удаления:
     * нельзя удалить категорию, если в ней есть товары.
     */
    long countProductsInCategory(ProductCategories category);

    @Builder
    @Getter
    class Filter {
        private String name; // поиск по имени (like)
    }

    static Filter.FilterBuilder getFilterBuilder() {
        return Filter.builder();
    }
}