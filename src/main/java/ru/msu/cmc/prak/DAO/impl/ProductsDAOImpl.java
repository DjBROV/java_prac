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
    public List<Products> getAllByName(String name) {
        try (Session session = sessionFactory.openSession()) {
            Query<Products> query = session.createQuery(
                    "from Products p where lower(p.name) like :name order by p.id",
                    Products.class
            );
            query.setParameter("name", likeExpr(name));
            return query.getResultList();
        }
    }

    @Override
    public Products getSingleByName(String name) {
        try (Session session = sessionFactory.openSession()) {
            Query<Products> query = session.createQuery(
                    "from Products p where lower(p.name) = lower(:name)",
                    Products.class
            );
            query.setParameter("name", name);
            query.setMaxResults(1);
            return query.uniqueResultOptional().orElse(null);
        }
    }

    @Override
    public List<Products> getByCategoryId(Long categoryId) {
        try (Session session = sessionFactory.openSession()) {
            Query<Products> query = session.createQuery(
                    "from Products p where p.category.id = :categoryId order by p.id",
                    Products.class
            );
            query.setParameter("categoryId", categoryId);
            return query.getResultList();
        }
    }

    @Override
    public List<Products> getByUnit(UnitsType unit) {
        try (Session session = sessionFactory.openSession()) {
            Query<Products> query = session.createQuery(
                    "from Products p where p.unit = :unit order by p.id",
                    Products.class
            );
            query.setParameter("unit", unit);
            return query.getResultList();
        }
    }

    @Override
    public List<Products> getBySize(SizeType size) {
        try (Session session = sessionFactory.openSession()) {
            Query<Products> query = session.createQuery(
                    "from Products p where p.product_size = :size order by p.id",
                    Products.class
            );
            query.setParameter("size", size);
            return query.getResultList();
        }
    }

    @Override
    public List<Products> getByFilter(Filter filter) {
        try (Session session = sessionFactory.openSession()) {
            StringBuilder hql = new StringBuilder("from Products p where 1=1");
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
                    "from Supplies s where s.product = :product order by s.time, s.id",
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
                    "from Orders o where o.product = :product order by o.time, o.id",
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
                    "from ProductUnits u where u.product = :product order by u.arrival, u.id",
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