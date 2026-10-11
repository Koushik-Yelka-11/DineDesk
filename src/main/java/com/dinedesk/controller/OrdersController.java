
package com.dinedesk.controller;

import com.dinedesk.database.Database;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import java.sql.*;

public class OrdersController extends NavController {

    @FXML
    private TextField searchField;

    @FXML
    private TableView<OrderRow> ordersTable;

    @FXML
    private TableColumn<OrderRow, Number> idColumn, tokenColumn;

    @FXML
    private TableColumn<OrderRow, String> dateColumn;

    @FXML
    private TableColumn<OrderRow, Number> totalColumn;

    public static class OrderRow {

        public final javafx.beans.property.IntegerProperty id =
                new javafx.beans.property.SimpleIntegerProperty();

        public final javafx.beans.property.IntegerProperty token =
                new javafx.beans.property.SimpleIntegerProperty();

        public final javafx.beans.property.StringProperty date =
                new javafx.beans.property.SimpleStringProperty();

        public final javafx.beans.property.DoubleProperty total =
                new javafx.beans.property.SimpleDoubleProperty();

        OrderRow(int i, String d, int t, double v) {
            id.set(i);
            date.set(d);
            token.set(t);
            total.set(v);
        }
    }

    @FXML
    private void initialize() {
        idColumn.setCellValueFactory(x -> x.getValue().id);
        dateColumn.setCellValueFactory(x -> x.getValue().date);
        tokenColumn.setCellValueFactory(x -> x.getValue().token);
        totalColumn.setCellValueFactory(x -> x.getValue().total);

        load("");

        searchField.textProperty().addListener(
                (o, a, b) -> load(b)
        );
    }

    private void load(String q) {
        var rows = FXCollections.<OrderRow>observableArrayList();

        try (
                Connection c = Database.connect();
                PreparedStatement p = c.prepareStatement(
                        "SELECT id, order_date, token_number, total_bill " +
                                "FROM orders " +
                                "WHERE CAST(token_number AS TEXT) LIKE ? " +
                                "OR order_date LIKE ? " +
                                "ORDER BY id DESC"
                )
        ) {
            p.setString(1, "%" + q + "%");
            p.setString(2, "%" + q + "%");

            try (ResultSet r = p.executeQuery()) {
                while (r.next()) {
                    rows.add(
                            new OrderRow(
                                    r.getInt(1),
                                    r.getString(2),
                                    r.getInt(3),
                                    r.getDouble(4)
                            )
                    );
                }
            }

            ordersTable.setItems(rows);

        } catch (Exception e) {
            new Alert(
                    Alert.AlertType.ERROR,
                    e.getMessage()
            ).showAndWait();
        }
    }
}
