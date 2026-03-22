create table test (
                      id bigint
);

select 69;

drop table test;

CREATE TYPE size_type AS ENUM ('small', 'middle', 'large');
CREATE TYPE units_type AS ENUM ('g', 'kg');



-- Таблица категорий продуктов
CREATE TABLE product_categories (
                                    category_id BIGINT PRIMARY KEY,
                                    name TEXT NOT NULL
);

-- Таблица продуктов
CREATE TABLE products (
                          product_id BIGINT PRIMARY KEY,
                          category_id BIGINT NOT NULL REFERENCES product_categories(category_id),
                          name TEXT NOT NULL,
                          description TEXT,
                          unit  units_type, --  единица измерения
                          size  size_type, --  размер
                          units_for_one INTEGER,
                          storage_life INTERVAL
);

-- Таблица поставщиков
CREATE TABLE providers (
                           provider_id BIGINT PRIMARY KEY,
                           name TEXT NOT NULL,
                           description TEXT,
                           address TEXT,
                           phone_num TEXT,
                           email TEXT
);

-- Таблица потребителей (клиентов)
CREATE TABLE consumers (
                           consumer_id BIGINT PRIMARY KEY,
                           name TEXT NOT NULL,
                           description TEXT,
                           address TEXT,
                           phone_num TEXT,
                           email TEXT
);

-- Таблица полок с нагрузкой
CREATE TABLE shelfs_workload (
                                 shelf_num BIGINT PRIMARY KEY,
                                 room_num INTEGER NOT NULL,
                                 workload_count INTEGER NOT NULL
);

-- Таблица поставок
CREATE TABLE supplies (
                          supply_id BIGINT PRIMARY KEY,
                          product_id BIGINT NOT NULL REFERENCES products(product_id),
                          amount NUMERIC NOT NULL,
                          time TIMESTAMP NOT NULL,
                          provider_id BIGINT NOT NULL REFERENCES providers(provider_id),
                          completed BOOLEAN NOT NULL DEFAULT FALSE
);

-- Таблица заказов
CREATE TABLE orders (
                        order_id BIGINT PRIMARY KEY,
                        product_id BIGINT NOT NULL REFERENCES products(product_id),
                        amount NUMERIC NOT NULL,
                        time TIMESTAMP NOT NULL,
                        consumer_id BIGINT NOT NULL REFERENCES consumers(consumer_id),
                        completed BOOLEAN NOT NULL DEFAULT FALSE
);

-- Таблица учёта единиц товара (партий)
CREATE TABLE product_units (
                               units_id BIGINT PRIMARY KEY,
                               product_id BIGINT NOT NULL REFERENCES products(product_id),
                               arrival TIMESTAMP NOT NULL,
                               amount NUMERIC NOT NULL,
                               shelf_num BIGINT NOT NULL REFERENCES shelfs_workload(shelf_num),
                               supply_id BIGINT REFERENCES supplies(supply_id),
                               order_id BIGINT REFERENCES orders(order_id)
);