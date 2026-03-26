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

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestPropertySource(locations = "classpath:application.properties")
public class OrdersDAOTest {

    @Autowired
    private OrdersDAO ordersDAO;
    @Autowired
    private ProductsDAO productsDAO;
    @Autowired
    private ConsumersDAO consumersDAO;
    @Autowired
    private ProductCategoriesDAO productCategoriesDAO;
    @Autowired
    private ProductUnitsDAO productUnitsDAO;
    @Autowired
    private SuppliesDAO suppliesDAO;
    @Autowired
    private ProvidersDAO providersDAO;
    @Autowired
    private ShelfsWorkloadDAO shelfsWorkloadDAO;
    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Test
    void testGetByProductId() {
        ProductCategories category = saveCategory(1L, "Техника");
        Products product = saveProduct(1L, category, "Ноутбук");
        Consumers consumer = saveConsumer(1L, "Клиент");

        saveOrder(1L, product, consumer, BigDecimal.valueOf(5), LocalDateTime.of(2026, 1, 1, 10, 0), false);

        List<Orders> found = ordersDAO.getByProductId(1L);
        assertEquals(1, found.size());
    }

    @Test
    void testGetByConsumerId() {
        ProductCategories category = saveCategory(1L, "Техника");
        Products product = saveProduct(1L, category, "Ноутбук");
        Consumers consumer = saveConsumer(1L, "Клиент");

        saveOrder(1L, product, consumer, BigDecimal.valueOf(5), LocalDateTime.of(2026, 1, 1, 10, 0), false);

        List<Orders> found = ordersDAO.getByConsumerId(1L);
        assertEquals(1, found.size());
    }

    @Test
    void testGetByCompleted() {
        ProductCategories category = saveCategory(1L, "Техника");
        Products product = saveProduct(1L, category, "Ноутбук");
        Consumers consumer = saveConsumer(1L, "Клиент");

        saveOrder(1L, product, consumer, BigDecimal.valueOf(5), LocalDateTime.of(2026, 1, 1, 10, 0), true);
        saveOrder(2L, product, consumer, BigDecimal.valueOf(7), LocalDateTime.of(2026, 1, 2, 10, 0), false);

        List<Orders> found = ordersDAO.getByCompleted(true);
        assertEquals(1, found.size());
    }

    @Test
    void testGetByTimeRange() {
        ProductCategories category = saveCategory(1L, "Техника");
        Products product = saveProduct(1L, category, "Ноутбук");
        Consumers consumer = saveConsumer(1L, "Клиент");

        saveOrder(1L, product, consumer, BigDecimal.valueOf(5), LocalDateTime.of(2026, 1, 1, 10, 0), true);
        saveOrder(2L, product, consumer, BigDecimal.valueOf(7), LocalDateTime.of(2026, 2, 1, 10, 0), false);

        List<Orders> found = ordersDAO.getByTimeRange(
                LocalDateTime.of(2026, 1, 1, 0, 0),
                LocalDateTime.of(2026, 1, 31, 23, 59)
        );
        assertEquals(1, found.size());
    }

    @Test
    void testGetByFilter() {
        ProductCategories category = saveCategory(1L, "Техника");
        Products product = saveProduct(1L, category, "Ноутбук");
        Consumers consumer = saveConsumer(1L, "Иван");

        saveOrder(1L, product, consumer, BigDecimal.valueOf(5), LocalDateTime.of(2026, 1, 1, 10, 0), true);

        OrdersDAO.Filter filter = OrdersDAO.getFilterBuilder()
                .productName("Ноут")
                .consumerName("Ива")
                .amountFrom(1)
                .amountTo(10)
                .timeFrom(LocalDateTime.of(2026, 1, 1, 0, 0))
                .timeTo(LocalDateTime.of(2026, 1, 2, 0, 0))
                .completed(true)
                .build();

        List<Orders> found = ordersDAO.getByFilter(filter);
        assertEquals(1, found.size());
    }

