package ru.msu.cmc.prak.DAO.impl;

import org.hibernate.Session;
import org.hibernate.query.Query;
import org.springframework.stereotype.Repository;
import ru.msu.cmc.prak.DAO.ConsumersDAO;
import ru.msu.cmc.prak.models.Consumers;
import ru.msu.cmc.prak.models.Orders;

import java.util.ArrayList;
import java.util.List;

@Repository
public class ConsumersDAOImpl extends CommonDAOImpl<Consumers, Long> implements ConsumersDAO {

    public ConsumersDAOImpl() {
        super(Consumers.class);
    }

    @Override
    public List<Consumers> getByFilter(Filter filter) {
        try (Session session = sessionFactory.openSession()) {
            StringBuilder hql = new StringBuilder("from Consumers c where 1=1");
            List<ParameterBinder> binders = new ArrayList<>();

            if (filter.getId() != null) {
                hql.append(" and c.id = :id");
                binders.add(q -> q.setParameter("id", filter.getId()));
            }
            if (filter.getName() != null) {
                hql.append(" and lower(c.name) like :name");
                binders.add(q -> q.setParameter("name", likeExpr(filter.getName())));
            }
            if (filter.getAddress() != null) {
                hql.append(" and lower(c.address) like :address");
                binders.add(q -> q.setParameter("address", likeExpr(filter.getAddress())));
            }
            if (filter.getPhoneNum() != null) {
                hql.append(" and lower(c.phoneNum) like :phoneNum");
                binders.add(q -> q.setParameter("phoneNum", likeExpr(filter.getPhoneNum())));
            }
            if (filter.getEmail() != null) {
                hql.append(" and lower(c.email) like :email");
                binders.add(q -> q.setParameter("email", likeExpr(filter.getEmail())));
            }

            hql.append(" order by c.id");

            Query<Consumers> query = session.createQuery(hql.toString(), Consumers.class);
            for (ParameterBinder binder : binders) {
                binder.bind(query);
            }
            return query.getResultList();
        }
    }

    @Override
    public List<Orders> getOrdersByConsumer(Consumers consumer) {
        try (Session session = sessionFactory.openSession()) {
            Query<Orders> query = session.createQuery(
                    "from Orders o where o.consumer = :consumer order by o.time, o.id",
                    Orders.class
            );
            query.setParameter("consumer", consumer);
            return query.getResultList();
        }
    }

    private String likeExpr(String value) {
        return "%" + value.toLowerCase() + "%";
    }

    @FunctionalInterface
    private interface ParameterBinder {
        void bind(Query<Consumers> query);
    }
}