package com.sulyagina.kp_dao;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import org.junit.Test;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by anastasia on 29.04.16.
 */
public class Tests {
    @Test
    public void testAddGet() {
        User user = new User();
        user.setName("Ivan");
        user.setAge(15);
        user.setDescription("Test description");

        ReflectionJdbcDAO<User> myUserDAO = new ReflectionJdbcDAOImpl<>();
        myUserDAO.insert(user);
        User newUser = myUserDAO.selectByKey(user);
        myUserDAO.deleteByKey(user);
        assertTrue ("Inserted and selected elements should be equal",
                ((ReflectionJdbcDAOImpl)myUserDAO).areEqual(user, newUser));
    }

    @Test
    public void testAddUpdate() {
        User user = new User();
        user.setName("Ivan");
        user.setAge(15);
        user.setDescription("Test description");

        ReflectionJdbcDAO<User> myUserDAO = new ReflectionJdbcDAOImpl<>();
        myUserDAO.insert(user);
        user.setDescription("New description");
        myUserDAO.update(user);
        User newUser = myUserDAO.selectByKey(user);
        myUserDAO.deleteByKey(user);
        assertEquals ("User description should be updated", newUser.getDescription(), "New description");
    }

    @Test
    public void testAddSelectAll() {
        User user = new User();
        user.setName("Ivan");
        user.setAge(15);
        user.setDescription("Test description");

        User secondUser = new User();
        secondUser.setName("Anna");
        secondUser.setAge(27);
        secondUser.setDescription("Anna's description");

        ReflectionJdbcDAO<User> myUserDAO = new ReflectionJdbcDAOImpl<>(User.class);
        List<User> users = myUserDAO.selectAll();
        for (User u: users) {
            myUserDAO.deleteByKey(u);
        }
        myUserDAO.insert(user);
        myUserDAO.insert(secondUser);

        users = myUserDAO.selectAll();
        assertEquals ("Amount of inserted users should be equal to selected", users.size(), 2);
    }

    public static void main(String[] args) {
        Result result = JUnitCore.runClasses(Tests.class);
        for (Failure failure : result.getFailures()) {
            System.out.println(failure.toString());
        }

    }
}
