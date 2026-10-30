
package com.dinedesk.database;

import java.sql.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class Database {

    private Database() {
    }

    // Get the permanent DineDesk database location.
    private static Path getDatabasePath() throws SQLException {
        String appData = System.getenv("APPDATA");

        Path appDataPath;

        if (appData != null && !appData.isBlank()) {
            appDataPath = Paths.get(appData);
        } else {
            appDataPath = Paths.get(
                    System.getProperty("user.home"),
                    "AppData",
                    "Roaming"
            );
        }

        Path directory = appDataPath.resolve("DineDesk");

        try {
            Files.createDirectories(directory);
        } catch (java.io.IOException e) {
            throw new SQLException(
                    "Unable to create DineDesk database directory.", e
            );
        }

        return directory.resolve("dinedesk.db");
    }

    // Open the database connection.
    public static Connection connect() throws SQLException {

        Path dbPath = getDatabasePath();

        System.out.println("DineDesk Database: " + dbPath);

        Connection connection = DriverManager.getConnection(
                "jdbc:sqlite:" + dbPath
        );

        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys=ON");
            statement.execute("PRAGMA busy_timeout=5000");
        }

        return connection;
    }

    // Both methods use exactly the same database.
    public static Connection getConnection() throws SQLException {
        return connect();
    }

    public static void initialize() {

        String[] q = {
                "CREATE TABLE IF NOT EXISTS categories(" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "name TEXT NOT NULL UNIQUE)",

                "CREATE TABLE IF NOT EXISTS menu_items(" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "category_id INTEGER NOT NULL," +
                        "name TEXT NOT NULL," +
                        "price REAL NOT NULL," +
                        "FOREIGN KEY(category_id) REFERENCES categories(id))",

                "CREATE TABLE IF NOT EXISTS orders(" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "order_date TEXT NOT NULL," +
                        "order_time TEXT NOT NULL DEFAULT '00:00:00'," +
                        "token_number INTEGER NOT NULL," +
                        "total_bill REAL NOT NULL," +
                        "UNIQUE(order_date,token_number))",

                "CREATE TABLE IF NOT EXISTS order_items(" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "order_id INTEGER NOT NULL," +
                        "menu_item_id INTEGER NOT NULL," +
                        "quantity INTEGER NOT NULL," +
                        "price REAL NOT NULL," +
                        "FOREIGN KEY(order_id) REFERENCES orders(id) ON DELETE CASCADE," +
                        "FOREIGN KEY(menu_item_id) REFERENCES menu_items(id))"
        };

        try (
                Connection c = connect();
                Statement s = c.createStatement()
        ) {

            for (String x : q) {
                s.execute(x);
            }

            // Migrate existing databases that predate the order_time column.
            boolean hasOrderTime = false;

            try (ResultSet columns =
                         s.executeQuery("PRAGMA table_info(orders)")) {

                while (columns.next()) {
                    if ("order_time".equalsIgnoreCase(
                            columns.getString("name"))) {
                        hasOrderTime = true;
                        break;
                    }
                }
            }

            if (!hasOrderTime) {
                s.execute(
                        "ALTER TABLE orders ADD COLUMN " +
                                "order_time TEXT NOT NULL DEFAULT '00:00:00'"
                );
            }

            String[] cats = {
                    "Tiffins",
                    "Dosa Items",
                    "Rice Items & Chinese",
                    "Meals",
                    "Rotis",
                    "Sweets & Snacks",
                    "Juice"
            };

            try (
                    PreparedStatement p = c.prepareStatement(
                            "INSERT OR IGNORE INTO categories(name) VALUES(?)"
                    )
            ) {

                for (String n : cats) {
                    p.setString(1, n);
                    p.addBatch();
                }

                p.executeBatch();
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
