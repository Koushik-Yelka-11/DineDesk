module com.dinedesk {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.dinedesk to javafx.fxml;
    exports com.dinedesk;
}