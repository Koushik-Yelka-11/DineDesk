
package com.dinedesk.controller;

import com.dinedesk.database.Database;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import java.sql.*;

public class MenuController extends NavController {

    @FXML
    private TableView<MenuRow> menuTable;

    @FXML
    private TableColumn<MenuRow, String> nameColumn, categoryColumn;

    @FXML
    private TableColumn<MenuRow, Number> priceColumn;

    @FXML
    private TextField nameField, priceField;

    @FXML
    private ComboBox<String> categoryCombo;

    public static class MenuRow {

        public final javafx.beans.property.IntegerProperty id =
                new javafx.beans.property.SimpleIntegerProperty();

        public final javafx.beans.property.IntegerProperty catId =
                new javafx.beans.property.SimpleIntegerProperty();

        public final javafx.beans.property.StringProperty name =
                new javafx.beans.property.SimpleStringProperty();

        public final javafx.beans.property.StringProperty category =
                new javafx.beans.property.SimpleStringProperty();

        public final javafx.beans.property.DoubleProperty price =
                new javafx.beans.property.SimpleDoubleProperty();

        MenuRow(int i, int ci, String n, String c, double p) {
            id.set(i);
            catId.set(ci);
            name.set(n);
            category.set(c);
            price.set(p);
        }
    }

    @FXML
    private void initialize() {
        nameColumn.setCellValueFactory(x -> x.getValue().name);
        categoryColumn.setCellValueFactory(x -> x.getValue().category);
        priceColumn.setCellValueFactory(x -> x.getValue().price);

        try (
                Connection c = Database.connect();
                Statement s = c.createStatement();
                ResultSet r = s.executeQuery(
                        "SELECT name FROM categories ORDER BY id"
                )
        ) {
            while (r.next()) {
                categoryCombo.getItems().add(r.getString(1));
            }
        } catch (Exception e) {
            error(e);
        }

        menuTable.getSelectionModel().selectedItemProperty().addListener(
                (o, a, b) -> {
                    if (b != null) {
                        nameField.setText(b.name.get());
                        priceField.setText("" + b.price.get());
                        categoryCombo.setValue(b.category.get());
                    }
                }
        );

        load();
    }

    private void load() {
        var list = FXCollections.<MenuRow>observableArrayList();

        try (
                Connection c = Database.connect();
                Statement s = c.createStatement();
                ResultSet r = s.executeQuery(
                        "SELECT m.id, m.category_id, m.name, c.name, m.price " +
                                "FROM menu_items m " +
                                "JOIN categories c ON c.id = m.category_id " +
                                "ORDER BY m.name"
                )
        ) {
            while (r.next()) {
                list.add(
                        new MenuRow(
                                r.getInt(1),
                                r.getInt(2),
                                r.getString(3),
                                r.getString(4),
                                r.getDouble(5)
                        )
                );
            }

            menuTable.setItems(list);

        } catch (Exception e) {
            error(e);
        }
    }

    @FXML
    private void saveItem() {
        try {
            if (nameField.getText().isBlank()
                    || categoryCombo.getValue() == null
                    || Double.parseDouble(priceField.getText()) < 0) {
                throw new Exception(
                        "Enter valid item name, category and price."
                );
            }

            try (
                    Connection c = Database.connect();
                    PreparedStatement p = c.prepareStatement(
                            "INSERT INTO menu_items(category_id, name, price) " +
                                    "SELECT id, ?, ? FROM categories WHERE name = ?"
                    )
            ) {
                p.setString(1, nameField.getText().trim());
                p.setDouble(2, Double.parseDouble(priceField.getText()));
                p.setString(3, categoryCombo.getValue());
                p.executeUpdate();
            }

            clearFields();
            load();

        } catch (Exception e) {
            error(e);
        }
    }

    @FXML
    private void deleteItem() {
        MenuRow row = menuTable.getSelectionModel().getSelectedItem();

        if (row == null) return;

        try (
                Connection c = Database.connect();
                PreparedStatement p = c.prepareStatement(
                        "DELETE FROM menu_items WHERE id = ?"
                )
        ) {
            p.setInt(1, row.id.get());
            p.executeUpdate();

            load();
            clearFields();

        } catch (Exception e) {
            error(e);
        }
    }

    @FXML
    private void clearFields() {
        nameField.clear();
        priceField.clear();
        categoryCombo.getSelectionModel().clearSelection();
        menuTable.getSelectionModel().clearSelection();
    }

    private void error(Exception e) {
        new Alert(
                Alert.AlertType.ERROR,
                e.getMessage()
        ).showAndWait();
    }
}
