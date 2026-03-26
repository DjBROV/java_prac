package ru.msu.cmc.prak.DAO;

import jakarta.persistence.EntityManagerFactory;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import ru.msu.cmc.prak.models.*;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestPropertySource(locations = "classpath:application.properties")
public class ProductsDAOTest {

    @Autowired
    private ProductsDAO productsDAO;

    @Autowired
    private ProductCategoriesDAO productCategoriesDAO;

    @Autowired
    private ProvidersDAO providersDAO;

    @Autowired
    private ConsumersDAO consumersDAO;

    @Autowired
    private SuppliesDAO suppliesDAO;

    @Autowired
    private OrdersDAO ordersDAO;

    @Autowired
    private ProductUnitsDAO productUnitsDAO;

    @Autowired
    private ShelfsWorkloadDAO shelfsWorkloadDAO;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Test
    void testGetAllByName() {
        ProductCategories category = saveCategory(1L, "Техника");
        saveProduct(100L, category, "Смартфон", UnitsType.kg, SizeType.small, 10, 30);
        saveProduct(101L, category, "Смартфон Pro", UnitsType.kg, SizeType.large, 20, 60);

        List<Products> found = productsDAO.getAllByName("Смартфон");
        assertEquals(2, found.size());
    }

    @Test
    void testGetSingleByName() {
        ProductCategories category = saveCategory(1L, "Техника");
        saveProduct(100L, category, "Планшет", UnitsType.kg, SizeType.middle, 5, 20);

        Products found = productsDAO.getSingleByName("Планшет");
        assertNotNull(found);
        assertEquals(100L, found.getId());
    }

    @Test
    void testGetByCategoryId() {
        ProductCategories cat1 = saveCategory(1L, "Техника");
        ProductCategories cat2 = saveCategory(2L, "Быт");
        saveProduct(100L, cat1, "Смартфон", UnitsType.kg, SizeType.small, 10, 30);
        saveProduct(101L, cat2, "Пылесос", UnitsType.kg, SizeType.large, 1, 100);

        List<Products> found = productsDAO.getByCategoryId(1L);
        assertEquals(1, found.size());
        assertEquals(100L, found.getFirst().getId());
    }

    @Test
    void testGetByUnit() {
        ProductCategories category = saveCategory(1L, "Техника");
        saveProduct(100L, category, "Товар1", UnitsType.kg, SizeType.small, 10, 30);
        saveProduct(101L, category, "Товар2", UnitsType.g, SizeType.small, 10, 30);

        List<Products> found = productsDAO.getByUnit(UnitsType.kg);
        assertEquals(1, found.size());
        assertEquals(100L, found.getFirst().getId());
    }

    @Test
    void testGetBySize() {
        ProductCategories category = saveCategory(1L, "Техника");
        saveProduct(100L, category, "Товар1", UnitsType.kg, SizeType.large, 10, 30);
        saveProduct(101L, category, "Товар2", UnitsType.kg, SizeType.small, 10, 30);

        List<Products> found = productsDAO.getBySize(SizeType.large);
        assertEquals(1, found.size());
        assertEquals(100L, found.getFirst().getId());
    }

    @Test
    void testGetByFilter() {
        ProductCategories cat1 = saveCategory(1L, "Электроника");
        ProductCategories cat2 = saveCategory(2L, "Мебель");

        saveProduct(100L, cat1, "Ноутбук", UnitsType.kg, SizeType.large, 1, 365);
        saveProduct(101L, cat2, "Стул", UnitsType.kg, SizeType.small, 1, 30);

        ProductsDAO.Filter filter = ProductsDAO.getFilterBuilder()
                .categoryName("Электро")
                .size(SizeType.large)
                .large(true)
                .minStorageLife(Duration.ofDays(300))
                .build();

        List<Products> found = productsDAO.getByFilter(filter);
        assertEquals(1, found.size());
        assertEquals(100L, found.getFirst().getId());
    }

    @Test
    void testGetSuppliesForProduct() {
        ProductCategories category = saveCategory(1L, "Техника");
        Products product = saveProduct(100L, category, "Ноутбук", UnitsType.kg, SizeType.large, 1, 365);
        Providers provider = saveProvider(1L, "Поставщик1");

        saveSupply(1L, product, provider);
        saveSupply(2L, product, provider);

        List<Supplies> found = productsDAO.getSuppliesForProduct(product);
        assertEquals(2, found.size());
    }

    @Test
    void testGetOrdersForProduct() {
        ProductCategories category = saveCategory(1L, "Техника");
        Products product = saveProduct(100L, category, "Ноутбук", UnitsType.kg, SizeType.large, 1, 365);
        Consumers consumer = saveConsumer(1L, "Клиент1");

        saveOrder(1L, product, consumer);
        saveOrder(2L, product, consumer);

        List<Orders> found = productsDAO.getOrdersForProduct(product);
        assertEquals(2, found.size());
    }

