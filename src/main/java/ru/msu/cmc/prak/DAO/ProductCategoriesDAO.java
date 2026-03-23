package ru.msu.cmc.prak.DAO;

import lombok.Builder;
import lombok.Getter;
import ru.msu.cmc.prak.models.ProductCategories;
import ru.msu.cmc.prak.models.Products;
import java.util.List;

public interface ProductCategoriesDAO extends CommonDAO<ProductCategories, Long> {

    /**
     * Поиск категорий по имени (частичное совпадение).
     * Используется в панели фильтров на странице "Категории товаров".
     */
    List<ProductCategories> getAllByName(String name);

    /**
     * Получение категории по точному имени (например, для проверки уникальности).
     */
    ProductCategories getSingleByName(String name);

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