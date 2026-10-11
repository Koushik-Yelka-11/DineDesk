
package com.dinedesk.controller;

import com.dinedesk.Main;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.layout.BorderPane;

public class NavController {

    @FXML
    private BorderPane root;

    @FXML
    private Node sidebar;

    private boolean expanded = true;

    @FXML
    private void toggleSidebar() {
        if (sidebar == null) return;

        expanded = !expanded;
        sidebar.setVisible(expanded);
        sidebar.setManaged(expanded);
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
}
