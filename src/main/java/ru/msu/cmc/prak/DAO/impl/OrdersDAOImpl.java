package ru.msu.cmc.prak.DAO.impl;

import org.hibernate.Session;
import org.hibernate.query.Query;
import org.springframework.stereotype.Repository;
import ru.msu.cmc.prak.DAO.OrdersDAO;
import ru.msu.cmc.prak.models.Consumers;
import ru.msu.cmc.prak.models.Orders;
import ru.msu.cmc.prak.models.ProductUnits;
import ru.msu.cmc.prak.models.Products;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Repository
public class OrdersDAOImpl extends CommonDAOImpl<Orders, Long> implements OrdersDAO {

    public OrdersDAOImpl() {
        super(Orders.class);
    }

    @Override
    public List<Orders> getByFilter(Filter filter) {
        try (Session session = sessionFactory.openSession()) {
            StringBuilder hql = new StringBuilder("from Orders o where 1=1");
            List<ParameterBinder> binders = new ArrayList<>();

            if (filter.getId() != null) {
                hql.append(" and o.id = :id");
                binders.add(q -> q.setParameter("id", filter.getId()));
            }
            if (filter.getProductId() != null) {
                hql.append(" and o.product.id = :productId");
                binders.add(q -> q.setParameter("productId", filter.getProductId()));
            }
            if (filter.getConsumerId() != null) {
                hql.append(" and o.consumer.id = :consumerId");
                binders.add(q -> q.setParameter("consumerId", filter.getConsumerId()));
            }
            if (filter.getProductName() != null) {
                hql.append(" and lower(o.product.name) like :productName");
                binders.add(q -> q.setParameter("productName", likeExpr(filter.getProductName())));
            }
            if (filter.getConsumerName() != null) {
                hql.append(" and lower(o.consumer.name) like :consumerName");
                binders.add(q -> q.setParameter("consumerName", likeExpr(filter.getConsumerName())));
            }
            if (filter.getAmountFrom() != null) {
                hql.append(" and o.amount >= :amountFrom");
                binders.add(q -> q.setParameter("amountFrom", bd(filter.getAmountFrom())));
            }
            if (filter.getAmountTo() != null) {
                hql.append(" and o.amount <= :amountTo");
                binders.add(q -> q.setParameter("amountTo", bd(filter.getAmountTo())));
            }
            if (filter.getTimeFrom() != null) {
                hql.append(" and o.time >= :timeFrom");
                binders.add(q -> q.setParameter("timeFrom", filter.getTimeFrom()));
            }
            if (filter.getTimeTo() != null) {
                hql.append(" and o.time <= :timeTo");
                binders.add(q -> q.setParameter("timeTo", filter.getTimeTo()));
            }
            if (filter.getCompleted() != null) {
                hql.append(" and o.completed = :completed");
                binders.add(q -> q.setParameter("completed", filter.getCompleted()));
            }

            hql.append(" order by o.time, o.id");

            Query<Orders> query = session.createQuery(hql.toString(), Orders.class);
            for (ParameterBinder binder : binders) {
                binder.bind(query);
            }
            return query.getResultList();
        }
    }

    @Override
    public Consumers getConsumer(Orders order) {
        return order == null ? null : order.getConsumer();
    }

    @Override
    public Products getProduct(Orders order) {
        return order == null ? null : order.getProduct();
    }

    @Override
    public List<ProductUnits> getProductUnitsForOrder(Orders order) {
        try (Session session = sessionFactory.openSession()) {
            Query<ProductUnits> query = session.createQuery(
                    "from ProductUnits u where u.order = :order order by u.arrival, u.id",
                    ProductUnits.class
            );
            query.setParameter("order", order);
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
        void bind(Query<Orders> query);
    }
}