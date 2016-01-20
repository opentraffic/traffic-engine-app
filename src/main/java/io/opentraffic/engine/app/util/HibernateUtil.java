package io.opentraffic.engine.app.util;

import io.opentraffic.engine.app.data.User;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.hibernate.service.ServiceRegistry;

import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

public class HibernateUtil {

    private static final Logger log = Logger.getLogger(HibernateUtil.class.getName());
    private static SessionFactory sessionFactory = buildSessionFactory();
    private static ServiceRegistry serviceRegistry;

    private static SessionFactory buildSessionFactory() {
        Configuration configuration = new Configuration();
        configuration.configure();
        serviceRegistry = new StandardServiceRegistryBuilder().applySettings(
                configuration.getProperties()).build();
        sessionFactory = configuration.buildSessionFactory(serviceRegistry);

        Session session = sessionFactory.openSession();
        Query q = session.createQuery("from User");
        q.setMaxResults(1);
        List<User> users = q.list();
        if(users.size() < 1){
            User u = new User();
            u.setUsername("admin");
            u.setPasswordHash(PasswordUtil.hash("admin"));
            u.setRole("Super Admin");
            persistUser(u);
        }
        return sessionFactory;
    }

    public static SessionFactory getSessionFactory() {
        return sessionFactory;
    }

    public static void persistUser(User user){
        Session session = sessionFactory.openSession();
        Transaction tx;
        try {
            tx = session.beginTransaction();
            session.saveOrUpdate(user);
            tx.commit();
        }
        catch (Exception e) {
            log.warning(e.getMessage());
            throw new RuntimeException("database error");
        }
        finally {
            session.close();
        }
    }

    public static List<User> getUsers(){
        Session session = sessionFactory.openSession();
        Query q = session.createQuery("from User u order by u.username");
        return q.list();
    }

    public static User getUser(Integer id) {
        Session session = sessionFactory.openSession();
        return (User) session.get(User.class, id);
    }

    public static void deleteUser(Integer id){
        Session session = sessionFactory.openSession();
        Transaction tx;
        try {
            tx = session.beginTransaction();
            User u = (User) session.get(User.class, id);
            session.delete(u);
            tx.commit();
        }
        catch (Exception e) {
            log.warning(e.getMessage());
            throw new RuntimeException("database error");
        }
        finally {
            session.close();
        }
    }

    public static void updateUser(User user){
        Session session = sessionFactory.openSession();
        Transaction tx;
        try {
            tx = session.beginTransaction();
            session.update(user);
            tx.commit();
        }
        catch (Exception e) {
            log.warning(e.getMessage());
            throw new RuntimeException("database error");
        }
        finally {
            session.close();
        }
    }

    public static User login(String username, String password, String cookie){
        Session session = sessionFactory.openSession();
        Query q;
        if(password != null){
            q = session.createQuery("from User u where u.username = :username and u.passwordHash = :passwordHash");
            q.setParameter("username", username);
            q.setParameter("passwordHash", password);
        }else{
            q = session.createQuery("from User u where u.username = :username and u.cookie = :cookie");
            q.setParameter("username", username);
            q.setParameter("cookie", cookie);
        }
        List<User> users = q.list();
        if(users.size() > 0){
            User user = users.get(0);
            if(user.getCookie() == null){
                user.setCookie(UUID.randomUUID().toString());
                persistUser(user);
            }
            return user;
        }
        return null;
    }

}