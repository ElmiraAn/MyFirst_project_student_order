package edu.javacourse.studentorder.dao;

import edu.javacourse.studentorder.config.Config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class ConnectionBuilder {
    public static Connection getConnection() throws SQLException {
        Connection con = DriverManager.getConnection(
                Config.getProperty(Config.DB_URL),//сделали конфигурирование наших свойств подключения к БЗ
                Config.getProperty(Config.DB_LOGIN),
                Config.getProperty(Config.DB_PASSWORD)
        );
        return con;// подкючение к БД
    }
}
