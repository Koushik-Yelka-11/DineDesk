package com.dinedesk.controller;

import com.dinedesk.database.Database;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.sql.Statement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.util.Locale;

public class DashboardController extends NavController {

    @FXML private Label salesLabel;
    @FXML private Label ordersLabel;
    @FXML private Label dateLabel;
    @FXML private VBox recentOrdersBox;
    @FXML private VBox recentOrdersEmptyState;
    @FXML
    private void initialize() {
        LocalDate today = LocalDate.now();
        dateLabel.setText(today.toString());

        recentOrdersEmptyState.setVisible(false);
        recentOrdersEmptyState.setManaged(false);

        loadTodaySummary(today);
        loadRecentOrders();
    }

    private void loadTodaySummary(LocalDate today) {
        String sql = """
                SELECT COALESCE(SUM(total_bill), 0), COUNT(*)
                FROM orders
                WHERE order_date = ?
                """;

        try (Connection connection = Database.connect();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, today.toString());

            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) {
                    salesLabel.setText(
                            String.format(Locale.US, "₹%,.2f", result.getDouble(1)));
                    ordersLabel.setText(Integer.toString(result.getInt(2)));
                }
            }
        } catch (Exception e) {
            salesLabel.setText("₹0.00");
            ordersLabel.setText("0");
            e.printStackTrace();
        }
    }


    private void loadRecentOrders() {
        if (recentOrdersBox == null) {
            System.err.println(
                    "Dashboard FXML is missing fx:id=\"recentOrdersBox\".");
            return;
        }

        recentOrdersBox.getChildren().clear();

        String sql = """
        SELECT
            o.id,
            o.order_date,
            o.token_number,
            o.total_bill,
            o.order_time,
            COALESCE(SUM(oi.quantity), 0) AS item_count
        FROM orders o
        LEFT JOIN order_items oi ON oi.order_id = o.id
        WHERE o.order_date = ?
        GROUP BY o.id, o.order_date, o.token_number,
                 o.total_bill, o.order_time
        ORDER BY o.id DESC
        LIMIT 5
        """;

        boolean foundOrders = false;

        try (Connection connection = Database.connect();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, LocalDate.now().toString());

            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {

                    foundOrders = true;

                    GridPane row = new GridPane();
                    row.getStyleClass().add("orders-row");

                    for (int i = 0; i < 5; i++) {
                        ColumnConstraints column = new ColumnConstraints();
                        column.setPercentWidth(20);
                        column.setHgrow(Priority.ALWAYS);
                        row.getColumnConstraints().add(column);
                    }

                    String token = Integer.toString(
                            result.getInt("token_number")
                    );
                    String orderDate = result.getString("order_date");
                    String orderTime = result.getString("order_time");

                    String displayDate = orderDate;
                    try {
                        displayDate = LocalDate.parse(orderDate)
                                .format(DateTimeFormatter.ofPattern("dd MMM yyyy"));
                    } catch (Exception ignored) {
                        // Keep the original date.
                    }

                    String displayTime = "--:--";
                    if (orderTime != null && !orderTime.isBlank()) {
                        try {
                            displayTime = LocalTime.parse(orderTime)
                                    .format(DateTimeFormatter.ofPattern("hh:mm a"));
                        } catch (Exception ignored) {
                            displayTime = orderTime;
                        }
                    }

                    int itemCount = result.getInt("item_count");
                    double amount = result.getDouble("total_bill");

                    addCell(row, "#" + token, 0, "order-id");
                    addCell(row, displayDate + "\n" + displayTime, 1, "body-copy");
                    addCell(row, itemCount + (itemCount == 1 ? " item" : " items"),
                            2, "body-copy");
                    addCell(row, String.format(Locale.US, "₹%,.2f", amount),
                            3, "header-title");
                    addCell(row, "Saved", 4, "paid-status");

                    recentOrdersBox.getChildren().add(row);
                }
            }

            if (recentOrdersEmptyState != null) {
                recentOrdersEmptyState.setVisible(!foundOrders);
                recentOrdersEmptyState.setManaged(!foundOrders);
            }

        } catch (Exception e) {
            e.printStackTrace();

            if (recentOrdersEmptyState != null) {
                recentOrdersEmptyState.setVisible(false);
                recentOrdersEmptyState.setManaged(false);
            }

            Label error = new Label(
                    "Could not load recent orders. Check the Run console.");
            error.getStyleClass().add("body-copy");
            error.setWrapText(true);
            error.setStyle("-fx-padding: 24px;");
            recentOrdersBox.getChildren().add(error);
        }
    }


    private void addCell(GridPane row, String text, int column, String styleClass) {
        Label label = new Label(text);
        label.getStyleClass().add(styleClass);
        label.setMaxWidth(Double.MAX_VALUE);
        label.setWrapText(true);
        label.setStyle("-fx-padding: 0 10px;");
        GridPane.setHgrow(label, Priority.ALWAYS);
        row.add(label, column, 0);
    }
}
