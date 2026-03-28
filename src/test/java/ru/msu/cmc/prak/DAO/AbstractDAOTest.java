package ru.msu.cmc.prak.DAO;

import jakarta.persistence.EntityManagerFactory;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import ru.msu.cmc.prak.models.*;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestPropertySource(locations = "classpath:application.properties")
public abstract class AbstractDAOTest {

    @Autowired
    protected ProductCategoriesDAO productCategoriesDAO;
    @Autowired
    protected ProductsDAO productsDAO;
    @Autowired
    protected ConsumersDAO consumersDAO;
    @Autowired
    protected ProvidersDAO providersDAO;
    @Autowired
    protected OrdersDAO ordersDAO;
    @Autowired
    protected SuppliesDAO suppliesDAO;
    @Autowired
    protected ProductUnitsDAO productUnitsDAO;
    @Autowired
    protected ShelfsWorkloadDAO shelfsWorkloadDAO;
    @Autowired
    protected EntityManagerFactory entityManagerFactory;

    protected ProductCategories saveCategory(Long id, String name) {
        ProductCategories category = new ProductCategories();
        category.setId(id);
        category.setName(name);
        productCategoriesDAO.save(category);
        return category;
    }

    protected Products saveProduct(Long id, ProductCategories category, String name) {
        return saveProduct(id, category, name, UnitsType.kg, SizeType.small, 1, 30, null);
    }

    protected Products saveProduct(Long id, ProductCategories category, String name,
                                   UnitsType unit, SizeType size, Integer unitsForOne,
                                   long storageDays, String description) {
        Products product = new Products();
        product.setId(id);
        product.setCategory(category);
        product.setName(name);
        product.setDescription(description);
        product.setUnit(unit);
        product.setProduct_size(size);
        product.setUnitsForOne(unitsForOne);
        product.setStorageLife(Duration.ofDays(storageDays));
        productsDAO.save(product);
        return product;
    }

    protected Consumers saveConsumer(Long id, String name) {
        return saveConsumer(id, name, null, null, "100" + id, "consumer" + id + "@test.local");
    }

    protected Consumers saveConsumer(Long id, String name, String description,
                                     String address, String phone, String email) {
        Consumers consumer = new Consumers();
        consumer.setId(id);
        consumer.setName(name);
        consumer.setDescription(description);
        consumer.setAddress(address);
        consumer.setPhoneNum(phone);
        consumer.setEmail(email);
        consumersDAO.save(consumer);
        return consumer;
    }

    protected Providers saveProvider(Long id, String name) {
        return saveProvider(id, name, null, null, "200" + id, "provider" + id + "@test.local");
    }

    protected Providers saveProvider(Long id, String name, String description,
                                     String address, String phone, String email) {
        Providers provider = new Providers();
        provider.setId(id);
        provider.setName(name);
        provider.setDescription(description);
        provider.setAddress(address);
        provider.setPhoneNum(phone);
        provider.setEmail(email);
        providersDAO.save(provider);
        return provider;
    }

    protected ShelfsWorkload saveShelf(Long id, Integer roomNum, Integer workloadCount) {
        ShelfsWorkload shelf = new ShelfsWorkload();
        shelf.setId(id);
        shelf.setRoomNum(roomNum);
        shelf.setWorkloadCount(workloadCount);
        shelfsWorkloadDAO.save(shelf);
        return shelf;
    }

    protected Orders saveOrder(Long id, Products product, Consumers consumer,
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

    protected Supplies saveSupply(Long id, Products product, Providers provider,
                                  BigDecimal amount, LocalDateTime time, boolean completed) {
        Supplies supply = new Supplies();
        supply.setId(id);
        supply.setProduct(product);
        supply.setProvider(provider);
        supply.setAmount(amount);
        supply.setTime(time);
        supply.setCompleted(completed);
        suppliesDAO.save(supply);
        return supply;
    }

    protected ProductUnits saveUnit(Long id, Products product, LocalDateTime arrival,
                                    BigDecimal amount, ShelfsWorkload shelf,
                                    Supplies supply, Orders order) {
        ProductUnits unit = new ProductUnits();
        unit.setId(id);
        unit.setProduct(product);
        unit.setArrival(arrival);
        unit.setAmount(amount);
        unit.setShelf(shelf);
        unit.setSupply(supply);
        unit.setOrder(order);
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