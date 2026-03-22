package ru.msu.cmc.prak.configs;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.hibernate5.LocalSessionFactoryBean;

import javax.sql.DataSource;
import java.util.Properties;

@Configuration
@PropertySource("classpath:application.properties")
public class HibernateDatabaseConfig {

    @Value("${driver}")
    private String DB_DRIVER;

    @Value("${url}")
    private String DB_URL;

    @Value("${username}")
    private String DB_USERNAME;

    @Value("${password}")
    private String DB_PASSWORD;

    @Bean
    public LocalSessionFactoryBean sessionFactory() {
        LocalSessionFactoryBean sessionFactory = new LocalSessionFactoryBean();
        sessionFactory.setDataSource(dataSource());
        // Сканируем пакет с JPA-сущностями
        sessionFactory.setPackagesToScan("ru.msu.cmc.prak.models");

        Properties hibernateProperties = new Properties();
        hibernateProperties.setProperty("hibernate.hbm2ddl.auto", "update");
        // Диалект для PostgreSQL 10 и выше
        hibernateProperties.setProperty("hibernate.dialect", "org.hibernate.dialect.PostgreSQL10Dialect");
        // Размер пула соединений (можно настроить под свои нужды)
        hibernateProperties.setProperty("hibernate.connection.pool_size", "1");
        // Дополнительные полезные настройки
        hibernateProperties.setProperty("hibernate.show_sql", "true");
        hibernateProperties.setProperty("hibernate.format_sql", "true");

        sessionFactory.setHibernateProperties(hibernateProperties);

        return sessionFactory;
    }

    @Bean
    public DataSource dataSource() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName(DB_DRIVER);
        dataSource.setUrl(DB_URL);
        dataSource.setUsername(DB_USERNAME);
        dataSource.setPassword(DB_PASSWORD);
        return dataSource;
    }
}