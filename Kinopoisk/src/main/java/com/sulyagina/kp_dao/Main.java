package com.sulyagina.kp_dao;

import java.util.List;
import org.sqlite.JDBC;

/**
 * Created by anastasia on 28.04.16.
 */
public class Main {
    public static void main(String[] args) {
        User first_user = new User();
        first_user.setName("Ivan");
        first_user.setAge(15);
        first_user.setSmth("some description");

        ReflectionJdbcDAO<User> myUserDAO = new ReflectionJdbcDAOImpl<>();
        myUserDAO.insert(first_user);
        first_user.setAge(20);
        myUserDAO.update(first_user);

        List<User> users = myUserDAO.selectAll();
        User newUser = myUserDAO.selectByKey(first_user);
        //if (newUser.equals(first_user))
         //   System.out.println("YEP");
        //else System.out.println("Shit");

        User second_user = new User();
        second_user.setAge(10);
        second_user.setName("Anna");
        second_user.setSmth("77");
        myUserDAO.insert(second_user);

        //List<User> users = myUserDAO.selectAll();

    }
}
