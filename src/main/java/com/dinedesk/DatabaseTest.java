
package com.dinedesk;

import com.dinedesk.database.Database;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

public class DatabaseTest {

    public static void main(String[] args) {
        String sql = "SELECT name FROM sqlite_master "
                + "WHERE type = 'table' ORDER BY name";

        try (Connection connection = Database.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {

            System.out.println("Database connected successfully!");
            System.out.println("Tables:");

            while (resultSet.next()) {
                System.out.println("- " + resultSet.getString("name"));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
