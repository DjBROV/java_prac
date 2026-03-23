package ru.msu.cmc.prak.DAO.impl;

import org.hibernate.Session;
import org.hibernate.query.Query;
import org.springframework.stereotype.Repository;
import ru.msu.cmc.prak.DAO.ProductUnitsDAO;
import ru.msu.cmc.prak.models.*;

import javax.persistence.criteria.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Repository
public class ProductUnitsDAOImpl extends CommonDAOImpl<ProductUnits, Long> implements ProductUnitsDAO {

    public ProductUnitsDAOImpl() {
        super(ProductUnits.class);
    }

    @Override
    public List<ProductUnits> getByProductId(Long productId) {
        try (Session session = sessionFactory.openSession()) {
            Query<ProductUnits> query = session.createQuery(
                    "FROM ProductUnits WHERE product.id = :prodId", ProductUnits.class);
            query.setParameter("prodId", productId);
            return query.getResultList();
        }
    }

    @Override
    public List<ProductUnits> getByShelfNum(Long shelfNum) {
        try (Session session = sessionFactory.openSession()) {
            Query<ProductUnits> query = session.createQuery(
                    "FROM ProductUnits WHERE shelf.id = :shelfId", ProductUnits.class);
            query.setParameter("shelfId", shelfNum);
            return query.getResultList();
        }
    }

    @Override
    public List<ProductUnits> getBySupplyId(Long supplyId) {
        try (Session session = sessionFactory.openSession()) {
            Query<ProductUnits> query = session.createQuery(
                    "FROM ProductUnits WHERE supply.id = :supplyId", ProductUnits.class);
            query.setParameter("supplyId", supplyId);
            return query.getResultList();
        }
    }

    @Override
    public List<ProductUnits> getByOrderId(Long orderId) {
        try (Session session = sessionFactory.openSession()) {
            Query<ProductUnits> query = session.createQuery(
                    "FROM ProductUnits WHERE order.id = :orderId", ProductUnits.class);
            query.setParameter("orderId", orderId);
            return query.getResultList();
        }
    }

    @Override
    public List<ProductUnits> getByArrivalRange(LocalDateTime from, LocalDateTime to) {
        try (Session session = sessionFactory.openSession()) {
            Query<ProductUnits> query = session.createQuery(
                    "FROM ProductUnits WHERE arrival BETWEEN :from AND :to", ProductUnits.class);
            query.setParameter("from", from);
            query.setParameter("to", to);
            return query.getResultList();
        }
    }

    @Override
    public List<ProductUnits> getByFilter(Filter filter) {
        try (Session session = sessionFactory.openSession()) {
            CriteriaBuilder builder = session.getCriteriaBuilder();
            CriteriaQuery<ProductUnits> criteriaQuery = builder.createQuery(ProductUnits.class);
            Root<ProductUnits> root = criteriaQuery.from(ProductUnits.class);

            List<Predicate> predicates = new ArrayList<>();

            if (filter.getProductId() != null) {
                predicates.add(builder.equal(root.get("product").get("id"), filter.getProductId()));
            }
            if (filter.getSupplyId() != null) {
                predicates.add(builder.equal(root.get("supply").get("id"), filter.getSupplyId()));
            }
            if (filter.getSupplierId() != null) {
                // фильтр по поставщику: нужно добраться до supply.provider.id
                predicates.add(builder.equal(root.get("supply").get("provider").get("id"), filter.getSupplierId()));
            }
            if (filter.getShelfNum() != null) {
                predicates.add(builder.equal(root.get("shelf").get("id"), filter.getShelfNum()));
            }
            if (filter.getRoomNum() != null) {
                predicates.add(builder.equal(root.get("shelf").get("roomNum"), filter.getRoomNum()));
            }
            if (filter.getMinAmount() != null) {
                predicates.add(builder.greaterThanOrEqualTo(root.get("amount"), filter.getMinAmount()));
            }
            if (filter.getMaxAmount() != null) {
                predicates.add(builder.lessThanOrEqualTo(root.get("amount"), filter.getMaxAmount()));
            }
            if (filter.getArrivalFrom() != null) {
                predicates.add(builder.greaterThanOrEqualTo(root.get("arrival"), filter.getArrivalFrom()));
            }
            if (filter.getArrivalTo() != null) {
                predicates.add(builder.lessThanOrEqualTo(root.get("arrival"), filter.getArrivalTo()));
            }
            if (filter.getReserved() != null) {
                if (filter.getReserved()) {
                    predicates.add(builder.isNotNull(root.get("order")));
                } else {
                    predicates.add(builder.isNull(root.get("order")));
                }
            }

            if (!predicates.isEmpty()) {
                criteriaQuery.where(predicates.toArray(new Predicate[0]));
            }

            return session.createQuery(criteriaQuery).getResultList();
        }
    }

    @Override
    public Products getProduct(ProductUnits unit) {
        return unit.getProduct();
    }

    @Override
    public ShelfsWorkload getShelf(ProductUnits unit) {
        return unit.getShelf();
    }

    @Override
    public Supplies getSupply(ProductUnits unit) {
        return unit.getSupply();
    }

    @Override
    public Orders getOrder(ProductUnits unit) {
        return unit.getOrder();
    }

    @Override
    public List<ProductUnits> getFreeUnits() {
        try (Session session = sessionFactory.openSession()) {
            Query<ProductUnits> query = session.createQuery(
                    "FROM ProductUnits WHERE order IS NULL", ProductUnits.class);
            return query.getResultList();
        }
    }

    @Override
    public List<ProductUnits> getReservedUnits() {
        try (Session session = sessionFactory.openSession()) {
            Query<ProductUnits> query = session.createQuery(
                    "FROM ProductUnits WHERE order IS NOT NULL", ProductUnits.class);
            return query.getResultList();
        }
    }
}