    @Test
    void testGetConsumer() {
        ProductCategories category = saveCategory(1L, "Техника");
        Products product = saveProduct(1L, category, "Ноутбук");
        Consumers consumer = saveConsumer(1L, "Иван");
        Orders order = saveOrder(1L, product, consumer, BigDecimal.valueOf(5), LocalDateTime.of(2026, 1, 1, 10, 0), true);

        Consumers found = ordersDAO.getConsumer(order);
        assertNotNull(found);
        assertEquals(1L, found.getId());
    }

    @Test
    void testGetProduct() {
        ProductCategories category = saveCategory(1L, "Техника");
        Products product = saveProduct(1L, category, "Ноутбук");
        Consumers consumer = saveConsumer(1L, "Иван");
        Orders order = saveOrder(1L, product, consumer, BigDecimal.valueOf(5), LocalDateTime.of(2026, 1, 1, 10, 0), true);

        Products found = ordersDAO.getProduct(order);
        assertNotNull(found);
        assertEquals(1L, found.getId());
    }

    @Test
    void testGetProductUnitsForOrder() {
        ProductCategories category = saveCategory(1L, "Техника");
        Products product = saveProduct(1L, category, "Ноутбук");
        Consumers consumer = saveConsumer(1L, "Иван");
        Providers provider = saveProvider(1L, "Поставщик");
        Supplies supply = saveSupply(1L, product, provider);
        ShelfsWorkload shelf = saveShelf(1L, 101, 50);
        Orders order = saveOrder(1L, product, consumer, BigDecimal.valueOf(5), LocalDateTime.of(2026, 1, 1, 10, 0), true);

        saveUnit(1L, product, supply, shelf, order);
        saveUnit(2L, product, supply, shelf, order);

        List<ProductUnits> found = ordersDAO.getProductUnitsForOrder(order);
        assertEquals(2, found.size());
    }

    private ProductCategories saveCategory(Long id, String name) {
        ProductCategories category = new ProductCategories();
        category.setId(id);
        category.setName(name);
        productCategoriesDAO.save(category);
        return category;
    }

    private Products saveProduct(Long id, ProductCategories category, String name) {
        Products product = new Products();
        product.setId(id);
        product.setCategory(category);
        product.setName(name);
        product.setUnit(UnitsType.kg);
        product.setProduct_size(SizeType.large);
        product.setUnitsForOne(1);
        product.setStorageLife(Duration.ofDays(90));
        productsDAO.save(product);
        return product;
    }

    private Consumers saveConsumer(Long id, String name) {
        Consumers consumer = new Consumers();
        consumer.setId(id);
        consumer.setName(name);
        consumer.setPhoneNum("123");
        consumer.setEmail("c" + id + "@test");
        consumersDAO.save(consumer);
        return consumer;
    }

    private Orders saveOrder(Long id, Products product, Consumers consumer,
                             BigDecimal amount, LocalDateTime time, boolean completed) {
        Orders order = new Orders();
        order.setId(id);
        order.setProduct(product);
        order.setConsumer(consumer);
        order.setAmount(amount);
        order.setTime(time);
        order.setCompleted(completed);
        ordersDAO.save(order);
        return order;
    }

    private Providers saveProvider(Long id, String name) {
        Providers provider = new Providers();
        provider.setId(id);
        provider.setName(name);
        provider.setPhoneNum("321");
        provider.setEmail("p" + id + "@test");
        providersDAO.save(provider);
        return provider;
    }

    private Supplies saveSupply(Long id, Products product, Providers provider) {
        Supplies supply = new Supplies();
        supply.setId(id);
        supply.setProduct(product);
        supply.setProvider(provider);
        supply.setAmount(BigDecimal.valueOf(10));
        supply.setTime(LocalDateTime.of(2026, 1, 1, 9, 0));
        suppliesDAO.save(supply);
        return supply;
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
        unit.setArrival(LocalDateTime.of(2026, 1, 2, 9, 0));
        unit.setAmount(BigDecimal.valueOf(2));
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