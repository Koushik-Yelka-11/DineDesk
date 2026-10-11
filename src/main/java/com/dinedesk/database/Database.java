
package com.dinedesk.database;

import java.sql.*;

public final class Database {

    private Database() {
    }

    public static Connection connect() throws SQLException {
        Connection c = DriverManager.getConnection("jdbc:sqlite:dinedesk.db");

        try (Statement s = c.createStatement()) {
            s.execute("PRAGMA foreign_keys=ON");
            s.execute("PRAGMA busy_timeout=5000");
        }

        return c;
    }

    public static java.sql.Connection getConnection() throws java.sql.SQLException {
        java.sql.Connection connection =
                java.sql.DriverManager.getConnection("jdbc:sqlite:dinedesk.db");

        try (java.sql.Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON");
            statement.execute("PRAGMA busy_timeout = 5000");
        }

        return connection;
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

            String[] cats = {
                    "Tiffins",
                    "Dosa Items",
                    "Rice Items",
                    "Meals",
                    "Chinese",
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
