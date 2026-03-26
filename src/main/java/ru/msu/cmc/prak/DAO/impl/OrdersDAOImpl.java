package ru.msu.cmc.prak.DAO.impl;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.springframework.stereotype.Repository;
import ru.msu.cmc.prak.DAO.OrdersDAO;
import ru.msu.cmc.prak.models.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Repository
public class OrdersDAOImpl extends CommonDAOImpl<Orders, Long> implements OrdersDAO {

    public OrdersDAOImpl() {
        super(Orders.class);
    }

    @Override
    public List<Orders> getByProductId(Long productId) {
        try (Session session = sessionFactory.openSession()) {
            Query<Orders> query = session.createQuery(
                    "FROM Orders WHERE product.id = :prodId", Orders.class);
            query.setParameter("prodId", productId);
            return query.getResultList();
        }
    }

    @Override
    public List<Orders> getByConsumerId(Long consumerId) {
        try (Session session = sessionFactory.openSession()) {
            Query<Orders> query = session.createQuery(
                    "FROM Orders WHERE consumer.id = :consId", Orders.class);
            query.setParameter("consId", consumerId);
            return query.getResultList();
        }
    }

    @Override
    public List<Orders> getByCompleted(Boolean completed) {
        try (Session session = sessionFactory.openSession()) {
            Query<Orders> query = session.createQuery(
                    "FROM Orders WHERE completed = :comp", Orders.class);
            query.setParameter("comp", completed);
            return query.getResultList();
        }
    }

    @Override
    public List<Orders> getByTimeRange(LocalDateTime from, LocalDateTime to) {
        try (Session session = sessionFactory.openSession()) {
            Query<Orders> query = session.createQuery(
                    "FROM Orders WHERE time BETWEEN :from AND :to", Orders.class);
            query.setParameter("from", from);
            query.setParameter("to", to);
            return query.getResultList();
        }
    }

    @Override
    public List<Orders> getByFilter(Filter filter) {
        try (Session session = sessionFactory.openSession()) {
            CriteriaBuilder builder = session.getCriteriaBuilder();
            CriteriaQuery<Orders> criteriaQuery = builder.createQuery(Orders.class);
            Root<Orders> root = criteriaQuery.from(Orders.class);

            List<Predicate> predicates = new ArrayList<>();

            if (filter.getId() != null) {
                predicates.add(builder.equal(root.get("id"), filter.getId()));
            }
            if (filter.getProductId() != null) {
                predicates.add(builder.equal(root.get("product").get("id"), filter.getProductId()));
            }
            if (filter.getConsumerId() != null) {
                predicates.add(builder.equal(root.get("consumer").get("id"), filter.getConsumerId()));
            }
            if (filter.getProductName() != null) {
                predicates.add(builder.like(root.get("product").get("name"), likeExpr(filter.getProductName())));
            }
            if (filter.getConsumerName() != null) {
                predicates.add(builder.like(root.get("consumer").get("name"), likeExpr(filter.getConsumerName())));
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
    public Consumers getConsumer(Orders order) {
        return order.getConsumer();
    }

    @Override
    public Products getProduct(Orders order) {
        return order.getProduct();
    }

    @Override
    public List<ProductUnits> getProductUnitsForOrder(Orders order) {
        try (Session session = sessionFactory.openSession()) {
            Query<ProductUnits> query = session.createQuery(
                    "FROM ProductUnits WHERE order = :order", ProductUnits.class);
            query.setParameter("order", order);
            return query.getResultList();
        }
    }

    private String likeExpr(String param) {
        return "%" + param + "%";
    }
}