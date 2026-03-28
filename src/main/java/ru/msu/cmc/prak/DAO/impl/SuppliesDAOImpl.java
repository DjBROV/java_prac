package ru.msu.cmc.prak.DAO.impl;

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
                    "from Supplies s where s.product.id = :productId order by s.time, s.id",
                    Supplies.class
            );
            query.setParameter("productId", productId);
            return query.getResultList();
        }
    }

    @Override
    public List<Supplies> getByProviderId(Long providerId) {
        try (Session session = sessionFactory.openSession()) {
            Query<Supplies> query = session.createQuery(
                    "from Supplies s where s.provider.id = :providerId order by s.time, s.id",
                    Supplies.class
            );
            query.setParameter("providerId", providerId);
            return query.getResultList();
        }
    }

    @Override
    public List<Supplies> getByCompleted(Boolean completed) {
        try (Session session = sessionFactory.openSession()) {
            Query<Supplies> query = session.createQuery(
                    "from Supplies s where s.completed = :completed order by s.time, s.id",
                    Supplies.class
            );
            query.setParameter("completed", completed);
            return query.getResultList();
        }
    }

    @Override
    public List<Supplies> getByTimeRange(LocalDateTime from, LocalDateTime to) {
        try (Session session = sessionFactory.openSession()) {
            Query<Supplies> query = session.createQuery(
                    "from Supplies s where s.time between :from and :to order by s.time, s.id",
                    Supplies.class
            );
            query.setParameter("from", from);
            query.setParameter("to", to);
            return query.getResultList();
        }
    }

    @Override
    public List<Supplies> getByFilter(Filter filter) {
        try (Session session = sessionFactory.openSession()) {
            StringBuilder hql = new StringBuilder("from Supplies s where 1=1");
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
                    "from ProductUnits u where u.supply = :supply order by u.arrival, u.id",
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
        return value == null ? null : BigDecimal.valueOf(value.longValue());
    }

    @FunctionalInterface
    private interface ParameterBinder {
        void bind(Query<Supplies> query);
    }
}