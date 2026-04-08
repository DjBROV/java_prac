package ru.msu.cmc.prak.DAO.impl;

import org.hibernate.Session;
import org.hibernate.query.Query;
import org.springframework.stereotype.Repository;
import ru.msu.cmc.prak.DAO.ProductUnitsDAO;
import ru.msu.cmc.prak.models.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Repository
public class ProductUnitsDAOImpl extends CommonDAOImpl<ProductUnits, Long> implements ProductUnitsDAO {

    public ProductUnitsDAOImpl() {
        super(ProductUnits.class);
    }

    @Override
    public ProductUnits getById(Long id) {
        try (Session session = sessionFactory.openSession()) {
            Query<ProductUnits> query = session.createQuery(
                    "select u from ProductUnits u " +
                            "left join fetch u.product " +
                            "left join fetch u.shelf " +
                            "left join fetch u.supply s " +
                            "left join fetch s.provider " +
                            "left join fetch u.order " +
                            "where u.id = :id",
                    ProductUnits.class
            );
            query.setParameter("id", id);
            return query.uniqueResult();
        }
    }

    @Override
    public List<ProductUnits> getByFilter(Filter filter) {
        try (Session session = sessionFactory.openSession()) {
            StringBuilder hql = new StringBuilder(
                    "select distinct u from ProductUnits u " +
                            "left join fetch u.product " +
                            "left join fetch u.shelf " +
                            "left join fetch u.supply s " +
                            "left join fetch s.provider " +
                            "left join fetch u.order " +
                            "where 1=1"
            );
            List<ParameterBinder> binders = new ArrayList<>();

            if (filter.getProductId() != null) {
                hql.append(" and u.product.id = :productId");
                binders.add(q -> q.setParameter("productId", filter.getProductId()));
            }
            if (filter.getSupplyId() != null) {
                hql.append(" and u.supply.id = :supplyId");
                binders.add(q -> q.setParameter("supplyId", filter.getSupplyId()));
            }
            if (filter.getSupplierId() != null) {
                hql.append(" and u.supply.provider.id = :supplierId");
                binders.add(q -> q.setParameter("supplierId", filter.getSupplierId()));
            }
            if (filter.getShelfNum() != null) {
                hql.append(" and u.shelf.id = :shelfNum");
                binders.add(q -> q.setParameter("shelfNum", filter.getShelfNum()));
            }
            if (filter.getRoomNum() != null) {
                hql.append(" and u.shelf.roomNum = :roomNum");
                binders.add(q -> q.setParameter("roomNum", filter.getRoomNum()));
            }
            if (filter.getMinAmount() != null) {
                hql.append(" and u.amount >= :minAmount");
                binders.add(q -> q.setParameter("minAmount", bd(filter.getMinAmount())));
            }
            if (filter.getMaxAmount() != null) {
                hql.append(" and u.amount <= :maxAmount");
                binders.add(q -> q.setParameter("maxAmount", bd(filter.getMaxAmount())));
            }
            if (filter.getArrivalFrom() != null) {
                hql.append(" and u.arrival >= :arrivalFrom");
                binders.add(q -> q.setParameter("arrivalFrom", filter.getArrivalFrom()));
            }
            if (filter.getArrivalTo() != null) {
                hql.append(" and u.arrival <= :arrivalTo");
                binders.add(q -> q.setParameter("arrivalTo", filter.getArrivalTo()));
            }
            if (filter.getReserved() != null) {
                if (filter.getReserved()) {
                    hql.append(" and u.order is not null");
                } else {
                    hql.append(" and u.order is null");
                }
            }

            hql.append(" order by u.arrival, u.id");

            Query<ProductUnits> query = session.createQuery(hql.toString(), ProductUnits.class);
            for (ParameterBinder binder : binders) {
                binder.bind(query);
            }
            return query.getResultList();
        }
    }

    @Override
    public Products getProduct(ProductUnits unit) {
        return unit == null ? null : unit.getProduct();
    }

    @Override
    public ShelfsWorkload getShelf(ProductUnits unit) {
        return unit == null ? null : unit.getShelf();
    }

    @Override
    public Supplies getSupply(ProductUnits unit) {
        return unit == null ? null : unit.getSupply();
    }

    @Override
    public Orders getOrder(ProductUnits unit) {
        return unit == null ? null : unit.getOrder();
    }

    @Override
    public List<ProductUnits> getFreeUnits() {
        try (Session session = sessionFactory.openSession()) {
            Query<ProductUnits> query = session.createQuery(
                    "select distinct u from ProductUnits u " +
                            "left join fetch u.product " +
                            "left join fetch u.shelf " +
                            "left join fetch u.supply s " +
                            "left join fetch s.provider " +
                            "where u.order is null " +
                            "order by u.arrival, u.id",
                    ProductUnits.class
            );
            return query.getResultList();
        }
    }

    @Override
    public List<ProductUnits> getReservedUnits() {
        try (Session session = sessionFactory.openSession()) {
            Query<ProductUnits> query = session.createQuery(
                    "select distinct u from ProductUnits u " +
                            "left join fetch u.product " +
                            "left join fetch u.shelf " +
                            "left join fetch u.supply s " +
                            "left join fetch s.provider " +
                            "left join fetch u.order " +
                            "where u.order is not null " +
                            "order by u.arrival, u.id",
                    ProductUnits.class
            );
            return query.getResultList();
        }
    }

    private BigDecimal bd(Integer value) {
        return BigDecimal.valueOf(value.longValue());
    }

    @FunctionalInterface
    private interface ParameterBinder {
        void bind(Query<ProductUnits> query);
    }
}