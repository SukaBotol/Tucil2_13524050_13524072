package stima;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class app extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        Scene scene = new Scene(FXMLLoader.load(getClass().getResource("/main.fxml")), 780, 520);
        stage.setScene(scene);
        stage.show();

        viewer3D v3d = new viewer3D();
        Stage viewer3dWindow = v3d.openWindow();
        viewer3dWindow.setX(0);
        viewer3dWindow.setY(0);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
