package com.sulyagina.kp_dao;

import java.io.*;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Created by anastasia on 23.04.16.
 */
public class ReflectionJdbcDAOImpl < T > implements ReflectionJdbcDAO< T > {

    private List<String> primaryKey = new ArrayList<>();
    private List<String> fields = new ArrayList<>();
    private String tableName;
    private String className;

    public ReflectionJdbcDAOImpl() {

    }


    private String convertFieldForDB(String fieldName) {
        StringBuilder s = new StringBuilder();
        for (int i = 0; i < fieldName.length(); ++i) {
            Character ch = fieldName.charAt(i);
            if (Character.isUpperCase(ch)) {
                s.append('_').append(Character.toLowerCase(ch));
            } else {
                s.append(ch);
            }
        }
        return s.toString();
    }

    private String convertFieldForClass(String fieldName) {
        StringBuilder s = new StringBuilder();
        for (int i = 0; i < fieldName.length(); ++i) {
            Character ch = fieldName.charAt(i);
            if (ch.equals('_')) {
                s.append(Character.toUpperCase(fieldName.charAt(i + 1)));
                i += 2;
            } else {
                s.append(ch);
            }
        }
        return s.toString();
    }

    private String format(List<String> fields, String toAppend, String separator) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < fields.size(); ++i) {
            sb.append(convertFieldForDB(fields.get(i))).append(toAppend);
            if (i + 1 != fields.size()) {
                sb.append(separator);
            }
        }
        return sb.toString();
    }

    public void insert(T object) {
        Connection c = null;
        Statement stmt = null;

        try {
            Class<T> obj = (Class<T>) object.getClass();
            Class.forName("org.sqlite.JDBC");
            c = DriverManager.getConnection("jdbc:sqlite:/Users/anastasia/Development/Kinopoisk/test.db");

            int fieldsCnt = 0;
            if (!obj.isAnnotationPresent(Table.class)) {
                throw new Exception("No table found");
            }
            if (tableName == null) {
                Annotation annotation = obj.getAnnotation(Table.class);
                Table table = (Table) annotation;
                tableName = table.name();
                className = obj.getName();
                for (Field field : obj.getDeclaredFields()) {
                    String fieldName = field.getName();
                    fields.add(convertFieldForDB(fieldName));
                    if (field.isAnnotationPresent(PK.class)) {
                        primaryKey.add(fieldName);
                    }
                    fieldsCnt++;
                }
            }

            stmt = c.createStatement();
            StringBuilder columns = new StringBuilder();
            for (String field : fields) {
                columns.append(field).append(" BLOB,");
            }
            String sql = "CREATE TABLE IF NOT EXISTS " + tableName + "(" + columns.substring(0, columns.length() - 1) + ");";
            stmt.executeUpdate(sql);
            stmt.close();

            sql = "INSERT INTO " + tableName + " (" + String.join(",", fields) + ") " +
                    "VALUES(" + format(new ArrayList<String>(Collections.nCopies(fields.size(), "")), "?", ",") + ");";
            PreparedStatement pstmt = c.prepareStatement(sql);
            fieldsCnt = 1;
            for (Field field : obj.getDeclaredFields()) {
                Object x = getFieldValue(field, object);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ObjectOutputStream oos = new ObjectOutputStream(baos);
                oos.writeObject(x);
                oos.close();
                pstmt.setBytes(fieldsCnt, baos.toByteArray());
                fieldsCnt++;
            }
            pstmt.executeUpdate();
            pstmt.close();
            c.close();

        } catch (Exception e) {
            System.out.println("Wrong object");
        }
    }

    private Object getFieldValue(Field field, Object object) {
        try {
            String fieldName = field.getName();
            String fieldGet = "get" + fieldName.replace(fieldName.charAt(0),
                    Character.toUpperCase(fieldName.charAt(0)));
            Method method = object.getClass().getMethod(fieldGet);
            return method.invoke(object);
        } catch (Exception e) {}
        return null;
    }

    private ArrayList<ByteArrayOutputStream> getObjectByKey(T object) {
        Class<T> obj = (Class<T>) object.getClass();
        ArrayList<ByteArrayOutputStream> keys = new ArrayList<>();
        try {
            for (String f : primaryKey) {

                Field field = obj.getDeclaredField(f);
                Object x = getFieldValue(field, object);

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ObjectOutputStream oos = new ObjectOutputStream(baos);
                oos.writeObject(x);
                oos.close();
                keys.add(baos);

            }
        } catch (Exception e) {
            System.out.println("Object is not valid");
        }
        return keys;
    }

    public void update(T object) {
        Connection c = null;

        try {
            Class<T> obj = (Class<T>) object.getClass();
            Class.forName("org.sqlite.JDBC");
            c = DriverManager.getConnection("jdbc:sqlite:/Users/anastasia/Development/Kinopoisk/test.db");

            //TODO insert correctly
            String sql = "UPDATE " + tableName + " SET " + format(fields, "=?", ",") +
                    " WHERE (" + format(primaryKey, "=?", " AND ") + ");";
            PreparedStatement pstmt = c.prepareStatement(sql);
            int fieldsCnt = 1;
            for (Field field : obj.getDeclaredFields()) {
                Object x = getFieldValue(field, object);

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ObjectOutputStream oos = new ObjectOutputStream(baos);
                oos.writeObject(x);
                oos.close();
                pstmt.setBytes(fieldsCnt, baos.toByteArray());
                fieldsCnt++;
            }

            ArrayList<ByteArrayOutputStream> keys = getObjectByKey(object);
            for (ByteArrayOutputStream stream : keys) {
                pstmt.setBytes(fieldsCnt++, stream.toByteArray());
            }
            pstmt.executeUpdate();
            pstmt.close();
            c.close();
        } catch (Exception e) {
            System.out.println("Wrong object");
        }
    }

    private T getObject(ResultSet rs) {
        try {
            Class obj = Class.forName(className);
            Object y = obj.newInstance();
            ResultSetMetaData rsmd = rs.getMetaData();
            int columnCount = rsmd.getColumnCount();

            for (int i = 1; i <= columnCount; i++) {
                String name = rsmd.getColumnName(i);

                InputStream is = rs.getBinaryStream(name);
                ObjectInputStream ois = new ObjectInputStream(is);
                Object value = ois.readObject();

                String fieldName = convertFieldForClass(name);
                Field field = obj.getDeclaredField(fieldName);

                String fieldSet = "set" + fieldName.replace(fieldName.charAt(0),
                        Character.toUpperCase(fieldName.charAt(0)));
                Method method = obj.getMethod(fieldSet, field.getType());
                Object v = field.getType().cast(value);
                method.invoke(y, v);
            }
            return (T)y;
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        return null;
    }

    public void deleteByKey(T key) {
        Connection c = null;
        try {
            Class.forName("org.sqlite.JDBC");
            c = DriverManager.getConnection("jdbc:sqlite:/Users/anastasia/Development/Kinopoisk/test.db");

            String sql = "DELETE FROM " + tableName + " WHERE " + format(primaryKey, "=?", " AND ") + ";";
            PreparedStatement pstmt = c.prepareStatement(sql);

            ArrayList<ByteArrayOutputStream> keys = getObjectByKey(key);
            for (int i = 0; i < primaryKey.size(); ++i) {
                pstmt.setBytes(i + 1, keys.get(i).toByteArray());
            }

            ResultSet rs = pstmt.executeQuery();
            rs.next();
            T x = getObject(rs);

            rs.close();
            pstmt.close();
            c.close();
        } catch ( Exception e ) {
            System.err.println("No object matching key found");
        }
    }


    public T selectByKey(T key) {
        Connection c = null;
        try {
            Class.forName("org.sqlite.JDBC");
            c = DriverManager.getConnection("jdbc:sqlite:/Users/anastasia/Development/Kinopoisk/test.db");

            String sql =  "SELECT * FROM " + tableName + " WHERE " + format(primaryKey, "=?", " AND ") + ";";
            PreparedStatement pstmt = c.prepareStatement(sql);

            ArrayList<ByteArrayOutputStream> keys = getObjectByKey(key);
            for (int i = 0; i < primaryKey.size(); ++i) {
                pstmt.setBytes(i + 1, keys.get(i).toByteArray());
            }
            ResultSet rs = pstmt.executeQuery();
            rs.next();
            T x = getObject(rs);

            rs.close();
            pstmt.close();
            c.close();
            return x;
        } catch ( Exception e ) {
            System.err.println("No object matching key found");
        }
        return null;
    }

    public List< T > selectAll() {
        List<T> objects = new ArrayList<>();
        Connection c = null;
        Statement stmt = null;
        try {
            Class.forName("org.sqlite.JDBC");
            c = DriverManager.getConnection("jdbc:sqlite:/Users/anastasia/Development/Kinopoisk/test.db");

            this.getClass().getG
            stmt = c.createStatement();
            ResultSet rs = stmt.executeQuery( "SELECT * FROM " + tableName + ";");
            while ( rs.next() ) {
                T x = getObject(rs);
                objects.add(x);
            }
            rs.close();
            stmt.close();
            c.close();
        } catch ( Exception e ) {
            System.err.println(e.getMessage());
        }
        return objects;
    }
}
