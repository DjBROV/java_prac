package ru.msu.cmc.prak.DAO.impl;

import org.hibernate.Session;
import org.hibernate.query.Query;
import org.springframework.stereotype.Repository;
import ru.msu.cmc.prak.DAO.ProductCategoriesDAO;
import ru.msu.cmc.prak.models.ProductCategories;
import ru.msu.cmc.prak.models.Products;

import java.util.ArrayList;
import java.util.List;

@Repository
public class ProductCategoriesDAOImpl extends CommonDAOImpl<ProductCategories, Long> implements ProductCategoriesDAO {

    public ProductCategoriesDAOImpl() {
        super(ProductCategories.class);
    }

    @Override
    public List<ProductCategories> getAllByName(String name) {
        try (Session session = sessionFactory.openSession()) {
            Query<ProductCategories> query = session.createQuery(
                    "from ProductCategories c where lower(c.name) like :name order by c.id",
                    ProductCategories.class
            );
            query.setParameter("name", likeExpr(name));
            return query.getResultList();
        }
    }

    @Override
    public ProductCategories getSingleByName(String name) {
        try (Session session = sessionFactory.openSession()) {
            Query<ProductCategories> query = session.createQuery(
                    "from ProductCategories c where lower(c.name) = lower(:name)",
                    ProductCategories.class
            );
            query.setParameter("name", name);
            query.setMaxResults(1);
            return query.uniqueResultOptional().orElse(null);
        }
    }

    @Override
    public List<ProductCategories> getByFilter(Filter filter) {
        try (Session session = sessionFactory.openSession()) {
            StringBuilder hql = new StringBuilder("from ProductCategories c where 1=1");
            List<ParameterBinder> binders = new ArrayList<>();

            if (filter.getName() != null) {
                hql.append(" and lower(c.name) like :name");
                binders.add(q -> q.setParameter("name", likeExpr(filter.getName())));
            }

            hql.append(" order by c.id");

            Query<ProductCategories> query = session.createQuery(hql.toString(), ProductCategories.class);
            for (ParameterBinder binder : binders) {
                binder.bind(query);
            }
            return query.getResultList();
        }
    }

    @Override
    public List<Products> getProductsInCategory(ProductCategories category) {
        try (Session session = sessionFactory.openSession()) {
            Query<Products> query = session.createQuery(
                    "from Products p where p.category = :category order by p.id",
                    Products.class
            );
            query.setParameter("category", category);
            return query.getResultList();
        }
    }

    @Override
    public long countProductsInCategory(ProductCategories category) {
        try (Session session = sessionFactory.openSession()) {
            Query<Long> query = session.createQuery(
                    "select count(p) from Products p where p.category = :category",
                    Long.class
            );
            query.setParameter("category", category);
            return query.getSingleResult();
        }
    }

    private String likeExpr(String value) {
        return "%" + value.toLowerCase() + "%";
    }

    @FunctionalInterface
    private interface ParameterBinder {
        void bind(Query<ProductCategories> query);
    }
}