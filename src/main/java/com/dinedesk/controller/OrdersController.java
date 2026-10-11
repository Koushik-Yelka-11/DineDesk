package com.dinedesk.controller;

import com.dinedesk.database.Database;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class OrdersController extends NavController {

    @FXML private TextField searchField;
    @FXML private TableView<OrderRow> ordersTable;

    @FXML private TableColumn<OrderRow, Number> idColumn;
    @FXML private TableColumn<OrderRow, String> dateColumn;
    @FXML private TableColumn<OrderRow, String> timeColumn;
    @FXML private TableColumn<OrderRow, Number> tokenColumn;
    @FXML private TableColumn<OrderRow, Number> totalColumn;

    private static final DateTimeFormatter DISPLAY_TIME =
            DateTimeFormatter.ofPattern("hh:mm a");

    public static class OrderRow {
        public final javafx.beans.property.IntegerProperty id =
                new javafx.beans.property.SimpleIntegerProperty();
        public final javafx.beans.property.StringProperty date =
                new javafx.beans.property.SimpleStringProperty();
        public final javafx.beans.property.StringProperty time =
                new javafx.beans.property.SimpleStringProperty();
        public final javafx.beans.property.IntegerProperty token =
                new javafx.beans.property.SimpleIntegerProperty();
        public final javafx.beans.property.DoubleProperty total =
                new javafx.beans.property.SimpleDoubleProperty();

        OrderRow(int id, String date, String time, int token, double total) {
            this.id.set(id);
            this.date.set(date == null ? "" : date);
            this.time.set(formatTime(time));
            this.token.set(token);
            this.total.set(total);
        }

        private static String formatTime(String value) {
            if (value == null || value.isBlank()) {
                return "--:--";
            }
            try {
                return LocalTime.parse(value).format(DISPLAY_TIME);
            } catch (Exception ignored) {
                return value;
            }
        }
    }

    @FXML
    private void initialize() {
        idColumn.setCellValueFactory(row -> row.getValue().id);
        dateColumn.setCellValueFactory(row -> row.getValue().date);
        timeColumn.setCellValueFactory(row -> row.getValue().time);
        tokenColumn.setCellValueFactory(row -> row.getValue().token);
        totalColumn.setCellValueFactory(row -> row.getValue().total);

        load("");

        searchField.textProperty().addListener(
                (observable, oldValue, newValue) -> load(newValue)
        );
    }

    private void load(String query) {
        var rows = FXCollections.<OrderRow>observableArrayList();
        String search = query == null ? "" : query.trim();

        String sql = """
                SELECT id, order_date, order_time, token_number, total_bill
                FROM orders
                WHERE CAST(token_number AS TEXT) LIKE ?
                   OR order_date LIKE ?
                   OR COALESCE(order_time, '') LIKE ?
                ORDER BY id DESC
                """;

        try (Connection connection = Database.connect();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            String pattern = "%" + search + "%";
            statement.setString(1, pattern);
            statement.setString(2, pattern);
            statement.setString(3, pattern);

            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    rows.add(new OrderRow(
                            result.getInt("id"),
                            result.getString("order_date"),
                            result.getString("order_time"),
                            result.getInt("token_number"),
                            result.getDouble("total_bill")
                    ));
                }
            }

            ordersTable.setItems(rows);

        } catch (Exception exception) {
            exception.printStackTrace();
            new Alert(
                    Alert.AlertType.ERROR,
                    "Could not load order history:\n" + exception.getMessage()
            ).showAndWait();
        }
    }
}
