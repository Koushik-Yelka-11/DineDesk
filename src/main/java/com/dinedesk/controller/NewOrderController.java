
package com.dinedesk.controller;

import com.dinedesk.Main;
import com.dinedesk.database.Database;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDate;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class NewOrderController {

    @FXML private VBox sidebar;

    @FXML private TextField searchField;
    @FXML private ComboBox<String> categoryCombo;

    @FXML private ListView<MenuEntry> menuListView;
    @FXML private ListView<CartEntry> cartListView;

    @FXML private Label menuCountLabel;
    @FXML private Label cartCountLabel;
    @FXML private Label subtotalLabel;
    @FXML private Label totalLabel;

    private final ObservableList<MenuEntry> menuItems =
            FXCollections.observableArrayList();

    private final ObservableList<CartEntry> cartItems =
            FXCollections.observableArrayList();

    private final List<MenuEntry> allMenuItems = new ArrayList<>();

    private final NumberFormat currency =
            NumberFormat.getCurrencyInstance(new Locale("en", "IN"));

    private boolean sidebarCollapsed = false;

    @FXML
    private void initialize() {
        menuListView.setItems(menuItems);
        cartListView.setItems(cartItems);

        menuListView.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(MenuEntry item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.name + "\n"
                            + currency.format(item.price));
                }
            }
        });

        cartListView.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(CartEntry item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.name
                            + "   × " + item.quantity
                            + "\n" + currency.format(item.getLineTotal()));
                }
            }
        });

        categoryCombo.getItems().add("All Categories");
        loadCategories();
        categoryCombo.getSelectionModel().selectFirst();

        loadMenuItems();

        categoryCombo.setOnAction(event -> filterMenu());

        searchField.textProperty().addListener(
                (observable, oldValue, newValue) -> filterMenu());

        updateTotals();
    }

    private void loadCategories() {
        String sql = "SELECT name FROM categories ORDER BY id";

        try (Connection connection = Database.connect();
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(sql)) {

            while (result.next()) {
                categoryCombo.getItems().add(result.getString("name"));
            }

        } catch (Exception exception) {
            showError("Could not load categories", exception);
        }
    }

    private void loadMenuItems() {
        allMenuItems.clear();

        String sql = """
                SELECT m.id, m.name, m.price, m.category_id, c.name AS category_name
                FROM menu_items m
                JOIN categories c ON c.id = m.category_id
                ORDER BY c.id, m.name
                """;

        try (Connection connection = Database.connect();
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(sql)) {

            while (result.next()) {
                allMenuItems.add(new MenuEntry(
                        result.getInt("id"),
                        result.getInt("category_id"),
                        result.getString("name"),
                        result.getDouble("price"),
                        result.getString("category_name")
                ));
            }

            filterMenu();

        } catch (Exception exception) {
            showError("Could not load menu items", exception);
        }
    }

    private void filterMenu() {
        String search = searchField.getText() == null
                ? ""
                : searchField.getText().trim().toLowerCase(Locale.ROOT);

        String category = categoryCombo.getValue();

        menuItems.setAll(allMenuItems.stream()
                .filter(item -> item.name.toLowerCase(Locale.ROOT)
                        .contains(search))
                .filter(item -> category == null
                        || category.equals("All Categories")
                        || item.categoryName.equals(category))
                .toList());

        menuCountLabel.setText(menuItems.size() + " items");
    }

    @FXML
    private void addSelectedItem() {
        MenuEntry selected = menuListView.getSelectionModel()
                .getSelectedItem();

        if (selected == null) {
            showInfo("Select a menu item first.");
            return;
        }

        for (CartEntry entry : cartItems) {
            if (entry.menuItemId == selected.id) {
                entry.quantity++;
                cartListView.refresh();
                updateTotals();
                return;
            }
        }

        cartItems.add(new CartEntry(
                selected.id,
                selected.name,
                selected.price,
                1
        ));

        updateTotals();
    }

    @FXML
    private void increaseSelectedQuantity() {
        CartEntry selected = cartListView.getSelectionModel()
                .getSelectedItem();

        if (selected == null) {
            showInfo("Select an item in the current order.");
            return;
        }

        selected.quantity++;
        cartListView.refresh();
        updateTotals();
    }

    @FXML
    private void decreaseSelectedQuantity() {
        CartEntry selected = cartListView.getSelectionModel()
                .getSelectedItem();

        if (selected == null) {
            showInfo("Select an item in the current order.");
            return;
        }

        if (selected.quantity > 1) {
            selected.quantity--;
        } else {
            cartItems.remove(selected);
        }

        cartListView.refresh();
        updateTotals();
    }

    @FXML
    private void removeSelectedItem() {
        CartEntry selected = cartListView.getSelectionModel()
                .getSelectedItem();

        if (selected != null) {
            cartItems.remove(selected);
            updateTotals();
        } else {
            showInfo("Select an item to remove.");
        }
    }

    @FXML
    private void clearOrder() {
        if (cartItems.isEmpty()) {
            return;
        }

        Alert confirmation = new Alert(
                Alert.AlertType.CONFIRMATION,
                "Clear all items from the current order?",
                javafx.scene.control.ButtonType.YES,
                javafx.scene.control.ButtonType.NO
        );

        confirmation.setHeaderText("Clear current order");

        if (confirmation.showAndWait().orElse(
                javafx.scene.control.ButtonType.NO)
                == javafx.scene.control.ButtonType.YES) {

            cartItems.clear();
            updateTotals();
        }
    }

    private void updateTotals() {
        double total = cartItems.stream()
                .mapToDouble(CartEntry::getLineTotal)
                .sum();

        int quantity = cartItems.stream()
                .mapToInt(item -> item.quantity)
                .sum();

        subtotalLabel.setText(currency.format(total));
        totalLabel.setText(currency.format(total));
        cartCountLabel.setText(quantity + " items");
    }

    @FXML
    private void saveOrder() {
        if (cartItems.isEmpty()) {
            showInfo("Add at least one item before saving the order.");
            return;
        }

        String date = LocalDate.now().toString();

        String insertOrder = """
                INSERT INTO orders(order_date, token_number, total_bill)
                VALUES (?, ?, ?)
                """;

        String insertItem = """
                INSERT INTO order_items(order_id, menu_item_id, quantity, price)
                VALUES (?, ?, ?, ?)
                """;

        double total = cartItems.stream()
                .mapToDouble(CartEntry::getLineTotal)
                .sum();

        try (Connection connection = Database.connect()) {
            connection.setAutoCommit(false);

            try {
                int token;

                try (PreparedStatement tokenQuery = connection.prepareStatement(
                        "SELECT COALESCE(MAX(token_number), 0) + 1 "
                                + "FROM orders WHERE order_date = ?")) {

                    tokenQuery.setString(1, date);

                    try (ResultSet result = tokenQuery.executeQuery()) {
                        result.next();
                        token = result.getInt(1);
                    }
                }

                int orderId;

                try (PreparedStatement orderStatement =
                             connection.prepareStatement(
                                     insertOrder,
                                     Statement.RETURN_GENERATED_KEYS)) {

                    orderStatement.setString(1, date);
                    orderStatement.setInt(2, token);
                    orderStatement.setDouble(3, total);
                    orderStatement.executeUpdate();

                    try (ResultSet keys = orderStatement.getGeneratedKeys()) {
                        if (!keys.next()) {
                            throw new java.sql.SQLException(
                                    "Could not retrieve the saved order ID.");
                        }

                        orderId = keys.getInt(1);
                    }
                }

                try (PreparedStatement itemStatement =
                             connection.prepareStatement(insertItem)) {

                    for (CartEntry item : cartItems) {
                        itemStatement.setInt(1, orderId);
                        itemStatement.setInt(2, item.menuItemId);
                        itemStatement.setInt(3, item.quantity);
                        itemStatement.setDouble(4, item.price);
                        itemStatement.addBatch();
                    }

                    itemStatement.executeBatch();
                }

                connection.commit();

                showInfo("Order saved successfully!\n\n"
                        + "Token number: " + token + "\n"
                        + "Total: " + currency.format(total));

                cartItems.clear();
                updateTotals();

            } catch (Exception exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(true);
            }

        } catch (Exception exception) {
            showError("Could not save order", exception);
        }
    }

    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("DineDesk");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showError(String title, Exception exception) {
        exception.printStackTrace();

        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("DineDesk");
        alert.setHeaderText(title);
        alert.setContentText(exception.getMessage() == null
                ? "An unexpected error occurred."
                : exception.getMessage());
        alert.showAndWait();
    }

    @FXML
    private void toggleSidebar() {
        sidebarCollapsed = !sidebarCollapsed;

        double width = sidebarCollapsed ? 76 : 245;
        sidebar.setPrefWidth(width);
        sidebar.setMinWidth(width);
        sidebar.setMaxWidth(width);
    }

    @FXML
    private void openDashboard() {
        Main.show("dashboard.fxml", "Dashboard");
    }

    @FXML
    private void openNewOrder() {
        Main.show("NewOrder.fxml", "New Order");
    }

    @FXML
    private void openOrders() {
        Main.show("orders.fxml", "Orders");
    }

    @FXML
    private void openMenu() {
        Main.show("menu.fxml", "Menu Management");
    }

    @FXML
    private void openReports() {
        Main.show("reports.fxml", "Reports");
    }

    @FXML
    private void openSettings() {
        Main.show("settings.fxml", "Settings");
    }

    @FXML
    private void openBackup() {
        Main.show("backup.fxml", "Backup & Restore");
    }

    private static class MenuEntry {
        private final int id;
        private final int categoryId;
        private final String name;
        private final double price;
        private final String categoryName;

        private MenuEntry(int id, int categoryId, String name,
                          double price, String categoryName) {
            this.id = id;
            this.categoryId = categoryId;
            this.name = name;
            this.price = price;
            this.categoryName = categoryName;
        }
    }

    private static class CartEntry {
        private final int menuItemId;
        private final String name;
        private final double price;
        private int quantity;

        private CartEntry(int menuItemId, String name,
                          double price, int quantity) {
            this.menuItemId = menuItemId;
            this.name = name;
            this.price = price;
            this.quantity = quantity;
        }

        private double getLineTotal() {
            return price * quantity;
        }
    }
}
