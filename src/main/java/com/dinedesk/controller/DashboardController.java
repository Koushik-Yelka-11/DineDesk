package com.dinedesk.controller;

import com.dinedesk.database.Database;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import java.sql.*;
import java.time.LocalDate;

public class DashboardController extends NavController {

    @FXML
    private Label salesLabel, ordersLabel, dateLabel;

    @FXML
    private void initialize() {
        dateLabel.setText(LocalDate.now().toString());

        try (
                Connection c = Database.connect();
                PreparedStatement p = c.prepareStatement(
                        "SELECT COALESCE(SUM(total_bill),0),COUNT(*) FROM orders WHERE order_date=?"
                )
        ) {
            p.setString(1, LocalDate.now().toString());

            try (ResultSet r = p.executeQuery()) {
                if (r.next()) {
                    salesLabel.setText(String.format("₹%,.2f", r.getDouble(1)));
                    ordersLabel.setText("" + r.getInt(2));
                }
            }
        } catch (Exception e) {
            salesLabel.setText("₹0.00");
            ordersLabel.setText("0");
        }
    }
}
