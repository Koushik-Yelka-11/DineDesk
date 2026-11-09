
package com.dinedesk.database;

import java.sql.*;

public final class Database {

    private Database() {
    }

    public static Connection connect() throws SQLException {

        java.nio.file.Path dbPath =
                java.nio.file.Paths.get("dinedesk.db").toAbsolutePath();

        System.out.println("DineDesk Database: " + dbPath);

        Connection c = DriverManager.getConnection(
                "jdbc:sqlite:" + dbPath
        );

        try (Statement s = c.createStatement()) {
            s.execute("PRAGMA foreign_keys=ON");
            s.execute("PRAGMA busy_timeout=5000");
        }

        return c;
    }

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

        try (Connection c = connect()) {

            c.setAutoCommit(false);

            try {

                try (Statement s = c.createStatement()) {

                    for (String sql : q) {
                        s.execute(sql);
                    }

                    // Add order_time to older databases if required.
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
                }

                // Create the required categories.
                String[] cats = {
                        "Tiffins",
                        "Dosa Items",
                        "Rice Items & Chinese",
                        "Meals",
                        "Rotis",
                        "Sweets & Snacks",
                        "Juice"
                };

                try (PreparedStatement p = c.prepareStatement(
                        "INSERT OR IGNORE INTO categories(name) VALUES(?)")) {

                    for (String name : cats) {
                        p.setString(1, name);
                        p.addBatch();
                    }

                    p.executeBatch();
                }

                // Merge Rice Items and Chinese into Rice Items & Chinese.
                String mergeSql =
                        "UPDATE menu_items " +
                                "SET category_id = (" +
                                "    SELECT id FROM categories " +
                                "    WHERE name = 'Rice Items & Chinese'" +
                                ") " +
                                "WHERE category_id IN (" +
                                "    SELECT id FROM categories " +
                                "    WHERE name IN ('Rice Items', 'Chinese')" +
                                ")";

                try (Statement s = c.createStatement()) {
                    int movedItems = s.executeUpdate(mergeSql);

                    System.out.println(
                            "Menu items moved into Rice Items & Chinese: "
                                    + movedItems
                    );

                    // Remove the old categories after moving their items.
                    s.executeUpdate(
                            "DELETE FROM categories " +
                                    "WHERE name IN ('Rice Items', 'Chinese')"
                    );
                }

                c.commit();

                System.out.println(
                        "DineDesk database initialization and category migration completed."
                );

            } catch (SQLException e) {

                c.rollback();
                throw e;
            }

        } catch (SQLException e) {
            throw new RuntimeException(
                    "Database initialization failed.", e
            );
        }
    }
}
