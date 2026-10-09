
package com.dinedesk.dao;

import com.dinedesk.database.Database;
import com.dinedesk.model.MenuItem;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class MenuItemDAO {

    public List<MenuItem> getAllMenuItems() throws SQLException {
        List<MenuItem> items = new ArrayList<>();

        String sql = """
                SELECT id, category_id, name, price
                FROM menu_items
                ORDER BY category_id, name
                """;

        try (Connection connection = Database.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                items.add(new MenuItem(
                        resultSet.getInt("id"),
                        resultSet.getInt("category_id"),
                        resultSet.getString("name"),
                        resultSet.getDouble("price")
                ));
            }
        }

        return items;
    }

    public List<MenuItem> getMenuItemsByCategory(int categoryId)
            throws SQLException {

        List<MenuItem> items = new ArrayList<>();

        String sql = """
                SELECT id, category_id, name, price
                FROM menu_items
                WHERE category_id = ?
                ORDER BY name
                """;

        try (Connection connection = Database.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setInt(1, categoryId);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    items.add(new MenuItem(
                            resultSet.getInt("id"),
                            resultSet.getInt("category_id"),
                            resultSet.getString("name"),
                            resultSet.getDouble("price")
                    ));
                }
            }
        }

        return items;
    }
}
