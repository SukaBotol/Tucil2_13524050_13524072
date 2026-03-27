package stima;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

public class viewer3D extends Application {

    // private final double[][] vertices = {
    // { -1, -1, -1 },
    // { 1, -1, -1 },
    // { 1, 1, -1 },
    // { -1, 1, -1 },
    // { -1, -1, 1 },
    // { 1, -1, 1 },
    // { 1, 1, 1 },
    // { -1, 1, 1 }
    // };

    // private final int[][] edges = {
    // { 0, 1 }, { 1, 2 }, { 2, 3 }, { 3, 0 },
    // { 4, 5 }, { 5, 6 }, { 6, 7 }, { 7, 4 },
    // { 0, 4 }, { 1, 5 }, { 2, 6 }, { 3, 7 }
    // };

    private double[][] vertices = new double[0][3];
    private final List<int[]> edges = new ArrayList<>();

    private double angleY = 0.0;
    private double scale = 900.0;
    private final double zoomSpeed = 25;
    private final Set<KeyCode> keys = new HashSet<>();

    @Override
    public void start(Stage stage) {
        Canvas canvas = new Canvas(1000, 700);
        GraphicsContext gc = canvas.getGraphicsContext2D();

        // load precompiled voxel
        String filename = "cowvox";
        loadObjVoxel("test/" + filename + ".obj");

        Scene scene = new Scene(new StackPane(canvas));
        scene.setOnKeyPressed(e -> keys.add(e.getCode()));
        scene.setOnKeyReleased(e -> keys.remove(e.getCode()));

        stage.setScene(scene);
        stage.show();

        canvas.requestFocus();

        // throttle rendering
        new AnimationTimer() {
            @Override
            public void handle(long now) {
                update();
                render(gc, canvas.getWidth(), canvas.getHeight());
            }
        }.start();
    }

    private void loadObjVoxel(String path) {
        List<double[]> vertex = new ArrayList<>();
        edges.clear();

        File file = new File(path);

        // create vertices
        try (Scanner read = new Scanner(file)) {
            while (read.hasNextLine()) {
                String data = read.nextLine().trim();
                if (data.isEmpty() || data.startsWith("#")) {
                    continue;
                }

                String[] splitData = data.split("\\s+");
                if (splitData[0].equals("v")) {
                    double x = Double.parseDouble(splitData[1]);
                    double y = Double.parseDouble(splitData[2]);
                    double z = Double.parseDouble(splitData[3]);
                    vertex.add(new double[] { x, y, z });
                }
            }
        } catch (Exception e) {
            System.err.println("failed");
        }

        vertices = vertex.toArray(new double[0][0]);

        // cube edges manual
        for (int i = 0; i + 7 < vertices.length; i += 8) {
            // 12 edges
            edges.add(new int[] { i + 0, i + 1 });
            edges.add(new int[] { i + 1, i + 2 });
            edges.add(new int[] { i + 2, i + 3 });
            edges.add(new int[] { i + 3, i + 0 });
            edges.add(new int[] { i + 4, i + 5 });
            edges.add(new int[] { i + 5, i + 6 });
            edges.add(new int[] { i + 6, i + 7 });
            edges.add(new int[] { i + 7, i + 4 });
            edges.add(new int[] { i + 0, i + 4 });
            edges.add(new int[] { i + 1, i + 5 });
            edges.add(new int[] { i + 2, i + 6 });
            edges.add(new int[] { i + 3, i + 7 });
        }

        // System.out.println("Loaded vertices: " + vertices.length + ", edges: " +
        // edges.size());
    }

    private void update() {
        // its game dev wow

        // rotate
        if (keys.contains(KeyCode.A))
            angleY -= 0.03;
        if (keys.contains(KeyCode.D))
            angleY += 0.03;

        // zoom
        if (keys.contains(KeyCode.W))
            scale += zoomSpeed;
        if (keys.contains(KeyCode.S))
            scale -= zoomSpeed;

        // if (focalLength < 10)
        // focalLength = 10;
    }

    // ref
    // https://www.iditect.com/faq/java/how-to-convert-a-3d-point-into-2d-perspective-projection-in-java.html

    private void render(GraphicsContext gc, double w, double h) {
        // clear screen
        gc.setFill(Color.BLACK);
        gc.fillRect(0, 0, w, h);

        // projection from 3D to 2D
        double[][] projections = new double[vertices.length][2];
        double xOffset = w / 2.0;
        double yOffset = h / 2.0;
        //double zOffset = 4.0;

        for (int i = 0; i < vertices.length; i++) {
            double x3D = vertices[i][0];
            double y3D = vertices[i][1];
            double z3D = vertices[i][2];

            // get rotated coords
            double rotatedX = x3D * Math.cos(angleY) - z3D * Math.sin(angleY);
            // double rotatedZ = x3D * Math.sin(angleY) + z3D * Math.cos(angleY);

            // just realize we can just ignore z rot and it looks fine 
            // well its ortoghraphic now
            double x2D = rotatedX * scale + xOffset;
            double y2D = -y3D * scale + yOffset;

            projections[i][0] = x2D;
            projections[i][1] = y2D;
        }

        // draw
        gc.setStroke(Color.BLUE);
        gc.setLineWidth(1);
        for (int[] e : edges) {
            int a = e[0];
            int b = e[1];
            if (a < 0 || b < 0 || a >= projections.length || b >= projections.length) {
                continue;
            }
            gc.strokeLine(projections[a][0], projections[a][1], projections[b][0], projections[b][1]);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}