module com.dinedesk {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;

    opens com.dinedesk to javafx.fxml;
    exports com.dinedesk;
}