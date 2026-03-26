package ru.msu.cmc.prak.DAO.impl;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.springframework.stereotype.Repository;
import ru.msu.cmc.prak.DAO.SuppliesDAO;
import ru.msu.cmc.prak.models.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Repository
public class SuppliesDAOImpl extends CommonDAOImpl<Supplies, Long> implements SuppliesDAO {

    public SuppliesDAOImpl() {
        super(Supplies.class);
    }

    @Override
    public List<Supplies> getByProductId(Long productId) {
        try (Session session = sessionFactory.openSession()) {
            Query<Supplies> query = session.createQuery(
                    "FROM Supplies WHERE product.id = :prodId", Supplies.class);
            query.setParameter("prodId", productId);
            return query.getResultList();
        }
    }

    @Override
    public List<Supplies> getByProviderId(Long providerId) {
        try (Session session = sessionFactory.openSession()) {
            Query<Supplies> query = session.createQuery(
                    "FROM Supplies WHERE provider.id = :provId", Supplies.class);
            query.setParameter("provId", providerId);
            return query.getResultList();
        }
    }

    @Override
    public List<Supplies> getByCompleted(Boolean completed) {
        try (Session session = sessionFactory.openSession()) {
            Query<Supplies> query = session.createQuery(
                    "FROM Supplies WHERE completed = :comp", Supplies.class);
            query.setParameter("comp", completed);
            return query.getResultList();
        }
    }

    @Override
    public List<Supplies> getByTimeRange(LocalDateTime from, LocalDateTime to) {
        try (Session session = sessionFactory.openSession()) {
            Query<Supplies> query = session.createQuery(
                    "FROM Supplies WHERE time BETWEEN :from AND :to", Supplies.class);
            query.setParameter("from", from);
            query.setParameter("to", to);
            return query.getResultList();
        }
    }

    @Override
    public List<Supplies> getByFilter(Filter filter) {
        try (Session session = sessionFactory.openSession()) {
            CriteriaBuilder builder = session.getCriteriaBuilder();
            CriteriaQuery<Supplies> criteriaQuery = builder.createQuery(Supplies.class);
            Root<Supplies> root = criteriaQuery.from(Supplies.class);

            List<Predicate> predicates = new ArrayList<>();

            if (filter.getId() != null) {
                predicates.add(builder.equal(root.get("id"), filter.getId()));
            }
            if (filter.getProductId() != null) {
                predicates.add(builder.equal(root.get("product").get("id"), filter.getProductId()));
            }
            if (filter.getProviderId() != null) {
                predicates.add(builder.equal(root.get("provider").get("id"), filter.getProviderId()));
            }
            if (filter.getProductName() != null) {
                predicates.add(builder.like(root.get("product").get("name"), likeExpr(filter.getProductName())));
            }
            if (filter.getProviderName() != null) {
                predicates.add(builder.like(root.get("provider").get("name"), likeExpr(filter.getProviderName())));
            }
            if (filter.getAmountFrom() != null) {
                predicates.add(builder.greaterThanOrEqualTo(root.get("amount"), BigDecimal.valueOf(filter.getAmountFrom())));
            }
            if (filter.getAmountTo() != null) {
                predicates.add(builder.lessThanOrEqualTo(root.get("amount"), BigDecimal.valueOf(filter.getAmountTo())));
            }
            if (filter.getTimeFrom() != null) {
                predicates.add(builder.greaterThanOrEqualTo(root.get("time"), filter.getTimeFrom()));
            }
            if (filter.getTimeTo() != null) {
                predicates.add(builder.lessThanOrEqualTo(root.get("time"), filter.getTimeTo()));
            }
            if (filter.getCompleted() != null) {
                predicates.add(builder.equal(root.get("completed"), filter.getCompleted()));
            }

            if (!predicates.isEmpty()) {
                criteriaQuery.where(predicates.toArray(new Predicate[0]));
            }

            return session.createQuery(criteriaQuery).getResultList();
        }
    }

    @Override
    public Providers getProvider(Supplies supply) {
        return supply.getProvider();
    }

    @Override
    public Products getProduct(Supplies supply) {
        return supply.getProduct();
    }

    @Override
    public List<ProductUnits> getProductUnitsForSupply(Supplies supply) {
        try (Session session = sessionFactory.openSession()) {
            Query<ProductUnits> query = session.createQuery(
                    "FROM ProductUnits WHERE supply = :supply", ProductUnits.class);
            query.setParameter("supply", supply);
            return query.getResultList();
        }
    }

    private String likeExpr(String param) {
        return "%" + param + "%";
    }
}