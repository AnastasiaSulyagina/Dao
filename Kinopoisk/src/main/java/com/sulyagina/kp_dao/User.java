package com.sulyagina.kp_dao;

import java.io.Serializable;

/**
 * Created by anastasia on 23.04.16.
 */
@Table(name = "user")
public class User implements Serializable {

    @PK
    private String name;
    @PK
    private int age;
    private String smth;

    public User() {}

    public User(String name, int age) {
        this.name = name;
        this.age = age;
    }
    public int getAge() {
        return age;
    }
    public void setAge( int age ) {
        this.age = age;
    }
    public String getName() {
        return name;
    }
    public void setName( String name ) {
        this.name = name;
    }
    public String getSmth() {
        return smth;
    }
    public void setSmth( String smth ) {
        this.smth = smth;
    }
}
