package stima;

import java.io.File;

import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class viewer {
    @FXML private Button chooseButton;
    @FXML private Button buildButton;
    @FXML private Label selectedFileLabel;
    @FXML private TextField depthField;
    @FXML private TextField outputField;
    @FXML private TextArea outputArea;

    private File selectedFile;

    @FXML
    private void initialize() {
        selectedFileLabel.setText("No .obj file selected");
    }

    @FXML
    private void chooseObjFile() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select an OBJ file");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("OBJ files", "*.obj"));

        File dataDir = new File("data");
        if (dataDir.isDirectory()) {
            chooser.setInitialDirectory(dataDir);
        }

        Stage stage = (Stage) chooseButton.getScene().getWindow();
        File file = chooser.showOpenDialog(stage);
        if (file == null) {
            return;
        }

        selectedFile = file;
        selectedFileLabel.setText("Selected: " + file.getAbsolutePath());
    }

    @FXML
    private void buildOctree() {
        if (selectedFile == null) {
            outputArea.setText("Choose an .obj file");
            return;
        }

        final int depth;
        try {
            depth = Integer.parseInt(depthField.getText().trim());
        } catch (NumberFormatException exception) {
            return;
        }

        if (depth < 0) {
            outputArea.setText("Depth must >= 0");
            return;
        }

        final String outputName = outputField.getText().trim();
        if (outputName.isEmpty()) {
            outputArea.setText("No output file");
            return;
        }

        Task<String> buildTask = new Task<>() {
            @Override
            protected String call() throws Exception {
                return test.objToVoxeL(selectedFile, depth, outputName);
            }
        };

        buildTask.setOnRunning(event -> {
            chooseButton.setDisable(true);
            buildButton.setDisable(true);
            outputArea.setText("Building octree...");
        });

        buildTask.setOnSucceeded(event -> {
            chooseButton.setDisable(false);
            buildButton.setDisable(false);
            outputArea.setText(buildTask.getValue());
        });

        buildTask.setOnFailed(event -> {
            chooseButton.setDisable(false);
            buildButton.setDisable(false);
            Throwable error = buildTask.getException();
            outputArea.setText("Failed: " + (error == null ? "error" : error.getMessage()));
        });

        Thread worker = new Thread(buildTask, "octree-builder");
        worker.setDaemon(true);
        worker.start();
    }
}
