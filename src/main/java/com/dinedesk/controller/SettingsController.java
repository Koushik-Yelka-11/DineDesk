
package com.dinedesk.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import com.dinedesk.database.Database;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;

import java.sql.Connection;
import java.sql.Statement;
import java.sql.SQLException;
import java.util.Optional;
public class SettingsController extends NavController {

    @FXML
    private void showInfo() {
        new Alert(
                Alert.AlertType.INFORMATION,
                "DineDesk — offline restaurant counter application. Data is stored in dinedesk.db."
        ).showAndWait();
    }
    @FXML
    private void resetOrders() {
        Alert confirmation = new Alert(Alert.AlertType.WARNING);

        confirmation.setTitle("Reset Orders");
        confirmation.setHeaderText("Are you sure you want to reset all orders?");
        confirmation.setContentText(
                "All saved orders and their order items will be permanently deleted.\n\n"
                        + "Menu categories and menu items will remain unchanged."
        );

        ButtonType resetButton = new ButtonType(
                "Reset All Orders",
                ButtonBar.ButtonData.OK_DONE
        );

        ButtonType cancelButton = new ButtonType(
                "Cancel",
                ButtonBar.ButtonData.CANCEL_CLOSE
        );

        confirmation.getButtonTypes().setAll(resetButton, cancelButton);

        Optional<ButtonType> result = confirmation.showAndWait();

        if (result.isEmpty() || result.get() != resetButton) {
            return;
        }
        try (Connection connection = Database.connect()) {
            connection.setAutoCommit(false);

            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate("DELETE FROM order_items");
                statement.executeUpdate("DELETE FROM orders");

                // Reset the SQLite-generated Order ID sequence.
                statement.executeUpdate(
                        "DELETE FROM sqlite_sequence WHERE name = 'orders'"
                );

                connection.commit();

            } catch (SQLException e) {
                connection.rollback();
                throw e;

            } finally {
                connection.setAutoCommit(true);
            }

            Alert success = new Alert(Alert.AlertType.INFORMATION);
            success.setTitle("Reset Complete");
            success.setHeaderText("Orders reset successfully");
            success.setContentText(
                    "All orders have been deleted.\n"
                            + "The next Order ID will start from 1.\n"
                            + "Your menu remains unchanged."
            );
            success.showAndWait();

        } catch (SQLException e) {
            e.printStackTrace();

            Alert error = new Alert(Alert.AlertType.ERROR);
            error.setTitle("Reset Failed");
            error.setHeaderText("Could not reset orders");
            error.setContentText(e.getMessage());
            error.showAndWait();
        }
    }
}
