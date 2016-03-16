package com.echo.pinterest.process;

/**
 * Created by jiangecho on 16/3/15.
 */

import com.echo.pinterest.conf.DBConf;
import com.google.gson.Gson;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.sql.*;

public class DBHandler implements PinHandler {

    private DBConf dbConf;

    private Connection connect = null;
    private Statement statement = null;

    /**
     * @param imageUrl
     * @param userName
     * @param boardName
     */
    @Override
    public void handle(String imageUrl, String userName, String boardName) {
        handle(imageUrl, boardName);
    }

    /**
     * @param imageUrl
     * @param sourceBoardName eg: the source name of https://jp.pinterest.com/source/bodybuilding.com is bodybuilding.com
     */
    @Override
    public void handle(String imageUrl, String sourceBoardName) {

        String querySql = "SELECT url FROM " + dbConf.table + " WHERE url = " + "'%s'";
        String insertSql = "INSERT INTO " + dbConf.table + " (url, boardName) " + "VALUES ('%s', '%s')";

        querySql = String.format(querySql, imageUrl);
        insertSql = String.format(insertSql, imageUrl, sourceBoardName);
        System.out.println("inserting: " + imageUrl);
        ResultSet resultSet;
        try {
            resultSet = statement.executeQuery(querySql);
            if (!resultSet.next()) {
                statement.execute(insertSql);
            }

            resultSet.close();
        } catch (SQLException e) {
            //e.printStackTrace();
            // do nothing
        }

    }

    @Override
    public boolean init() {
        try {
            dbConf = new Gson().fromJson(new FileReader("conf.json"), DBConf.class);
            Class.forName("com.mysql.jdbc.Driver");
            connect = DriverManager.getConnection("jdbc:mysql://" + dbConf.host, dbConf.user, dbConf.password);
            statement = connect.createStatement();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return false;
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            System.out.println("load jdbc driver failed");
            return false;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }


        return true;
    }

    @Override
    public void deInit() {
        try {
            if (statement != null) {
                statement.close();
            }
            if (connect != null) {
                connect.close();
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

    }
}
