package ru.msu.cmc.prak.DAO.impl;

import org.hibernate.Session;
import org.hibernate.query.Query;
import org.springframework.stereotype.Repository;
import ru.msu.cmc.prak.DAO.ProductCategoriesDAO;
import ru.msu.cmc.prak.models.ProductCategories;
import ru.msu.cmc.prak.models.Products;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
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
                    "FROM ProductCategories WHERE name LIKE :name", ProductCategories.class);
            query.setParameter("name", likeExpr(name));
            return query.getResultList();
        }
    }

    @Override
    public ProductCategories getSingleByName(String name) {
        List<ProductCategories> candidates = getAllByName(name);
        return candidates.size() == 1 ? candidates.get(0) : null;
    }

    @Override
    public List<ProductCategories> getByFilter(Filter filter) {
        try (Session session = sessionFactory.openSession()) {
            CriteriaBuilder builder = session.getCriteriaBuilder();
            CriteriaQuery<ProductCategories> criteriaQuery = builder.createQuery(ProductCategories.class);
            Root<ProductCategories> root = criteriaQuery.from(ProductCategories.class);

            List<Predicate> predicates = new ArrayList<>();
            if (filter.getName() != null) {
                predicates.add(builder.like(root.get("name"), likeExpr(filter.getName())));
            }

            if (!predicates.isEmpty()) {
                criteriaQuery.where(predicates.toArray(new Predicate[0]));
            }

            return session.createQuery(criteriaQuery).getResultList();
        }
    }

    @Override
    public List<Products> getProductsInCategory(ProductCategories category) {
        // Используем связь @OneToMany, достаточно просто обратиться к коллекции, но для ленивой загрузки лучше запрос
        try (Session session = sessionFactory.openSession()) {
            Query<Products> query = session.createQuery(
                    "FROM Products WHERE category = :cat", Products.class);
            query.setParameter("cat", category);
            return query.getResultList();
        }
    }

    @Override
    public long countProductsInCategory(ProductCategories category) {
        try (Session session = sessionFactory.openSession()) {
            Query<Long> query = session.createQuery(
                    "SELECT COUNT(p) FROM Products p WHERE category = :cat", Long.class);
            query.setParameter("cat", category);
            return query.getSingleResult();
        }
    }

    private String likeExpr(String param) {
        return "%" + param + "%";
    }
}