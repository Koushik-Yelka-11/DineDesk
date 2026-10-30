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
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.geometry.Insets;
import javafx.scene.layout.HBox;
import javafx.beans.property.ReadOnlyIntegerWrapper;
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

                datePane.setStyle(
                        "-fx-font-size: 13px;" +
                                "-fx-font-weight: bold;" +
                                "-fx-border-color: transparent;" +
                                "-fx-background-color: transparent;" +
                                "-fx-background-insets: 0;" +
                                "-fx-border-insets: 0;"
                );

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

        TableColumn<OrderRow, Void> detailsColumn = new TableColumn<>("Details");
        detailsColumn.setPrefWidth(100);
        detailsColumn.setMinWidth(90);

        detailsColumn.setCellFactory(column -> new TableCell<>() {
            private final Button detailsButton = new Button("Details");

            {
                detailsButton.setStyle(
                        "-fx-background-color: #EAF2FF;" +
                                "-fx-text-fill: #245DDB;" +
                                "-fx-background-radius: 6;" +
                                "-fx-cursor: hand;"
                );

                detailsButton.setOnAction(event -> {
                    OrderRow selectedOrder = getTableView()
                            .getItems()
                            .get(getIndex());

                    showOrderDetails(selectedOrder.id.get());
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : detailsButton);
            }
        });
        table.getColumns().addAll(id, date, time, token, total, detailsColumn);
        table.setPlaceholder(new Label("No orders for this date."));
        return table;
    }
    private void showOrderDetails(int orderId) {
        TableView<ItemQuantity> itemsTable = new TableView<>();

        TableColumn<ItemQuantity, String> itemColumn =
                new TableColumn<>("Item");

        itemColumn.setCellValueFactory(data ->
                data.getValue().itemName);

        itemColumn.setPrefWidth(260);

        TableColumn<ItemQuantity, Number> quantityColumn =
                new TableColumn<>("Quantity");

        quantityColumn.setCellValueFactory(data ->
                data.getValue().quantity);

        quantityColumn.setPrefWidth(100);

        itemsTable.getColumns().addAll(itemColumn, quantityColumn);
        itemsTable.setColumnResizePolicy(
                TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN
        );

        String sql = """
            
                SELECT mi.name AS item_name,
                                           oi.quantity AS quantity
                                    FROM order_items oi
                                    INNER JOIN menu_items mi
                                        ON mi.id = oi.menu_item_id
                                    WHERE oi.order_id = ?
                                    ORDER BY mi.name
            """;

        try (Connection connection = Database.connect();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, orderId);

            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    itemsTable.getItems().add(
                            new ItemQuantity(
                                    result.getString("item_name"),
                                    result.getInt("quantity")
                            )
                    );
                }
            }

        } catch (Exception exception) {
            exception.printStackTrace();

            new Alert(
                    Alert.AlertType.ERROR,
                    "Could not load order items:\n" + exception.getMessage()
            ).showAndWait();

            return;
        }

        if (itemsTable.getItems().isEmpty()) {
            itemsTable.setPlaceholder(
                    new Label("No items found for this order.")
            );
        }

        itemsTable.setPrefHeight(
                Math.min(350, Math.max(100, itemsTable.getItems().size() * 40 + 45))
        );

        Label title = new Label("Order Details");
        title.setStyle(
                "-fx-font-size: 20px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-text-fill: #172033;"
        );

        Button closeButton = new Button("Close");
        closeButton.setPrefWidth(90);
        closeButton.setStyle(
                "-fx-background-color: #EAF2FF;" +
                        "-fx-text-fill: #245DDB;" +
                        "-fx-background-radius: 7;" +
                        "-fx-cursor: hand;"
        );

        HBox buttonRow = new HBox(closeButton);
        buttonRow.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);

        VBox layout = new VBox(15, title, itemsTable, buttonRow);
        layout.setPadding(new Insets(20));
        layout.setStyle("-fx-background-color: white;");

        Stage popup = new Stage();
        popup.setTitle("Order Details");
        popup.initModality(Modality.APPLICATION_MODAL);

        if (ordersContainer.getScene() != null
                && ordersContainer.getScene().getWindow() != null) {
            popup.initOwner(ordersContainer.getScene().getWindow());
        }

        popup.setScene(new Scene(layout, 400, 300));
        popup.setResizable(false);

        closeButton.setOnAction(event -> popup.close());

        popup.showAndWait();
    }
    public static class ItemQuantity {
        private final StringProperty itemName = new SimpleStringProperty();
        private final IntegerProperty quantity = new SimpleIntegerProperty();

        public ItemQuantity(String itemName, int quantity) {
            this.itemName.set(itemName);
            this.quantity.set(quantity);
        }
    }
    private String formatDate(String value) {
        try { return LocalDate.parse(value).format(DISPLAY_DATE); }
        catch (Exception ignored) { return value; }
    }
}
