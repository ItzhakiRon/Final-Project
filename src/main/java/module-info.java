module com.example.rongame {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.example.rongame to javafx.fxml;
    exports com.example.rongame;
}