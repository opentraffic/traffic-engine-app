package io.opentraffic.engine.app.util;

import io.opentraffic.engine.app.data.SavedRoute;
import io.opentraffic.engine.app.data.User;

import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;
import spark.Request;
import spark.Route;
import spark.Response;


public class AuthUtil {

    private static final Logger log = Logger.getLogger(AuthUtil.class.getName());
/*
    public static SessionFactory getSessionFactory() {
        return sessionFactory;
    }

    public static void persistEntity(Object obj){
        Session session = sessionFactory.openSession();
        Transaction tx;
        try {
            tx = session.beginTransaction();
            session.saveOrUpdate(obj);
            tx.commit();
        }
        catch (Exception e) {
            log.warning(e.getMessage());
            throw new RuntimeException(e.getMessage());
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

    public static SavedRoute getSavedRoute(Integer id) {
        Session session = sessionFactory.openSession();
        return (SavedRoute) session.get(SavedRoute.class, id);
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

    public static void deleteSavedRoute(Integer id){
        Session session = sessionFactory.openSession();
        Transaction tx;
        try {
            tx = session.beginTransaction();
            SavedRoute savedRoute = (SavedRoute) session.get(SavedRoute.class, id);
            session.delete(savedRoute);
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
            session.merge(user);
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

    public static List<SavedRoute> getRoutesForUser(User user, String country){
        Session session = sessionFactory.openSession();
        Query q = session.createQuery("from SavedRoute sr where sr.name is not null and sr.user = :user and sr.country = :country");
        q.setParameter("user", user);
        q.setParameter("country", country);
        return q.list();
    }

    public static User login(String username, String password, String cookie){
        Session session = sessionFactory.openSession();
        Query q;
        if(password != null){
            q = session.createQuery("from User u left join fetch u.savedRoutes where u.username = :username and u.passwordHash = :passwordHash");
            q.setParameter("username", username);
            q.setParameter("passwordHash", password);
        }else{
            q = session.createQuery("from User u left join fetch u.savedRoutes where u.username = :username and u.cookie = :cookie");
            q.setParameter("username", username);
            q.setParameter("cookie", cookie);
        }
        List<User> users = q.list();
        if(users.size() > 0){
            User user = users.get(0);
            if(user.getCookie() == null){
                user.setCookie(UUID.randomUUID().toString());
                Transaction tx;
                try {
                    tx = session.beginTransaction();
                    session.saveOrUpdate(user);
                    tx.commit();
                }
                catch (Exception e) {
                    log.warning(e.getMessage());
                    throw new RuntimeException(e.getMessage());
                }
                finally {
                    session.close();
                }
            }
            return user;
        }
        return null;
    }
*/
}
