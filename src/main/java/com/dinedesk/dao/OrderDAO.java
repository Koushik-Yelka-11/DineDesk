
package com.dinedesk.dao;

import com.dinedesk.database.Database;
import com.dinedesk.model.Order;
import com.dinedesk.model.OrderItem;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public class OrderDAO {

    public int saveOrder(Order order) throws SQLException {

        if (order == null || order.isEmpty()) {
            throw new IllegalArgumentException(
                    "Cannot save an empty order."
            );
        }

        String orderSql = """
                INSERT INTO orders
                    (order_date, token_number, total_bill)
                VALUES (?, ?, ?)
                """;

        String itemSql = """
                INSERT INTO order_items
                    (order_id, menu_item_id, quantity, price)
                VALUES (?, ?, ?, ?)
                """;

        try (Connection connection = Database.getConnection()) {
            connection.setAutoCommit(false);

            try {
                LocalDate today = LocalDate.now();
                int tokenNumber = getNextTokenNumber(connection, today);

                int orderId;

                try (PreparedStatement orderStatement =
                             connection.prepareStatement(
                                     orderSql,
                                     PreparedStatement.RETURN_GENERATED_KEYS
                             )) {

                    orderStatement.setString(1, today.toString());
                    orderStatement.setInt(2, tokenNumber);
                    orderStatement.setDouble(3, order.getTotalBill());

                    orderStatement.executeUpdate();

                    try (ResultSet keys =
                                 orderStatement.getGeneratedKeys()) {
                        if (!keys.next()) {
                            throw new SQLException(
                                    "Failed to generate order ID."
                            );
                        }

                        orderId = keys.getInt(1);
                    }
                }

                try (PreparedStatement itemStatement =
                             connection.prepareStatement(itemSql)) {

                    for (OrderItem item : order.getItems()) {
                        itemStatement.setInt(1, orderId);
                        itemStatement.setInt(
                                2, item.getMenuItem().getId()
                        );
                        itemStatement.setInt(3, item.getQuantity());
                        itemStatement.setDouble(
                                4, item.getMenuItem().getPrice()
                        );

                        itemStatement.addBatch();
                    }

                    itemStatement.executeBatch();
                }

                connection.commit();
                return orderId;

            } catch (SQLException | RuntimeException e) {
                connection.rollback();
                throw e;

            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    private int getNextTokenNumber(
            Connection connection,
            LocalDate today
    ) throws SQLException {

        String sql = """
                SELECT COALESCE(MAX(token_number), 0) + 1
                FROM orders
                WHERE order_date = ?
                """;

        try (PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, today.toString());

            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1);
            }
        }
    }
}
