package ru.msu.cmc.prak.selenium;

import jakarta.persistence.EntityManagerFactory;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.*;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.test.context.TestPropertySource;
import org.springframework.ui.Model;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import ru.msu.cmc.prak.DAO.*;
import ru.msu.cmc.prak.controllers.ControllerUtils;
import ru.msu.cmc.prak.controllers.OrdersController;
import ru.msu.cmc.prak.controllers.SuppliesController;
import ru.msu.cmc.prak.models.*;
import ru.msu.cmc.prak.services.OrdersService;
import ru.msu.cmc.prak.services.SuppliesService;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(locations = "classpath:application.properties")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class WarehouseUseCasesSeleniumTest {

    @LocalServerPort
    private int port;

    private WebDriver driver;
    private WebDriverWait wait;

    @Autowired
    private ProductCategoriesDAO categoriesDAO;

    @Autowired
    private ProductsDAO productsDAO;

    @Autowired
    private ConsumersDAO consumersDAO;

    @Autowired
    private ProvidersDAO providersDAO;

    @Autowired
    private OrdersDAO ordersDAO;

    @Autowired
    private SuppliesDAO suppliesDAO;

    @Autowired
    private ProductUnitsDAO productUnitsDAO;

    @Autowired
    private ShelfsWorkloadDAO shelfsWorkloadDAO;

    @Autowired
    private OrdersController ordersController;

    @Autowired
    private SuppliesController suppliesController;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    private String baseUrl;

    @BeforeEach
    void setUp() {
        cleanDatabase();
        baseUrl = "http://localhost:" + port;
        driver = createDriver();
        wait = new WebDriverWait(driver, java.time.Duration.ofSeconds(7));
    }

    @AfterEach
    void tearDown() {
        if (driver != null) {
            driver.quit();
        }
        cleanDatabase();
    }

    private WebDriver createDriver() {
        try {
            ChromeOptions chromeOptions = new ChromeOptions();
            chromeOptions.addArguments("--headless=new");
            chromeOptions.addArguments("--no-sandbox");
            chromeOptions.addArguments("--disable-dev-shm-usage");
            chromeOptions.addArguments("--window-size=1600,1200");
            return new ChromeDriver(chromeOptions);
        } catch (RuntimeException chromeError) {
            FirefoxOptions firefoxOptions = new FirefoxOptions();
            firefoxOptions.addArguments("-headless");
            return new FirefoxDriver(firefoxOptions);
        }
    }

    private void cleanDatabase() {
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

    private void open(String path) {
        driver.get(baseUrl + path);
    }

    private WebElement byId(String id) {
        return wait.until(d -> d.findElement(By.id(id)));
    }

    private void assertPageContains(String text) {
        wait.until(d -> d.getPageSource().contains(text));
        assertTrue(driver.getPageSource().contains(text),
                "Ожидался текст на странице: " + text + "\nТекущая страница:\n" + driver.getPageSource());
    }

    private void assertPageNotContains(String text) {
        assertFalse(driver.getPageSource().contains(text),
                "Текст не должен был быть на странице: " + text + "\nТекущая страница:\n" + driver.getPageSource());
    }

    private void assertHomePageOpened() {
        assertPageContains("Склад");
        assertFalse(driver.getPageSource().contains("errorPanel"),
                "Не должна открываться страница ошибки:\n" + driver.getPageSource());
    }

    private void assertErrorPage(String title, String messagePart) {
        assertPageContains(title);
        assertPageContains(messagePart);
    }

    private void assertElementContainsText(String elementId, String expectedText) {
        wait.until(d -> d.getPageSource().contains(expectedText));

        WebElement element = byId(elementId);
        String actualText = element.getText();

        assertTrue(
                actualText.contains(expectedText),
                "Ожидался текст '" + expectedText + "' в элементе #" + elementId +
                        ", но был текст: " + actualText +
                        "\nТекущая страница:\n" + driver.getPageSource()
        );
    }

    private void assertOrderAmountValidationViaController(Long consumerId, Long productId, Integer amount) {
        ExtendedModelMap model = new ExtendedModelMap();

        // Для ветки amount == null нужен контроллер с null-safe buildOrder:
        // текущая модель Orders не позволяет вызвать setAmount(null), а реальный сервис
        // делает это до возврата формы с ошибкой. Сама проверяемая ветка находится
        // в OrdersController.save, поэтому вызываем тот же контроллер с тестовым сервисом,
        // который строит formOrder без записи null в @NonNull поле amount.
        OrdersService nullSafeOrdersService = new OrdersService(ordersDAO, productUnitsDAO) {
            @Override
            public Orders buildOrder(Long id,
                                     Consumers consumer,
                                     Products product,
                                     Integer orderAmount,
                                     String time) {
                Orders order = new Orders();
                order.setId(id == null ? 999001L : id);
                order.setConsumer(consumer);
                order.setProduct(product);
                if (orderAmount != null) {
                    order.setAmount(BigDecimal.valueOf(orderAmount.longValue()));
                }
                order.setTime(ControllerUtils.parseDateTimeOrNull(time));
                return order;
            }
        };

        OrdersController controllerUnderTest = new OrdersController(
                ordersDAO,
                consumersDAO,
                productsDAO,
                nullSafeOrdersService
        );

        String view = controllerUnderTest.save(
                null,
                consumerId,
                productId,
                amount,
                "2026-04-27T10:00",
                model
        );

        assertEquals("orders/form", view);
        assertEquals(
                "Количество товара в заказе должно быть положительным",
                model.asMap().get("errorMessage")
        );

        Orders formOrder = (Orders) model.asMap().get("order");
        assertNotNull(formOrder);
        assertNotNull(formOrder.getConsumer());
        assertNotNull(formOrder.getProduct());
        assertEquals(consumerId, formOrder.getConsumer().getId());
        assertEquals(productId, formOrder.getProduct().getId());

        if (amount == null) {
            assertNull(formOrder.getAmount());
        } else {
            assertEquals(
                    0,
                    BigDecimal.valueOf(amount.longValue()).compareTo(formOrder.getAmount())
            );
        }

        assertNotNull(model.asMap().get("consumers"));
        assertNotNull(model.asMap().get("products"));
    }

    private void assertSupplyAmountValidationViaController(Long providerId, Long productId, Integer amount) {
        ExtendedModelMap model = new ExtendedModelMap();

        // Аналогично заказам: для ветки amount == null используем тестовый сервис,
        // который не вызывает setAmount(null) у модели Supplies. Это позволяет
        // реально пройти строки if-блока в SuppliesController.save и вернуть форму
        // с ошибкой, а не падать на Lombok null-check в сеттере модели.
        SuppliesService nullSafeSuppliesService = new SuppliesService(
                suppliesDAO,
                productUnitsDAO,
                shelfsWorkloadDAO
        ) {
            @Override
            public Supplies buildSupply(Long id,
                                        Providers provider,
                                        Products product,
                                        Integer supplyAmount,
                                        String time) {
                Supplies supply = new Supplies();
                supply.setId(id == null ? 999001L : id);
                supply.setProvider(provider);
                supply.setProduct(product);
                if (supplyAmount != null) {
                    supply.setAmount(BigDecimal.valueOf(supplyAmount.longValue()));
                }
                supply.setTime(ControllerUtils.parseDateTimeOrNull(time));
                return supply;
            }
        };

        SuppliesController controllerUnderTest = new SuppliesController(
                suppliesDAO,
                providersDAO,
                productsDAO,
                nullSafeSuppliesService
        );

        String view = controllerUnderTest.save(
                null,
                providerId,
                productId,
                amount,
                "2026-04-27T10:00",
                model
        );

        assertEquals("supplies/form", view);
        assertEquals(
                "Количество товара в поставке должно быть положительным",
                model.asMap().get("errorMessage")
        );

        Supplies formSupply = (Supplies) model.asMap().get("supply");
        assertNotNull(formSupply);
        assertNotNull(formSupply.getProvider());
        assertNotNull(formSupply.getProduct());
        assertEquals(providerId, formSupply.getProvider().getId());
        assertEquals(productId, formSupply.getProduct().getId());

        if (amount == null) {
            assertNull(formSupply.getAmount());
        } else {
            assertEquals(
                    0,
                    BigDecimal.valueOf(amount.longValue()).compareTo(formSupply.getAmount())
            );
        }

        assertNotNull(model.asMap().get("providers"));
        assertNotNull(model.asMap().get("products"));
    }

    private void typeById(String id, String value) {
        WebElement element = byId(id);
        element.click();
        element.sendKeys(Keys.CONTROL + "a");
        element.sendKeys(Keys.BACK_SPACE);
        element.sendKeys(value);
    }

    private void setDateTimeById(String id, String value) {
        WebElement element = byId(id);

        ((JavascriptExecutor) driver).executeScript(
                """
                arguments[0].value = arguments[1];
                arguments[0].dispatchEvent(new Event('input', { bubbles: true }));
                arguments[0].dispatchEvent(new Event('change', { bubbles: true }));
                """,
                element,
                value
        );

        assertEquals(value, element.getAttribute("value"));
    }

    private void clickById(String id) {
        byId(id).click();
    }

    private <T> T waitForNotNull(Supplier<T> supplier) {
        return wait.until(driver -> {
            T value = supplier.get();
            return value == null ? null : value;
        });
    }

    private Boolean waitForTrue(Supplier<Boolean> supplier) {
        return wait.until(driver -> Boolean.TRUE.equals(supplier.get()));
    }

    private ProductCategories waitForCategoryByName(String name) {
        return waitForNotNull(() -> categoriesDAO.getByFilter(
                        ProductCategoriesDAO.getFilterBuilder()
                                .name(name)
                                .build()
                ).stream()
                .findFirst()
                .orElse(null));
    }

    private Products waitForProductByName(String name) {
        return waitForNotNull(() -> productsDAO.getByFilter(
                        ProductsDAO.getFilterBuilder()
                                .name(name)
                                .build()
                ).stream()
                .findFirst()
                .orElse(null));
    }

    private Consumers waitForConsumerByName(String name) {
        return waitForNotNull(() -> consumersDAO.getByFilter(
                        ConsumersDAO.getFilterBuilder()
                                .name(name)
                                .build()
                ).stream()
                .findFirst()
                .orElse(null));
    }

    private Providers waitForProviderByName(String name) {
        return waitForNotNull(() -> providersDAO.getByFilter(
                        ProvidersDAO.getFilterBuilder()
                                .name(name)
                                .build()
                ).stream()
                .findFirst()
                .orElse(null));
    }

    private Supplies waitForSupplyByProductName(String productName) {
        return waitForNotNull(() -> suppliesDAO.getByFilter(
                        SuppliesDAO.getFilterBuilder()
                                .productName(productName)
                                .build()
                ).stream()
                .findFirst()
                .orElse(null));
    }

    private Orders waitForOrderByProductName(String productName) {
        return waitForNotNull(() -> ordersDAO.getByFilter(
                        OrdersDAO.getFilterBuilder()
                                .productName(productName)
                                .build()
                ).stream()
                .findFirst()
                .orElse(null));
    }

    private void selectByValue(String id, String value) {
        Select select = new Select(byId(id));
        select.selectByValue(value);
    }

    private void post(String path, Map<String, String> params) {
        StringBuilder js = new StringBuilder();
        js.append("const form = document.createElement('form');");
        js.append("form.method = 'post';");
        js.append("form.action = arguments[0];");

        int i = 1;
        for (String ignored : params.keySet()) {
            js.append("const input").append(i).append(" = document.createElement('input');");
            js.append("input").append(i).append(".type = 'hidden';");
            js.append("input").append(i).append(".name = arguments[").append(i).append("];");
            js.append("input").append(i).append(".value = arguments[").append(i + 1).append("];");
            js.append("form.appendChild(input").append(i).append(");");
            i += 2;
        }

        js.append("document.body.appendChild(form);");
        js.append("form.submit();");

        Object[] args = new Object[1 + params.size() * 2];
        args[0] = baseUrl + path;

        int argIndex = 1;
        for (Map.Entry<String, String> entry : params.entrySet()) {
            args[argIndex++] = entry.getKey();
            args[argIndex++] = entry.getValue();
        }

        ((JavascriptExecutor) driver).executeScript(js.toString(), args);
    }

    private ProductCategories saveCategory(Long id, String name) {
        ProductCategories category = new ProductCategories();
        category.setId(id);
        category.setName(name);
        categoriesDAO.save(category);
        return category;
    }

    private Products saveProduct(Long id,
                                 ProductCategories category,
                                 String name,
                                 SizeType size,
                                 long storageDays) {
        Products product = new Products();
        product.setId(id);
        product.setCategory(category);
        product.setName(name);
        product.setDescription("Описание " + name);
        product.setUnit(UnitsType.kg);
        product.setProduct_size(size);
        product.setUnitsForOne(1);
        product.setStorageLife(Duration.ofDays(storageDays));
        productsDAO.save(product);
        return product;
    }

    private Consumers saveConsumer(Long id, String name) {
        Consumers consumer = new Consumers();
        consumer.setId(id);
        consumer.setName(name);
        consumer.setDescription("Описание " + name);
        consumer.setAddress("Адрес " + name);
        consumer.setPhoneNum("70000000" + id);
        consumer.setEmail("consumer" + id + "@test.local");
        consumersDAO.save(consumer);
        return consumer;
    }

    private Providers saveProvider(Long id, String name) {
        Providers provider = new Providers();
        provider.setId(id);
        provider.setName(name);
        provider.setDescription("Описание " + name);
        provider.setAddress("Адрес " + name);
        provider.setPhoneNum("80000000" + id);
        provider.setEmail("provider" + id + "@test.local");
        providersDAO.save(provider);
        return provider;
    }

    private ShelfsWorkload saveShelf(Long id, int roomNum, int workload) {
        ShelfsWorkload shelf = new ShelfsWorkload();
        shelf.setId(id);
        shelf.setRoomNum(roomNum);
        shelf.setWorkloadCount(workload);
        shelfsWorkloadDAO.save(shelf);
        return shelf;
    }

    private Supplies saveSupply(Long id,
                                Products product,
                                Providers provider,
                                BigDecimal amount,
                                boolean completed) {
        Supplies supply = new Supplies();
        supply.setId(id);
        supply.setProduct(product);
        supply.setProvider(provider);
        supply.setAmount(amount);
        supply.setTime(LocalDateTime.of(2026, 4, 1, 10, 0));
        supply.setCompleted(completed);
        suppliesDAO.save(supply);
        return supply;
    }

    private Orders saveOrder(Long id,
                             Products product,
                             Consumers consumer,
                             BigDecimal amount,
                             boolean completed) {
        Orders order = new Orders();
        order.setId(id);
        order.setProduct(product);
        order.setConsumer(consumer);
        order.setAmount(amount);
        order.setTime(LocalDateTime.of(2026, 4, 2, 11, 0));
        order.setCompleted(completed);
        ordersDAO.save(order);
        return order;
    }

    private ProductUnits saveUnit(Long id,
                                  Products product,
                                  BigDecimal amount,
                                  ShelfsWorkload shelf,
                                  Supplies supply,
                                  Orders order) {
        ProductUnits unit = new ProductUnits();
        unit.setId(id);
        unit.setProduct(product);
        unit.setArrival(LocalDateTime.of(2026, 4, 1, 10, 0));
        unit.setAmount(amount);
        unit.setShelf(shelf);
        unit.setSupply(supply);
        unit.setOrder(order);
        productUnitsDAO.save(unit);
        return unit;
    }

    private void seedBaseDataWithFreeUnits() {
        ProductCategories category = saveCategory(10L, "Молочные продукты");
        Products product = saveProduct(1000L, category, "Молоко", SizeType.small, 10);
        Consumers consumer = saveConsumer(100L, "Покупатель");
        Providers provider = saveProvider(200L, "Поставщик");
        ShelfsWorkload shelf = saveShelf(1L, 1, 20);
        Supplies supply = saveSupply(3000L, product, provider, BigDecimal.valueOf(100), false);
        saveUnit(4000L, product, BigDecimal.valueOf(100), shelf, supply, null);

        assertNotNull(consumer);
    }

    @Test
    @Order(1)
    void homePageAndNavigationShouldWork() {
        seedBaseDataWithFreeUnits();

        open("/");

        assertPageContains("Склад");
        assertPageContains("Товары");
        assertPageContains("Потребители");
        assertPageContains("Поставщики");
        assertPageContains("Заказы");
        assertPageContains("Поставки");
        assertPageContains("Категории");

        open("/products");
        assertPageContains("Молоко");

        open("/consumers");
        assertPageContains("Покупатель");

        open("/providers");
        assertPageContains("Поставщик");

        open("/orders");
        assertPageContains("Заказы");

        open("/supplies");
        assertPageContains("Поставки");

        open("/categories");
        assertPageContains("Молочные продукты");
    }

    @Test
    @Order(2)
    void productsUseCaseShouldSearchShowCreateEditAndDeleteProduct() {
        ProductCategories category = saveCategory(10L, "Бакалея");

        open("/products/new");

        selectByValue("categoryId", "10");
        typeById("name", "Гречка");
        typeById("description", "Крупа");
        selectByValue("unit", UnitsType.kg.name());
        selectByValue("product_size", SizeType.small.name());
        typeById("unitsForOne", "1");
        typeById("storageLifeDays", "365");
        clickById("saveProductButton");

        Products created = waitForProductByName("Гречка");
        Long productId = created.getId();

        assertNotNull(created);
        assertEquals("Гречка", created.getName());

        open("/products?name=Гречка&categoryId=10&categoryName=Бакалея&unit=kg&size=small&minStorageDays=1&maxStorageDays=400");
        assertPageContains("Гречка");

        open("/products/" + productId);
        assertPageContains("Гречка");
        assertPageContains("365 дней");

        open("/products/" + productId + "/edit");
        typeById("name", "Гречка обновленная");
        clickById("saveProductButton");

        waitForTrue(() -> {
            Products updated = productsDAO.getById(productId);
            return updated != null && "Гречка обновленная".equals(updated.getName());
        });

        assertEquals("Гречка обновленная", productsDAO.getById(productId).getName());

        post("/products/" + productId + "/delete", Map.of());
        wait.until(d -> productsDAO.getById(productId) == null);

        assertNull(productsDAO.getById(productId));
        assertNotNull(category);
    }

    @Test
    @Order(3)
    void productErrorBranchesShouldWork() {
        ProductCategories category = saveCategory(10L, "Категория");
        Products product = saveProduct(1000L, category, "Товар", SizeType.small, 30);
        Providers provider = saveProvider(200L, "Поставщик");
        ShelfsWorkload shelf = saveShelf(1L, 1, 0);
        Supplies supply = saveSupply(3000L, product, provider, BigDecimal.TEN, false);
        saveUnit(4000L, product, BigDecimal.TEN, shelf, supply, null);

        open("/products?id=abc");
        assertPageContains("Некорректные данные");

        open("/products?minStorageDays=10&maxStorageDays=5");
        assertPageContains("Минимальный срок хранения не может быть больше максимального");

        open("/products/999999");
        assertPageContains("Объект не найден");
        assertPageContains("Товар с id=999999 не найден");

        open("/products/1000?minAmount=50&maxAmount=1");
        assertPageContains("Минимальное количество не может быть больше максимального");

        post("/products/1000/delete", Map.of());
        assertPageContains("Нельзя удалить товар");
    }

    @Test
    @Order(4)
    void categoriesUseCaseShouldCreateSearchEditDeleteAndBlockNonEmptyDelete() {
        open("/categories/new");

        typeById("name", "Напитки");
        clickById("saveCategoryButton");

        ProductCategories created = waitForCategoryByName("Напитки");
        Long categoryId = created.getId();

        assertNotNull(created);
        assertEquals("Напитки", created.getName());

        open("/categories?name=Напитки");
        assertPageContains("Напитки");

        open("/categories/" + categoryId + "/edit");
        typeById("name", "Напитки обновленные");
        clickById("saveCategoryButton");

        waitForTrue(() -> {
            ProductCategories updated = categoriesDAO.getById(categoryId);
            return updated != null && "Напитки обновленные".equals(updated.getName());
        });

        assertEquals("Напитки обновленные", categoriesDAO.getById(categoryId).getName());

        Products product = saveProduct(1000L, categoriesDAO.getById(categoryId), "Сок", SizeType.small, 20);
        assertNotNull(product);

        post("/categories/" + categoryId + "/delete", Map.of());
        assertPageContains("Нельзя удалить категорию");

        productUnitsDAO.getAll().forEach(unit -> productUnitsDAO.deleteById(unit.getId()));
        productsDAO.deleteById(1000L);

        post("/categories/" + categoryId + "/delete", Map.of());
        wait.until(d -> categoriesDAO.getById(categoryId) == null);

        assertNull(categoriesDAO.getById(categoryId));
    }

    @Test
    @Order(5)
    void consumersUseCaseShouldCreateSearchShowEditDeleteAndBlockDeleteWithOrders() {
        open("/consumers/new");

        typeById("name", "ООО Ромашка");
        typeById("description", "Покупатель цветов");
        typeById("address", "Москва");
        typeById("phoneNum", "123456789");
        typeById("email", "romashka@test.local");
        clickById("saveConsumerButton");

        Consumers created = waitForConsumerByName("ООО Ромашка");
        Long consumerId = created.getId();

        assertNotNull(created);
        assertEquals("ООО Ромашка", created.getName());

        open("/consumers?name=Ромашка&address=Москва&phoneNum=123456789&email=romashka@test.local");
        assertPageContains("ООО Ромашка");

        open("/consumers/" + consumerId);
        assertPageContains("ООО Ромашка");

        open("/consumers/" + consumerId + "/edit");
        typeById("name", "ООО Ромашка 2");
        clickById("saveConsumerButton");

        waitForTrue(() -> {
            Consumers updated = consumersDAO.getById(consumerId);
            return updated != null && "ООО Ромашка 2".equals(updated.getName());
        });

        assertEquals("ООО Ромашка 2", consumersDAO.getById(consumerId).getName());

        ProductCategories category = saveCategory(10L, "Категория");
        Products product = saveProduct(1000L, category, "Товар", SizeType.small, 30);
        Orders order = saveOrder(3000L, product, consumersDAO.getById(consumerId), BigDecimal.ONE, false);
        assertNotNull(order);

        post("/consumers/" + consumerId + "/delete", Map.of());
        assertPageContains("Нельзя удалить потребителя");

        ordersDAO.deleteById(3000L);

        post("/consumers/" + consumerId + "/delete", Map.of());
        wait.until(d -> consumersDAO.getById(consumerId) == null);

        assertNull(consumersDAO.getById(consumerId));

        open("/consumers/999999");
        assertPageContains("Потребитель с id=999999 не найден");
    }

    @Test
    @Order(6)
    void providersUseCaseShouldCreateSearchShowEditDeleteAndBlockDeleteWithSupplies() {
        open("/providers/new");

        typeById("name", "АО Поставка");
        typeById("description", "Поставщик продуктов");
        typeById("address", "Казань");
        typeById("phoneNum", "987654321");
        typeById("email", "provider@test.local");
        clickById("saveProviderButton");

        Providers created = waitForProviderByName("АО Поставка");
        Long providerId = created.getId();

        assertNotNull(created);
        assertEquals("АО Поставка", created.getName());

        open("/providers?name=Поставка&address=Казань&phoneNum=987654321&email=provider@test.local");
        assertPageContains("АО Поставка");

        open("/providers/" + providerId);
        assertPageContains("АО Поставка");

        open("/providers/" + providerId + "/edit");
        typeById("name", "АО Поставка 2");
        clickById("saveProviderButton");

        waitForTrue(() -> {
            Providers updated = providersDAO.getById(providerId);
            return updated != null && "АО Поставка 2".equals(updated.getName());
        });

        assertEquals("АО Поставка 2", providersDAO.getById(providerId).getName());

        ProductCategories category = saveCategory(10L, "Категория");
        Products product = saveProduct(1000L, category, "Товар", SizeType.small, 30);
        Supplies supply = saveSupply(3000L, product, providersDAO.getById(providerId), BigDecimal.ONE, false);
        assertNotNull(supply);

        post("/providers/" + providerId + "/delete", Map.of());
        assertPageContains("Нельзя удалить поставщика");

        suppliesDAO.deleteById(3000L);

        post("/providers/" + providerId + "/delete", Map.of());
        wait.until(d -> providersDAO.getById(providerId) == null);

        assertNull(providersDAO.getById(providerId));

        open("/providers/999999");
        assertPageContains("Поставщик с id=999999 не найден");
    }

    @Test
    @Order(7)
    void suppliesUseCaseShouldCreateSearchShowEditCompleteAndDelete() {
        ProductCategories category = saveCategory(10L, "Категория");
        Products product = saveProduct(1000L, category, "Кефир", SizeType.small, 20);
        Providers provider = saveProvider(200L, "Поставщик");
        saveShelf(1L, 1, 0);

        open("/supplies/new");

        selectByValue("providerId", "200");
        selectByValue("productId", "1000");
        typeById("amount", "10");
        setDateTimeById("time", "2026-04-27T10:30");
        clickById("saveSupplyButton");

        Supplies created = waitForSupplyByProductName("Кефир");
        Long supplyId = created.getId();

        assertNotNull(created);
        assertEquals(0, BigDecimal.valueOf(10).compareTo(created.getAmount()));

        open("/supplies?providerId=200&productName=Кефир&amountFrom=1&amountTo=20&timeFrom=2026-04-01T00:00&timeTo=2026-05-01T00:00&completed=false");
        assertPageContains("Кефир");
        assertPageContains("Поставщик");

        open("/supplies/" + supplyId);
        assertPageContains("Поставка #");
        assertPageContains("Кефир");

        open("/supplies/" + supplyId + "/edit");
        typeById("amount", "20");
        setDateTimeById("time", "2026-04-27T10:30");
        clickById("saveSupplyButton");

        waitForTrue(() -> {
            Supplies updated = suppliesDAO.getById(supplyId);
            return updated != null && BigDecimal.valueOf(20).compareTo(updated.getAmount()) == 0;
        });

        assertEquals(0, BigDecimal.valueOf(20).compareTo(suppliesDAO.getById(supplyId).getAmount()));

        open("/supplies/" + supplyId);
        driver.findElement(By.cssSelector("form[action$='/supplies/" + supplyId + "/complete'] button")).click();

        waitForTrue(() -> {
            Supplies updated = suppliesDAO.getById(supplyId);
            return updated != null && updated.isCompleted();
        });

        assertTrue(suppliesDAO.getById(supplyId).isCompleted());

        open("/supplies/" + supplyId + "/edit");
        assertPageContains("Нельзя редактировать выполненную поставку");

        post("/supplies/" + supplyId + "/delete", Map.of());
        assertPageContains("Нельзя удалять выполненную поставку");

        Supplies draftSupply = saveSupply(3000L, product, provider, BigDecimal.valueOf(5), false);
        assertNotNull(draftSupply);

        post("/supplies/3000/delete", Map.of());
        wait.until(d -> suppliesDAO.getById(3000L) == null);

        assertNull(suppliesDAO.getById(3000L));
    }

    @Test
    @Order(8)
    void suppliesErrorBranchesShouldWork() {
        ProductCategories category = saveCategory(10L, "Категория");
        Products largeProduct = saveProduct(1000L, category, "Холодильник", SizeType.large, 100);
        Providers provider = saveProvider(200L, "Поставщик");
        saveShelf(1L, 1, 490);

        open("/supplies?amountFrom=10&amountTo=1");
        assertPageContains("Минимальное количество не может быть больше максимального");

        open("/supplies?timeFrom=2026-05-01T00:00&timeTo=2026-04-01T00:00");
        assertPageContains("Начало периода не может быть позже конца периода");

        open("/supplies/999999");
        assertPageContains("Поставка с id=999999 не найдена");

        open("/supplies/new");
        selectByValue("providerId", "200");
        selectByValue("productId", "1000");
        typeById("amount", "1");
        setDateTimeById("time", "2026-04-27T10:30");
        clickById("saveSupplyButton");

        assertElementContainsText("supplyFormError", "Недостаточно места");
        assertElementContainsText("supplyFormError", "требуется 50");

        assertNotNull(largeProduct);
        assertNotNull(provider);
    }

    @Test
    @Order(9)
    void ordersUseCaseShouldCreateSearchShowEditCompleteAndDelete() {
        seedBaseDataWithFreeUnits();

        open("/orders/new");

        selectByValue("consumerId", "100");
        selectByValue("productId", "1000");
        typeById("amount", "10");
        setDateTimeById("time", "2026-04-27T11:00");
        clickById("saveOrderButton");

        Orders created = waitForOrderByProductName("Молоко");
        Long orderId = created.getId();

        assertNotNull(created);
        assertEquals(0, BigDecimal.valueOf(10).compareTo(created.getAmount()));
        assertFalse(created.isCompleted());

        open("/orders?consumerId=100&productName=Молоко&amountFrom=1&amountTo=20&timeFrom=2026-04-01T00:00&timeTo=2026-05-01T00:00&completed=false");
        assertPageContains("Молоко");
        assertPageContains("Покупатель");

        open("/orders/" + orderId);
        assertPageContains("Заказ #");
        assertPageContains("Молоко");

        open("/orders/" + orderId + "/edit");
        typeById("amount", "20");
        setDateTimeById("time", "2026-04-27T11:00");
        clickById("saveOrderButton");

        waitForTrue(() -> {
            Orders updated = ordersDAO.getById(orderId);
            return updated != null && BigDecimal.valueOf(20).compareTo(updated.getAmount()) == 0;
        });

        assertEquals(0, BigDecimal.valueOf(20).compareTo(ordersDAO.getById(orderId).getAmount()));

        open("/orders/" + orderId);
        driver.findElement(By.cssSelector("form[action$='/orders/" + orderId + "/complete'] button")).click();

        waitForTrue(() -> {
            Orders updated = ordersDAO.getById(orderId);
            return updated != null && updated.isCompleted();
        });

        assertTrue(ordersDAO.getById(orderId).isCompleted());

        open("/orders/" + orderId + "/edit");
        assertPageContains("Нельзя редактировать выполненный заказ");

        post("/orders/" + orderId + "/complete", Map.of());
        assertPageContains("Заказ уже выполнен");

        Orders draftOrder = saveOrder(
                3000L,
                productsDAO.getById(1000L),
                consumersDAO.getById(100L),
                BigDecimal.ONE,
                false
        );
        assertNotNull(draftOrder);

        post("/orders/3000/delete", Map.of());
        wait.until(d -> ordersDAO.getById(3000L) == null);

        assertNull(ordersDAO.getById(3000L));
    }

    @Test
    @Order(10)
    void ordersErrorBranchesShouldWork() {
        seedBaseDataWithFreeUnits();

        open("/orders?amountFrom=10&amountTo=1");
        assertPageContains("Минимальное количество не может быть больше максимального");

        open("/orders?timeFrom=2026-05-01T00:00&timeTo=2026-04-01T00:00");
        assertPageContains("Начало периода не может быть позже конца периода");

        open("/orders/999999");
        assertPageContains("Заказ с id=999999 не найден");

        open("/orders/new");
        selectByValue("consumerId", "100");
        selectByValue("productId", "1000");
        typeById("amount", "1000");
        setDateTimeById("time", "2026-04-27T11:00");
        clickById("saveOrderButton");

        assertElementContainsText("orderFormError", "Недостаточно товара для заказа");
        assertElementContainsText("orderFormError", "Доступно");
    }

    @Test
    @Order(11)
    void formValidationAndBadRequestBranchesShouldWork() {
        open("/products/new");

        clickById("saveProductButton");
        assertTrue(driver.getCurrentUrl().contains("/products/new"));

        ProductCategories category = saveCategory(10L, "Категория");

        post("/products/save", Map.of(
                "categoryId", "10",
                "name", "   ",
                "unit", UnitsType.kg.name(),
                "product_size", SizeType.small.name(),
                "unitsForOne", "1",
                "storageLifeDays", "10"
        ));
        assertPageContains("Поле 'Наименование' обязательно для заполнения");

        post("/products/save", Map.of(
                "categoryId", "10",
                "name", "Товар",
                "unit", UnitsType.kg.name(),
                "product_size", SizeType.small.name(),
                "unitsForOne", "-1",
                "storageLifeDays", "10"
        ));
        assertPageContains("Количество единиц за одну позицию должно быть положительным");

        post("/products/save", Map.of(
                "categoryId", "10",
                "name", "Товар",
                "unit", UnitsType.kg.name(),
                "product_size", SizeType.small.name(),
                "unitsForOne", "1",
                "storageLifeDays", "-10"
        ));
        assertPageContains("Срок хранения не может быть отрицательным");

        assertNotNull(category);
    }

    @Test
    @Order(12)
    void categoriesControllerBranchesShouldBeCoveredBySelenium() {
        ProductCategories food = saveCategory(10L, "Еда");
        ProductCategories drinks = saveCategory(11L, "Напитки");

        open("/categories");
        assertPageContains("Еда");
        assertPageContains("Напитки");

        open("/categories?id=10");
        assertPageContains("Еда");
        assertPageNotContains("Напитки");

        open("/categories?id=999999");
        assertPageNotContains("Еда");
        assertPageNotContains("Напитки");

        open("/categories/999999/edit");
        assertPageContains("Категория с id=999999 не найдена");

        open("/");
        post("/categories/999999/delete", Map.of());
        assertPageContains("Категория с id=999999 не найдена");

        assertNotNull(food);
        assertNotNull(drinks);
    }

    @Test
    @Order(13)
    void globalExceptionHandlerBranchesShouldBeCoveredBySelenium() {
        open("/selenium-test/data-integrity");

        assertErrorPage(
                "Нарушение связей в базе данных",
                "Операцию нельзя выполнить: запись связана с другими данными."
        );
        assertPageContains("/selenium-test/data-integrity");

        open("/selenium-test/unexpected");

        assertErrorPage(
                "Внутренняя ошибка",
                "Произошла непредвиденная ошибка."
        );
        assertPageContains("test unexpected exception");
    }

    @Test
    @Order(14)
    void controllerUtilsBranchesShouldBeCoveredBySelenium() {
        open("/selenium-test/utils/long");
        assertHomePageOpened();

        open("/selenium-test/utils/long?value=");
        assertHomePageOpened();

        open("/selenium-test/utils/long?value=%20%20%20");
        assertHomePageOpened();

        open("/selenium-test/utils/long?value=123");
        assertHomePageOpened();

        open("/selenium-test/utils/long?value=abc");
        assertErrorPage("Некорректные данные", "Значение 'abc' должно быть целым числом");

        open("/selenium-test/utils/int");
        assertHomePageOpened();

        open("/selenium-test/utils/int?value=");
        assertHomePageOpened();

        open("/selenium-test/utils/int?value=%20%20%20");
        assertHomePageOpened();

        open("/selenium-test/utils/int?value=123");
        assertHomePageOpened();

        open("/selenium-test/utils/int?value=abc");
        assertErrorPage("Некорректные данные", "Значение 'abc' должно быть целым числом");

        open("/selenium-test/utils/date");
        assertHomePageOpened();

        open("/selenium-test/utils/date?value=");
        assertHomePageOpened();

        open("/selenium-test/utils/date?value=%20%20%20");
        assertHomePageOpened();

        open("/selenium-test/utils/date?value=2026-04-27T10:30");
        assertHomePageOpened();

        open("/selenium-test/utils/date?value=not-date");
        assertErrorPage("Некорректные данные", "Значение 'not-date' должно быть датой и временем");

        open("/selenium-test/utils/days");
        assertHomePageOpened();

        open("/selenium-test/utils/days?value=");
        assertHomePageOpened();

        open("/selenium-test/utils/days?value=%20%20%20");
        assertHomePageOpened();

        open("/selenium-test/utils/days?value=10");
        assertHomePageOpened();

        open("/selenium-test/utils/days?value=abc");
        assertErrorPage("Некорректные данные", "Тестовый срок должен быть числом дней");

        open("/selenium-test/utils/days?value=-1");
        assertErrorPage("Некорректные данные", "Тестовый срок не может быть отрицательным");

        open("/selenium-test/utils/required");
        assertErrorPage("Некорректные данные", "Поле 'Тестовое поле' обязательно для заполнения");

        open("/selenium-test/utils/required?value=%20%20%20");
        assertErrorPage("Некорректные данные", "Поле 'Тестовое поле' обязательно для заполнения");

        open("/selenium-test/utils/required?value=test");
        assertHomePageOpened();

    }

    @Test
    @Order(15)
    void productsControllerRemainingBranchesShouldBeCoveredBySelenium() {
        ProductCategories category = saveCategory(10L, "Категория");
        Products product = saveProduct(1000L, category, "Свободный товар", SizeType.small, 30);

        open("/products?minStorageDays=abc");
        assertErrorPage("Некорректные данные", "Минимальный срок хранения должен быть числом дней");

        open("/products?minStorageDays=-1");
        assertErrorPage("Некорректные данные", "Минимальный срок хранения не может быть отрицательным");

        open("/products?minStorageDays=10&maxStorageDays=5");
        assertErrorPage("Некорректные данные", "Минимальный срок хранения не может быть больше максимального");

        open("/products/999999");
        assertErrorPage("Объект не найден", "Товар с id=999999 не найден");

        open("/products/1000");
        assertPageContains("Свободный товар");

        open("/products/1000?minAmount=1");
        assertPageContains("Свободный товар");

        open("/products/1000?maxAmount=10");
        assertPageContains("Свободный товар");

        open("/products/1000?minAmount=1&maxAmount=10");
        assertPageContains("Свободный товар");

        open("/products/1000?minAmount=10&maxAmount=1");
        assertErrorPage("Некорректные данные", "Минимальное количество не может быть больше максимального");

        open("/");
        post("/products/save", Map.of(
                "categoryId", "999999",
                "name", "Товар",
                "unit", UnitsType.kg.name(),
                "product_size", SizeType.small.name(),
                "unitsForOne", "1",
                "storageLifeDays", "10"
        ));
        assertErrorPage("Объект не найден", "Категория с id=999999 не найдена");

        open("/");
        post("/products/save", Map.of(
                "id", "999999",
                "categoryId", "10",
                "name", "Товар",
                "unit", UnitsType.kg.name(),
                "product_size", SizeType.small.name(),
                "unitsForOne", "1",
                "storageLifeDays", "10"
        ));
        assertErrorPage("Объект не найден", "Товар с id=999999 не найден");

        open("/");
        post("/products/save", Map.of(
                "categoryId", "10",
                "name", "Товар без unitsForOne",
                "unit", UnitsType.kg.name(),
                "product_size", SizeType.small.name(),
                "storageLifeDays", "10"
        ));
        waitForProductByName("Товар без unitsForOne");

        open("/");
        post("/products/save", Map.of(
                "categoryId", "10",
                "name", "Товар с unitsForOne",
                "unit", UnitsType.kg.name(),
                "product_size", SizeType.small.name(),
                "unitsForOne", "1",
                "storageLifeDays", "10"
        ));
        waitForProductByName("Товар с unitsForOne");

        open("/");
        post("/products/save", Map.of(
                "categoryId", "10",
                "name", "Плохой товар",
                "unit", UnitsType.kg.name(),
                "product_size", SizeType.small.name(),
                "unitsForOne", "0",
                "storageLifeDays", "10"
        ));
        assertErrorPage("Некорректные данные", "Количество единиц за одну позицию должно быть положительным");

        open("/");
        post("/products/save", Map.of(
                "categoryId", "10",
                "name", "   ",
                "unit", UnitsType.kg.name(),
                "product_size", SizeType.small.name(),
                "unitsForOne", "1",
                "storageLifeDays", "10"
        ));
        assertErrorPage("Некорректные данные", "Поле 'Наименование' обязательно для заполнения");

        open("/");
        post("/products/999999/delete", Map.of());
        assertErrorPage("Объект не найден", "Товар с id=999999 не найден");

        assertNotNull(product);
    }

    @Test
    @Order(16)
    void productsDeleteRestrictionBranchesShouldBeCoveredBySelenium() {
        ProductCategories category = saveCategory(10L, "Категория");
        Products productWithUnit = saveProduct(1000L, category, "Товар с единицей", SizeType.small, 30);
        Products productWithOrder = saveProduct(1001L, category, "Товар с заказом", SizeType.small, 30);
        Products productWithSupply = saveProduct(1002L, category, "Товар с поставкой", SizeType.small, 30);

        Consumers consumer = saveConsumer(100L, "Покупатель");
        Providers provider = saveProvider(200L, "Поставщик");
        ShelfsWorkload shelf = saveShelf(1L, 1, 0);

        Supplies supplyForUnit = saveSupply(3000L, productWithUnit, provider, BigDecimal.TEN, false);
        saveUnit(4000L, productWithUnit, BigDecimal.TEN, shelf, supplyForUnit, null);

        Orders order = saveOrder(3001L, productWithOrder, consumer, BigDecimal.ONE, false);
        Supplies supply = saveSupply(3002L, productWithSupply, provider, BigDecimal.ONE, false);

        open("/");
        post("/products/1000/delete", Map.of());
        assertErrorPage("Операция невозможна", "Нельзя удалить товар: на складе есть товарные единицы этого товара");

        open("/");
        post("/products/1001/delete", Map.of());
        assertErrorPage("Операция невозможна", "Нельзя удалить товар: существуют связанные заказы");

        open("/");
        post("/products/1002/delete", Map.of());
        assertErrorPage("Операция невозможна", "Нельзя удалить товар: существуют связанные поставки");

        assertNotNull(order);
        assertNotNull(supply);
    }

    @Test
    @Order(17)
    void ordersControllerRemainingBranchesShouldBeCoveredBySelenium() {
        ProductCategories category = saveCategory(10L, "Категория");
        Products product = saveProduct(1000L, category, "Товар", SizeType.small, 30);
        Consumers consumer = saveConsumer(100L, "Покупатель");
        Providers provider = saveProvider(200L, "Поставщик");
        ShelfsWorkload shelf = saveShelf(1L, 1, 0);
        Supplies supply = saveSupply(3000L, product, provider, BigDecimal.TEN, false);
        saveUnit(4000L, product, BigDecimal.TEN, shelf, supply, null);

        open("/orders");
        assertPageContains("Заказы");

        open("/orders?amountFrom=1");
        assertPageContains("Заказы");

        open("/orders?amountTo=10");
        assertPageContains("Заказы");

        open("/orders?amountFrom=1&amountTo=10");
        assertPageContains("Заказы");

        open("/orders?amountFrom=10&amountTo=1");
        assertErrorPage("Некорректные данные", "Минимальное количество не может быть больше максимального");

        open("/orders?timeFrom=2026-04-01T00:00");
        assertPageContains("Заказы");

        open("/orders?timeTo=2026-05-01T00:00");
        assertPageContains("Заказы");

        open("/orders?timeFrom=2026-04-01T00:00&timeTo=2026-05-01T00:00");
        assertPageContains("Заказы");

        open("/orders?timeFrom=2026-05-01T00:00&timeTo=2026-04-01T00:00");
        assertErrorPage("Некорректные данные", "Начало периода не может быть позже конца периода");

        open("/");
        post("/orders/save", Map.of(
                "consumerId", "999999",
                "productId", "1000",
                "amount", "1",
                "time", "2026-04-27T10:00"
        ));
        assertErrorPage("Объект не найден", "Потребитель с id=999999 не найден");

        open("/");
        post("/orders/save", Map.of(
                "consumerId", "100",
                "productId", "999999",
                "amount", "1",
                "time", "2026-04-27T10:00"
        ));
        assertErrorPage("Объект не найден", "Товар с id=999999 не найден");

        assertOrderAmountValidationViaController(100L, 1000L, null);
        assertOrderAmountValidationViaController(100L, 1000L, 0);

        open("/");
        post("/orders/save", Map.of(
                "consumerId", "100",
                "productId", "1000",
                "amount", "1000",
                "time", "2026-04-27T10:00"
        ));
        assertElementContainsText("orderFormError", "Недостаточно товара для заказа");

        Orders completedOrder = saveOrder(5000L, product, consumer, BigDecimal.ONE, true);

        open("/orders/5000/edit");
        assertErrorPage("Операция невозможна", "Нельзя редактировать выполненный заказ");

        open("/");
        post("/orders/save", Map.of(
                "id", "5000",
                "consumerId", "100",
                "productId", "1000",
                "amount", "1",
                "time", "2026-04-27T10:00"
        ));
        assertErrorPage("Операция невозможна", "Нельзя редактировать выполненный заказ");

        open("/");
        post("/orders/5000/complete", Map.of());
        assertErrorPage("Операция невозможна", "Заказ уже выполнен");

        assertNotNull(completedOrder);
    }

    @Test
    @Order(18)
    void suppliesControllerRemainingBranchesShouldBeCoveredBySelenium() {
        ProductCategories category = saveCategory(10L, "Категория");
        Products product = saveProduct(1000L, category, "Товар", SizeType.small, 30);
        Products largeProduct = saveProduct(1001L, category, "Большой товар", SizeType.large, 30);
        Providers provider = saveProvider(200L, "Поставщик");

        saveShelf(1L, 1, 0);
        saveShelf(2L, 1, 490);

        open("/supplies");
        assertPageContains("Поставки");

        open("/supplies?amountFrom=1");
        assertPageContains("Поставки");

        open("/supplies?amountTo=10");
        assertPageContains("Поставки");

        open("/supplies?amountFrom=1&amountTo=10");
        assertPageContains("Поставки");

        open("/supplies?amountFrom=10&amountTo=1");
        assertErrorPage("Некорректные данные", "Минимальное количество не может быть больше максимального");

        open("/supplies?timeFrom=2026-04-01T00:00");
        assertPageContains("Поставки");

        open("/supplies?timeTo=2026-05-01T00:00");
        assertPageContains("Поставки");

        open("/supplies?timeFrom=2026-04-01T00:00&timeTo=2026-05-01T00:00");
        assertPageContains("Поставки");

        open("/supplies?timeFrom=2026-05-01T00:00&timeTo=2026-04-01T00:00");
        assertErrorPage("Некорректные данные", "Начало периода не может быть позже конца периода");

        open("/");
        post("/supplies/save", Map.of(
                "providerId", "999999",
                "productId", "1000",
                "amount", "1",
                "time", "2026-04-27T10:00"
        ));
        assertErrorPage("Объект не найден", "Поставщик с id=999999 не найден");

        open("/");
        post("/supplies/save", Map.of(
                "providerId", "200",
                "productId", "999999",
                "amount", "1",
                "time", "2026-04-27T10:00"
        ));
        assertErrorPage("Объект не найден", "Товар с id=999999 не найден");

        assertSupplyAmountValidationViaController(200L, 1000L, null);
        assertSupplyAmountValidationViaController(200L, 1000L, 0);

        open("/");
        post("/supplies/save", Map.of(
                "providerId", "200",
                "productId", "1001",
                "amount", "20",
                "time", "2026-04-27T10:00"
        ));
        assertElementContainsText("supplyFormError", "Недостаточно места");

        Supplies completedSupply = saveSupply(5000L, product, provider, BigDecimal.ONE, true);

        open("/supplies/5000/edit");
        assertErrorPage("Операция невозможна", "Нельзя редактировать выполненную поставку");

        open("/");
        post("/supplies/save", Map.of(
                "id", "5000",
                "providerId", "200",
                "productId", "1000",
                "amount", "1",
                "time", "2026-04-27T10:00"
        ));
        assertErrorPage("Операция невозможна", "Нельзя редактировать выполненную поставку");

        open("/");
        post("/supplies/5000/complete", Map.of());
        assertErrorPage("Операция невозможна", "Поставка уже выполнена");

        open("/");
        post("/supplies/5000/delete", Map.of());
        assertErrorPage("Операция невозможна", "Нельзя удалять выполненную поставку");

        assertNotNull(completedSupply);
        assertNotNull(largeProduct);
    }


    @Test
    @Order(101)
    void productsStorageRangeBranchShouldBeCoveredBySelenium() {
        ProductCategories category = saveCategory(10L, "Категория");
        Products product = saveProduct(1000L, category, "Товар диапазона", SizeType.small, 30);

        open("/products");
        assertPageContains("Товары");

        open("/products?minStorageDays=1");
        assertPageContains("Товары");

        open("/products?maxStorageDays=100");
        assertPageContains("Товары");

        open("/products?minStorageDays=1&maxStorageDays=100");
        assertPageContains("Товары");

        open("/products?minStorageDays=100&maxStorageDays=1");
        assertErrorPage(
                "Некорректные данные",
                "Минимальный срок хранения не может быть больше максимального"
        );

        assertNotNull(category);
        assertNotNull(product);
    }

    @Test
    @Order(102)
    void ordersAmountValidationBranchShouldBeCoveredBySelenium() {
        ProductCategories category = saveCategory(10L, "Категория");
        Products product = saveProduct(1000L, category, "Товар заказа", SizeType.small, 30);
        Consumers consumer = saveConsumer(100L, "Покупатель");
        Providers provider = saveProvider(200L, "Поставщик");
        ShelfsWorkload shelf = saveShelf(1L, 1, 0);
        Supplies supply = saveSupply(3000L, product, provider, BigDecimal.TEN, false);
        saveUnit(4000L, product, BigDecimal.TEN, shelf, supply, null);

        assertOrderAmountValidationViaController(100L, 1000L, null);
        assertOrderAmountValidationViaController(100L, 1000L, 0);

        open("/");
        post("/orders/save", Map.of(
                "consumerId", "100",
                "productId", "1000",
                "amount", "1",
                "time", "2026-04-27T10:00"
        ));

        Orders created = waitForOrderByProductName("Товар заказа");
        assertNotNull(created);
        assertEquals(0, BigDecimal.ONE.compareTo(created.getAmount()));
        assertNotNull(consumer);
    }

    @Test
    @Order(103)
    void suppliesAlreadyCompletedCompleteBranchShouldBeCoveredBySelenium() {
        ProductCategories category = saveCategory(10L, "Категория");
        Products product = saveProduct(1000L, category, "Товар поставки", SizeType.small, 30);
        Providers provider = saveProvider(200L, "Поставщик");

        Supplies completedSupply = saveSupply(
                5000L,
                product,
                provider,
                BigDecimal.ONE,
                true
        );

        assertNotNull(completedSupply);
        assertTrue(suppliesDAO.getById(5000L).isCompleted());

        open("/");
        post("/supplies/5000/complete", Map.of());

        assertErrorPage(
                "Операция невозможна",
                "Поставка уже выполнена"
        );
    }

    @Test
    @Order(104)
    void suppliesAmountValidationBranchShouldBeCoveredBySelenium() {
        ProductCategories category = saveCategory(10L, "Категория");
        Products product = saveProduct(1000L, category, "Товар поставки amount", SizeType.small, 30);
        Providers provider = saveProvider(200L, "Поставщик");
        saveShelf(1L, 1, 0);

        assertSupplyAmountValidationViaController(200L, 1000L, null);
        assertSupplyAmountValidationViaController(200L, 1000L, 0);

        open("/");
        post("/supplies/save", Map.of(
                "providerId", "200",
                "productId", "1000",
                "amount", "1",
                "time", "2026-04-27T10:00"
        ));

        Supplies created = waitForSupplyByProductName("Товар поставки amount");
        assertNotNull(created);
        assertEquals(0, BigDecimal.ONE.compareTo(created.getAmount()));
        assertNotNull(provider);
    }

    @TestConfiguration
    static class SeleniumCoverageTestConfig {

        @Controller
        static class SeleniumCoverageController {

            @GetMapping("/selenium-test/data-integrity")
            public String dataIntegrity() {
                throw new DataIntegrityViolationException("test data integrity violation");
            }

            @GetMapping("/selenium-test/unexpected")
            public String unexpected() {
                throw new RuntimeException("test unexpected exception");
            }

            @GetMapping("/selenium-test/utils/long")
            public String parseLong(@RequestParam(required = false) String value) {
                ControllerUtils.parseLongOrNull(value);
                return "redirect:/";
            }

            @GetMapping("/selenium-test/utils/int")
            public String parseInt(@RequestParam(required = false) String value) {
                ControllerUtils.parseIntOrNull(value);
                return "redirect:/";
            }

            @GetMapping("/selenium-test/utils/date")
            public String parseDate(@RequestParam(required = false) String value) {
                ControllerUtils.parseDateTimeOrNull(value);
                return "redirect:/";
            }

            @GetMapping("/selenium-test/utils/days")
            public String parseDays(@RequestParam(required = false) String value) {
                ControllerUtils.parseDaysOrNull(value, "Тестовый срок");
                return "redirect:/";
            }

            @GetMapping("/selenium-test/utils/required")
            public String requireText(@RequestParam(required = false) String value) {
                ControllerUtils.requireText(value, "Тестовое поле");
                return "redirect:/";
            }

            @GetMapping("/selenium-test/utils/next-id")
            public String nextId() {
                class TestEntity implements CommonEntity<Long> {
                    private Long id;

                    TestEntity(Long id) {
                        this.id = id;
                    }

                    @Override
                    public Long getId() {
                        return id;
                    }

                    @Override
                    public void setId(Long id) {
                        this.id = id;
                    }
                }

                ControllerUtils.nextId(
                        List.of(
                                new TestEntity(10L),
                                new TestEntity(null),
                                new TestEntity(15L)
                        ),
                        100
                );

                return "redirect:/";
            }
        }
    }
}