    @Test
    void testGetUnitsForProduct() {
        ProductCategories category = saveCategory(1L, "Техника");
        Products product = saveProduct(100L, category, "Ноутбук", UnitsType.kg, SizeType.large, 1, 365);
        Providers provider = saveProvider(1L, "Поставщик1");
        Supplies supply = saveSupply(1L, product, provider);
        ShelfsWorkload shelf = saveShelf(1L, 101, 10);

        saveUnit(1L, product, supply, shelf, null);
        saveUnit(2L, product, supply, shelf, null);

        List<ProductUnits> found = productsDAO.getUnitsForProduct(product);
        assertEquals(2, found.size());
    }

    @Test
    void testGetCategory() {
        ProductCategories category = saveCategory(1L, "Техника");
        Products product = saveProduct(100L, category, "Ноутбук", UnitsType.kg, SizeType.large, 1, 365);

        ProductCategories found = productsDAO.getCategory(product);
        assertNotNull(found);
        assertEquals(1L, found.getId());
    }

    private ProductCategories saveCategory(Long id, String name) {
        ProductCategories category = new ProductCategories();
        category.setId(id);
        category.setName(name);
        productCategoriesDAO.save(category);
        return category;
    }

    private Products saveProduct(Long id, ProductCategories category, String name,
                                 UnitsType unit, SizeType size, Integer unitsForOne, int storageDays) {
        Products product = new Products();
        product.setId(id);
        product.setCategory(category);
        product.setName(name);
        product.setUnit(unit);
        product.setProduct_size(size);
        product.setUnitsForOne(unitsForOne);
        product.setStorageLife(Duration.ofDays(storageDays));
        productsDAO.save(product);
        return product;
    }

    private Providers saveProvider(Long id, String name) {
        Providers provider = new Providers();
        provider.setId(id);
        provider.setName(name);
        provider.setPhoneNum("111");
        provider.setEmail("p" + id + "@mail.test");
        providersDAO.save(provider);
        return provider;
    }

    private Consumers saveConsumer(Long id, String name) {
        Consumers consumer = new Consumers();
        consumer.setId(id);
        consumer.setName(name);
        consumer.setPhoneNum("222");
        consumer.setEmail("c" + id + "@mail.test");
        consumersDAO.save(consumer);
        return consumer;
    }

    private Supplies saveSupply(Long id, Products product, Providers provider) {
        Supplies supply = new Supplies();
        supply.setId(id);
        supply.setProduct(product);
        supply.setProvider(provider);
        supply.setAmount(java.math.BigDecimal.valueOf(10));
        supply.setTime(java.time.LocalDateTime.of(2026, 1, 1, 10, 0));
        suppliesDAO.save(supply);
        return supply;
    }

    private Orders saveOrder(Long id, Products product, Consumers consumer) {
        Orders order = new Orders();
        order.setId(id);
        order.setProduct(product);
        order.setConsumer(consumer);
        order.setAmount(java.math.BigDecimal.valueOf(5));
        order.setTime(java.time.LocalDateTime.of(2026, 1, 2, 10, 0));
        ordersDAO.save(order);
        return order;
    }

    private ShelfsWorkload saveShelf(Long id, Integer roomNum, Integer workload) {
        ShelfsWorkload shelf = new ShelfsWorkload();
        shelf.setId(id);
        shelf.setRoomNum(roomNum);
        shelf.setWorkloadCount(workload);
        shelfsWorkloadDAO.save(shelf);
        return shelf;
    }

    private ProductUnits saveUnit(Long id, Products product, Supplies supply, ShelfsWorkload shelf, Orders order) {
        ProductUnits unit = new ProductUnits();
        unit.setId(id);
        unit.setProduct(product);
        unit.setSupply(supply);
        unit.setShelf(shelf);
        unit.setOrder(order);
        unit.setArrival(java.time.LocalDateTime.of(2026, 1, 3, 10, 0));
        unit.setAmount(java.math.BigDecimal.valueOf(2));
        productUnitsDAO.save(unit);
        return unit;
    }

    @BeforeAll
    @AfterEach
    void annihilation() {
        SessionFactory sessionFactory = entityManagerFactory.unwrap(SessionFactory.class);
        try (Session session = sessionFactory.openSession()) {
            session.beginTransaction();
            session.createNativeQuery("TRUNCATE TABLE product_units CASCADE").executeUpdate();
            session.createNativeQuery("TRUNCATE TABLE orders CASCADE").executeUpdate();
            session.createNativeQuery("TRUNCATE TABLE supplies CASCADE").executeUpdate();
            session.createNativeQuery("TRUNCATE TABLE products CASCADE").executeUpdate();
            session.createNativeQuery("TRUNCATE TABLE consumers CASCADE").executeUpdate();
            session.createNativeQuery("TRUNCATE TABLE providers CASCADE").executeUpdate();
            session.createNativeQuery("TRUNCATE TABLE shelfs_workload CASCADE").executeUpdate();
            session.createNativeQuery("TRUNCATE TABLE product_categories CASCADE").executeUpdate();
            session.getTransaction().commit();
        }
    }
}