module ImageWizard {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.desktop;
    requires javafx.graphics;
    requires javafx.swing;


    opens controller to javafx.fxml;
    exports Launcher;
    opens view to javafx.fxml;
}