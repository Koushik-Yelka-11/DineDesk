package com.dinedesk.database;

import java.sql.*;
import java.nio.file.Path;

public class DatabaseInspector {

    private static void inspect(String databasePath) {

        System.out.println("\n==================================");
        System.out.println("DATABASE: " + databasePath);
        System.out.println("==================================");

        String url = "jdbc:sqlite:" + databasePath;

        try (Connection connection = DriverManager.getConnection(url)) {

            connection.setReadOnly(true);

            String[] tables = {
                    "categories",
                    "menu_items",
                    "orders",
                    "order_items"
            };

            for (String table : tables) {

                String sql = "SELECT COUNT(*) FROM " + table;

                try (Statement statement = connection.createStatement();
                     ResultSet result = statement.executeQuery(sql)) {

                    if (result.next()) {
                        System.out.println(
                                table + " : " + result.getInt(1)
                        );
                    }
                }
            }

            System.out.println("\nCategories:");

            try (Statement statement = connection.createStatement();
                 ResultSet result = statement.executeQuery(
                         "SELECT id, name FROM categories ORDER BY id"
                 )) {

                while (result.next()) {
                    System.out.println(
                            result.getInt("id") + " - " +
                                    result.getString("name")
                    );
                }
            }

        } catch (SQLException e) {
            System.out.println("Inspection error: " + e.getMessage());
        }
    }

    public static void main(String[] args) {

        inspect(
                "C:/Users/HOME/AppData/Roaming/DineDesk/dinedesk.db"
        );

        inspect(
                "C:/Users/HOME/AppData/Roaming/DineDesk/dinedesk_before_migration.db"
        );

        inspect(
                "C:/Users/HOME/Desktop/DineDesk_Backups/dinedesk_windows_original.db"
        );

        inspect(
                "C:/Users/HOME/Desktop/DineDesk_Backups/dinedesk_windows_before_migration.db"
        );
    }
}
