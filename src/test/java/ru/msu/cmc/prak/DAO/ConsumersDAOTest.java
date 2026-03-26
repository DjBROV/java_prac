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
public class ConsumersDAOTest {

    @Autowired
    private ConsumersDAO consumersDAO;
    @Autowired
    private OrdersDAO ordersDAO;
    @Autowired
    private ProductsDAO productsDAO;
    @Autowired
    private ProductCategoriesDAO productCategoriesDAO;
    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Test
    void testGetAllByName() {
        saveConsumer(1L, "Иван");
        saveConsumer(2L, "Иванов");

        List<Consumers> found = consumersDAO.getAllByName("Иван");
        assertEquals(2, found.size());
    }

    @Test
    void testGetSingleByName() {
        saveConsumer(1L, "Пётр");

        Consumers found = consumersDAO.getSingleByName("Пётр");
        assertNotNull(found);
        assertEquals(1L, found.getId());
    }

    @Test
    void testGetByPhoneNum() {
        saveConsumer(1L, "A", "12345", "a@test");
        saveConsumer(2L, "B", "12399", "b@test");

        List<Consumers> found = consumersDAO.getByPhoneNum("123");
        assertEquals(2, found.size());
    }

    @Test
    void testGetByEmail() {
        saveConsumer(1L, "A", "111", "ivan@test");
        saveConsumer(2L, "B", "222", "petr@test");

        List<Consumers> found = consumersDAO.getByEmail("ivan");
        assertEquals(1, found.size());
        assertEquals(1L, found.getFirst().getId());
    }

    @Test
    void testGetByFilter() {
        Consumers consumer = new Consumers();
        consumer.setId(1L);
        consumer.setName("Иван Петров");
        consumer.setAddress("Москва");
        consumer.setPhoneNum("123456");
        consumer.setEmail("ivan@test");
        consumersDAO.save(consumer);

        ConsumersDAO.Filter filter = ConsumersDAO.getFilterBuilder()
                .name("Иван")
                .address("Моск")
                .phoneNum("123")
                .email("ivan")
                .build();

        List<Consumers> found = consumersDAO.getByFilter(filter);
        assertEquals(1, found.size());
        assertEquals(1L, found.getFirst().getId());
    }

    @Test
    void testGetOrdersByConsumer() {
        Consumers consumer = saveConsumer(1L, "Иван");
        ProductCategories category = saveCategory(1L, "Техника");
        Products product = saveProduct(1L, category, "Телефон");

        saveOrder(1L, product, consumer);
        saveOrder(2L, product, consumer);

        List<Orders> orders = consumersDAO.getOrdersByConsumer(consumer);
        assertEquals(2, orders.size());
    }

    private Consumers saveConsumer(Long id, String name) {
        return saveConsumer(id, name, "100", "c" + id + "@test");
    }

    private Consumers saveConsumer(Long id, String name, String phone, String email) {
        Consumers consumer = new Consumers();
        consumer.setId(id);
        consumer.setName(name);
        consumer.setPhoneNum(phone);
        consumer.setEmail(email);
        consumersDAO.save(consumer);
        return consumer;
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
        product.setProduct_size(SizeType.small);
        product.setUnitsForOne(1);
        product.setStorageLife(Duration.ofDays(30));
        productsDAO.save(product);
        return product;
    }

    private Orders saveOrder(Long id, Products product, Consumers consumer) {
        Orders order = new Orders();
        order.setId(id);
        order.setProduct(product);
        order.setConsumer(consumer);
        order.setAmount(BigDecimal.valueOf(3));
        order.setTime(LocalDateTime.of(2026, 1, 1, 10, 0));
        ordersDAO.save(order);
        return order;
    }

    @BeforeAll
    @AfterEach
    void annihilation() {
        SessionFactory sessionFactory = entityManagerFactory.unwrap(SessionFactory.class);
        try (Session session = sessionFactory.openSession()) {
            session.beginTransaction();
            session.createNativeQuery("TRUNCATE TABLE orders CASCADE").executeUpdate();
            session.createNativeQuery("TRUNCATE TABLE products CASCADE").executeUpdate();
            session.createNativeQuery("TRUNCATE TABLE consumers CASCADE").executeUpdate();
            session.createNativeQuery("TRUNCATE TABLE product_categories CASCADE").executeUpdate();
            session.getTransaction().commit();
        }
    }
}