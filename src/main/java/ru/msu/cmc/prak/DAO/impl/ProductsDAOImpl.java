package ru.msu.cmc.prak.DAO.impl;

import org.hibernate.Session;
import org.hibernate.query.Query;
import org.springframework.stereotype.Repository;
import ru.msu.cmc.prak.DAO.ProductsDAO;
import ru.msu.cmc.prak.models.*;

import java.util.ArrayList;
import java.util.List;

@Repository
public class ProductsDAOImpl extends CommonDAOImpl<Products, Long> implements ProductsDAO {

    public ProductsDAOImpl() {
        super(Products.class);
    }

    @Override
    public Products getById(Long id) {
        try (Session session = sessionFactory.openSession()) {
            Query<Products> query = session.createQuery(
                    "select p from Products p " +
                            "left join fetch p.category " +
                            "where p.id = :id",
                    Products.class
            );
            query.setParameter("id", id);
            return query.uniqueResult();
        }
    }

    @Override
    public List<Products> getByFilter(Filter filter) {
        try (Session session = sessionFactory.openSession()) {
            StringBuilder hql = new StringBuilder(
                    "select distinct p from Products p " +
                            "left join fetch p.category " +
                            "where 1=1"
            );
            List<ParameterBinder> binders = new ArrayList<>();

            if (filter.getId() != null) {
                hql.append(" and p.id = :id");
                binders.add(q -> q.setParameter("id", filter.getId()));
            }
            if (filter.getName() != null) {
                hql.append(" and lower(p.name) like :name");
                binders.add(q -> q.setParameter("name", likeExpr(filter.getName())));
            }
            if (filter.getCategoryId() != null) {
                hql.append(" and p.category.id = :categoryId");
                binders.add(q -> q.setParameter("categoryId", filter.getCategoryId()));
            }
            if (filter.getCategoryName() != null) {
                hql.append(" and lower(p.category.name) like :categoryName");
                binders.add(q -> q.setParameter("categoryName", likeExpr(filter.getCategoryName())));
            }
            if (filter.getUnit() != null) {
                hql.append(" and p.unit = :unit");
                binders.add(q -> q.setParameter("unit", filter.getUnit()));
            }
            if (filter.getSize() != null) {
                hql.append(" and p.product_size = :size");
                binders.add(q -> q.setParameter("size", filter.getSize()));
            }
            if (filter.getMinStorageLife() != null) {
                hql.append(" and p.storageLife >= :minStorageLife");
                binders.add(q -> q.setParameter("minStorageLife", filter.getMinStorageLife()));
            }
            if (filter.getMaxStorageLife() != null) {
                hql.append(" and p.storageLife <= :maxStorageLife");
                binders.add(q -> q.setParameter("maxStorageLife", filter.getMaxStorageLife()));
            }
            if (Boolean.TRUE.equals(filter.getLarge())) {
                hql.append(" and p.product_size = :largeSize");
                binders.add(q -> q.setParameter("largeSize", SizeType.large));
            }

            hql.append(" order by p.id");

            Query<Products> query = session.createQuery(hql.toString(), Products.class);
            for (ParameterBinder binder : binders) {
                binder.bind(query);
            }
            return query.getResultList();
        }
    }

    @Override
    public List<Supplies> getSuppliesForProduct(Products product) {
        try (Session session = sessionFactory.openSession()) {
            Query<Supplies> query = session.createQuery(
                    "select s from Supplies s " +
                            "left join fetch s.provider " +
                            "left join fetch s.product " +
                            "where s.product = :product " +
                            "order by s.time, s.id",
                    Supplies.class
            );
            query.setParameter("product", product);
            return query.getResultList();
        }
    }

    @Override
    public List<Orders> getOrdersForProduct(Products product) {
        try (Session session = sessionFactory.openSession()) {
            Query<Orders> query = session.createQuery(
                    "select o from Orders o " +
                            "left join fetch o.consumer " +
                            "left join fetch o.product " +
                            "where o.product = :product " +
                            "order by o.time, o.id",
                    Orders.class
            );
            query.setParameter("product", product);
            return query.getResultList();
        }
    }

    @Override
    public List<ProductUnits> getUnitsForProduct(Products product) {
        try (Session session = sessionFactory.openSession()) {
            Query<ProductUnits> query = session.createQuery(
                    "select distinct u from ProductUnits u " +
                            "left join fetch u.product " +
                            "left join fetch u.shelf " +
                            "left join fetch u.supply s " +
                            "left join fetch s.provider " +
                            "left join fetch u.order " +
                            "where u.product = :product " +
                            "order by u.arrival, u.id",
                    ProductUnits.class
            );
            query.setParameter("product", product);
            return query.getResultList();
        }
    }

    @Override
    public ProductCategories getCategory(Products product) {
        return product == null ? null : product.getCategory();
    }

    private String likeExpr(String value) {
        return "%" + value.toLowerCase() + "%";
    }

    @FunctionalInterface
    private interface ParameterBinder {
        void bind(Query<Products> query);
    }
}