package ru.msu.cmc.prak.DAO.impl;

import org.hibernate.Session;
import org.hibernate.query.Query;
import org.springframework.stereotype.Repository;
import ru.msu.cmc.prak.DAO.SuppliesDAO;
import ru.msu.cmc.prak.models.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Repository
public class SuppliesDAOImpl extends CommonDAOImpl<Supplies, Long> implements SuppliesDAO {

    public SuppliesDAOImpl() {
        super(Supplies.class);
    }

    @Override
    public Supplies getById(Long id) {
        try (Session session = sessionFactory.openSession()) {
            Query<Supplies> query = session.createQuery(
                    "select s from Supplies s " +
                            "left join fetch s.product " +
                            "left join fetch s.provider " +
                            "where s.id = :id",
                    Supplies.class
            );
            query.setParameter("id", id);
            return query.uniqueResult();
        }
    }

    @Override
    public List<Supplies> getByFilter(Filter filter) {
        try (Session session = sessionFactory.openSession()) {
            StringBuilder hql = new StringBuilder(
                    "select distinct s from Supplies s " +
                            "left join fetch s.product " +
                            "left join fetch s.provider " +
                            "where 1=1"
            );
            List<ParameterBinder> binders = new ArrayList<>();

            if (filter.getId() != null) {
                hql.append(" and s.id = :id");
                binders.add(q -> q.setParameter("id", filter.getId()));
            }
            if (filter.getProductId() != null) {
                hql.append(" and s.product.id = :productId");
                binders.add(q -> q.setParameter("productId", filter.getProductId()));
            }
            if (filter.getProviderId() != null) {
                hql.append(" and s.provider.id = :providerId");
                binders.add(q -> q.setParameter("providerId", filter.getProviderId()));
            }
            if (filter.getProductName() != null) {
                hql.append(" and lower(s.product.name) like :productName");
                binders.add(q -> q.setParameter("productName", likeExpr(filter.getProductName())));
            }
            if (filter.getProviderName() != null) {
                hql.append(" and lower(s.provider.name) like :providerName");
                binders.add(q -> q.setParameter("providerName", likeExpr(filter.getProviderName())));
            }
            if (filter.getAmountFrom() != null) {
                hql.append(" and s.amount >= :amountFrom");
                binders.add(q -> q.setParameter("amountFrom", bd(filter.getAmountFrom())));
            }
            if (filter.getAmountTo() != null) {
                hql.append(" and s.amount <= :amountTo");
                binders.add(q -> q.setParameter("amountTo", bd(filter.getAmountTo())));
            }
            if (filter.getTimeFrom() != null) {
                hql.append(" and s.time >= :timeFrom");
                binders.add(q -> q.setParameter("timeFrom", filter.getTimeFrom()));
            }
            if (filter.getTimeTo() != null) {
                hql.append(" and s.time <= :timeTo");
                binders.add(q -> q.setParameter("timeTo", filter.getTimeTo()));
            }
            if (filter.getCompleted() != null) {
                hql.append(" and s.completed = :completed");
                binders.add(q -> q.setParameter("completed", filter.getCompleted()));
            }

            hql.append(" order by s.time, s.id");

            Query<Supplies> query = session.createQuery(hql.toString(), Supplies.class);
            for (ParameterBinder binder : binders) {
                binder.bind(query);
            }
            return query.getResultList();
        }
    }

    @Override
    public Providers getProvider(Supplies supply) {
        return supply == null ? null : supply.getProvider();
    }

    @Override
    public Products getProduct(Supplies supply) {
        return supply == null ? null : supply.getProduct();
    }

    @Override
    public List<ProductUnits> getProductUnitsForSupply(Supplies supply) {
        try (Session session = sessionFactory.openSession()) {
            Query<ProductUnits> query = session.createQuery(
                    "select distinct u from ProductUnits u " +
                            "left join fetch u.product " +
                            "left join fetch u.shelf " +
                            "left join fetch u.supply s " +
                            "left join fetch s.provider " +
                            "left join fetch u.order " +
                            "where u.supply = :supply " +
                            "order by u.arrival, u.id",
                    ProductUnits.class
            );
            query.setParameter("supply", supply);
            return query.getResultList();
        }
    }

    private String likeExpr(String value) {
        return "%" + value.toLowerCase() + "%";
    }

    private BigDecimal bd(Integer value) {
        return BigDecimal.valueOf(value.longValue());
    }

    @FunctionalInterface
    private interface ParameterBinder {
        void bind(Query<Supplies> query);
    }
}