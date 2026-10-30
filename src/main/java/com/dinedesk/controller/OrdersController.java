package com.dinedesk.controller;

import com.dinedesk.database.Database;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TitledPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class OrdersController extends NavController {

    @FXML private TextField searchField;
    @FXML private VBox ordersContainer;

    private static final DateTimeFormatter DISPLAY_TIME = DateTimeFormatter.ofPattern("hh:mm a");
    private static final DateTimeFormatter DISPLAY_DATE = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH);

    public static class OrderRow {
        public final IntegerProperty id = new SimpleIntegerProperty();
        public final StringProperty date = new SimpleStringProperty();
        public final StringProperty time = new SimpleStringProperty();
        public final IntegerProperty token = new SimpleIntegerProperty();
        public final DoubleProperty total = new SimpleDoubleProperty();

        private final String rawDate;
        private final String rawTime;

        OrderRow(int id, String date, String time, int token, double total) {
            this.id.set(id);
            this.date.set(date == null ? "" : date);
            this.time.set(formatTime(time));
            this.token.set(token);
            this.total.set(total);
            this.rawDate = date == null ? "" : date;
            this.rawTime = time == null ? "" : time;
        }

        private static String formatTime(String value) {
            if (value == null || value.isBlank()) return "--:--";
            try { return LocalTime.parse(value).format(DISPLAY_TIME); }
            catch (Exception ignored) { return value; }
        }
    }

    @FXML
    private void initialize() {
        load("");
        searchField.textProperty().addListener((observable, oldValue, newValue) -> load(newValue));
    }

    private void load(String query) {
        String search = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        Map<String, List<OrderRow>> byDate = new LinkedHashMap<>();

        String sql = """
                SELECT id, order_date, order_time, token_number, total_bill
                FROM orders
                ORDER BY order_date DESC, order_time DESC, id DESC
                """;

        try (Connection connection = Database.connect();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {

            while (result.next()) {
                OrderRow row = new OrderRow(
                        result.getInt("id"), result.getString("order_date"),
                        result.getString("order_time"), result.getInt("token_number"),
                        result.getDouble("total_bill"));

                String searchable = (row.id.get() + " " + row.rawDate + " " + row.rawTime + " "
                        + row.token.get() + " " + row.total.get() + " " + row.time.get()).toLowerCase(Locale.ROOT);
                if (!search.isEmpty() && !searchable.contains(search)) continue;
                byDate.computeIfAbsent(row.rawDate.isBlank() ? "Unknown date" : row.rawDate,
                        ignored -> new ArrayList<>()).add(row);
            }

            ordersContainer.getChildren().clear();
            if (byDate.isEmpty()) {
                Label empty = new Label(search.isEmpty() ? "No orders found." : "No orders match your search.");
                empty.setStyle("-fx-text-fill: #64748B; -fx-padding: 24; -fx-font-size: 14px;");
                ordersContainer.getChildren().add(empty);
                return;
            }

            String today = LocalDate.now().toString();
            String yesterday = LocalDate.now().minusDays(1).toString();
            for (Map.Entry<String, List<OrderRow>> entry : byDate.entrySet()) {
                String date = entry.getKey();
                List<OrderRow> rows = entry.getValue();
                double dailyTotal = rows.stream().mapToDouble(r -> r.total.get()).sum();
                String labelDate = date;
                if (date.equals(today)) labelDate = "Today — " + formatDate(date);
                else if (date.equals(yesterday)) labelDate = "Yesterday — " + formatDate(date);
                else labelDate = formatDate(date);

                TableView<OrderRow> table = createTable();
                table.getItems().setAll(rows);
                table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
                table.setPrefHeight(Math.min(420, Math.max(100, rows.size() * 42 + 32)));
                table.setMinHeight(Math.min(420, Math.max(100, rows.size() * 42 + 32)));

                Label summary = new Label(rows.size() + (rows.size() == 1 ? " order" : " orders")
                        + "     •     Daily sales: ₹" + String.format(Locale.ROOT, "%.2f", dailyTotal));
                summary.setStyle("-fx-text-fill: #64748B; -fx-font-size: 12px; -fx-padding: 0 0 8 2;");
                VBox content = new VBox(8, summary, table);
                content.setStyle("-fx-padding: 12 4 8 4;");

                TitledPane datePane = new TitledPane(labelDate, content);
                datePane.setExpanded(date.equals(today) || !search.isEmpty());
                datePane.setAnimated(false);
                datePane.setStyle("-fx-font-size: 13px; -fx-font-weight: bold;");
                ordersContainer.getChildren().add(datePane);
            }
        } catch (Exception exception) {
            exception.printStackTrace();
            new Alert(Alert.AlertType.ERROR,
                    "Could not load order history:\n" + exception.getMessage()).showAndWait();
        }
    }

    private TableView<OrderRow> createTable() {
        TableView<OrderRow> table = new TableView<>();
        TableColumn<OrderRow, Number> id = new TableColumn<>("Order ID");
        id.setCellValueFactory(row -> row.getValue().id);
        id.setPrefWidth(100);

        TableColumn<OrderRow, String> date = new TableColumn<>("Date");
        date.setCellValueFactory(row -> row.getValue().date);
        date.setPrefWidth(150);

        TableColumn<OrderRow, String> time = new TableColumn<>("Time");
        time.setCellValueFactory(row -> row.getValue().time);
        time.setPrefWidth(130);

        TableColumn<OrderRow, Number> token = new TableColumn<>("Daily token");
        token.setCellValueFactory(row -> row.getValue().token);
        token.setPrefWidth(130);

        TableColumn<OrderRow, Number> total = new TableColumn<>("Total (₹)");
        total.setCellValueFactory(row -> row.getValue().total);
        total.setPrefWidth(130);

        table.getColumns().addAll(id, date, time, token, total);
        table.setPlaceholder(new Label("No orders for this date."));
        return table;
    }

    private String formatDate(String value) {
        try { return LocalDate.parse(value).format(DISPLAY_DATE); }
        catch (Exception ignored) { return value; }
    }
}
