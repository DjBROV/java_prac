package ru.msu.cmc.prak.DAO.impl;

import org.hibernate.Session;
import org.hibernate.query.Query;
import org.springframework.stereotype.Repository;
import ru.msu.cmc.prak.DAO.ProductsDAO;
import ru.msu.cmc.prak.models.*;

import javax.persistence.criteria.*;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Repository
public class ProductsDAOImpl extends CommonDAOImpl<Products, Long> implements ProductsDAO {

    public ProductsDAOImpl() {
        super(Products.class);
    }

    @Override
    public List<Products> getAllByName(String name) {
        try (Session session = sessionFactory.openSession()) {
            Query<Products> query = session.createQuery(
                    "FROM Products WHERE name LIKE :name", Products.class);
            query.setParameter("name", likeExpr(name));
            return query.getResultList();
        }
    }

    @Override
    public Products getSingleByName(String name) {
        List<Products> candidates = getAllByName(name);
        return candidates.size() == 1 ? candidates.get(0) : null;
    }

    @Override
    public List<Products> getByCategoryId(Long categoryId) {
        // categoryId не нужен, используем объект категории, но для совместимости можно сделать через join
        try (Session session = sessionFactory.openSession()) {
            Query<Products> query = session.createQuery(
                    "FROM Products WHERE category.id = :catId", Products.class);
            query.setParameter("catId", categoryId);
            return query.getResultList();
        }
    }

    @Override
    public List<Products> getByUnit(UnitsType unit) {
        try (Session session = sessionFactory.openSession()) {
            Query<Products> query = session.createQuery(
                    "FROM Products WHERE unit = :unit", Products.class);
            query.setParameter("unit", unit);
            return query.getResultList();
        }
    }

    @Override
    public List<Products> getBySize(SizeType size) {
        try (Session session = sessionFactory.openSession()) {
            Query<Products> query = session.createQuery(
                    "FROM Products WHERE product_size = :size", Products.class);
            query.setParameter("size", size);
            return query.getResultList();
        }
    }

    @Override
    public List<Products> getByFilter(Filter filter) {
        try (Session session = sessionFactory.openSession()) {
            CriteriaBuilder builder = session.getCriteriaBuilder();
            CriteriaQuery<Products> criteriaQuery = builder.createQuery(Products.class);
            Root<Products> root = criteriaQuery.from(Products.class);

            List<Predicate> predicates = new ArrayList<>();

            if (filter.getId() != null) {
                predicates.add(builder.equal(root.get("id"), filter.getId()));
            }
            if (filter.getName() != null) {
                predicates.add(builder.like(root.get("name"), likeExpr(filter.getName())));
            }
            if (filter.getCategoryId() != null) {
                // фильтр по ID категории
                predicates.add(builder.equal(root.get("category").get("id"), filter.getCategoryId()));
            }
            if (filter.getCategoryName() != null) {
                predicates.add(builder.like(root.get("category").get("name"), likeExpr(filter.getCategoryName())));
            }
            if (filter.getUnit() != null) {
                predicates.add(builder.equal(root.get("unit"), filter.getUnit()));
            }
            if (filter.getSize() != null) {
                predicates.add(builder.equal(root.get("size"), filter.getSize()));
            }
            if (filter.getMinStorageLife() != null) {
                predicates.add(builder.greaterThanOrEqualTo(root.get("storageLife"), filter.getMinStorageLife()));
            }
            if (filter.getMaxStorageLife() != null) {
                predicates.add(builder.lessThanOrEqualTo(root.get("storageLife"), filter.getMaxStorageLife()));
            }
            if (Boolean.TRUE.equals(filter.getLarge())) {
                predicates.add(builder.equal(root.get("size"), SizeType.large));
            }

            if (!predicates.isEmpty()) {
                criteriaQuery.where(predicates.toArray(new Predicate[0]));
            }

            return session.createQuery(criteriaQuery).getResultList();
        }
    }

    @Override
    public List<Supplies> getSuppliesForProduct(Products product) {
        // Используем обратную связь напрямую, но чтобы избежать LazyInitializationException, лучше запрос
        try (Session session = sessionFactory.openSession()) {
            Query<Supplies> query = session.createQuery(
                    "FROM Supplies WHERE product = :prod", Supplies.class);
            query.setParameter("prod", product);
            return query.getResultList();
        }
    }

    @Override
    public List<Orders> getOrdersForProduct(Products product) {
        try (Session session = sessionFactory.openSession()) {
            Query<Orders> query = session.createQuery(
                    "FROM Orders WHERE product = :prod", Orders.class);
            query.setParameter("prod", product);
            return query.getResultList();
        }
    }

    @Override
    public List<ProductUnits> getUnitsForProduct(Products product) {
        try (Session session = sessionFactory.openSession()) {
            Query<ProductUnits> query = session.createQuery(
                    "FROM ProductUnits WHERE product = :prod", ProductUnits.class);
            query.setParameter("prod", product);
            return query.getResultList();
        }
    }

    @Override
    public ProductCategories getCategory(Products product) {
        // просто возвращаем категорию, она уже загружена, но можно и через запрос
        return product.getCategory();
    }

    private String likeExpr(String param) {
        return "%" + param + "%";
    }
}