
module com.dinedesk {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires java.desktop;

    opens com.dinedesk to javafx.fxml;
    opens com.dinedesk.controller to javafx.fxml;

    exports com.dinedesk;
    exports com.dinedesk.controller;
}
