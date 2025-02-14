import javafx.application.Application;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Orientation;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.image.WritableImage;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.Arrays;

@SuppressWarnings("unused")
public class Main extends Application {
    // Rotation angles in radians and the sphere color
    private double rotationX = 0;
    private double rotationY = 0;
    private Color sphereColor = Color.WHITE;
    private double lastX = 0, lastY = 0;

    private Canvas canvas;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("3D Renderer");
        primaryStage.setHeight(800);
        primaryStage.setWidth(1200);

        // Create a canvas for rendering
        canvas = new Canvas(800, 600);
        addMouseHandlers();

        // Layout: use BorderPane
        BorderPane root = new BorderPane();
        root.setCenter(canvas);

        // Bottom control panel: heading slider and RGB sliders
        Slider headingSlider = new Slider(0, 360, 180);
        headingSlider.setMaxWidth(400);
        headingSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            rotationX = Math.toRadians(newVal.doubleValue());
            draw();
        });

        // Red, Green, Blue sliders
        Slider redSlider = createColorSlider(255);
        Slider greenSlider = createColorSlider(255);
        Slider blueSlider = createColorSlider(255);

        ChangeListener<Number> colorListener = (obs, oldVal, newVal) -> {
            sphereColor = Color.rgb(
                    (int) redSlider.getValue(),
                    (int) greenSlider.getValue(),
                    (int) blueSlider.getValue()
            );
            draw();
        };
        redSlider.valueProperty().addListener(colorListener);
        greenSlider.valueProperty().addListener(colorListener);
        blueSlider.valueProperty().addListener(colorListener);

        HBox colorBox = new HBox(10, new Label("Red"), redSlider,
                new Label("Green"), greenSlider,
                new Label("Blue"), blueSlider);
        VBox bottomBox = new VBox(10, headingSlider, colorBox);
        root.setBottom(bottomBox);

        // Left control panel: pitch slider (vertical)
        Slider pitchSlider = new Slider(-90, 90, 0);
        pitchSlider.setOrientation(Orientation.VERTICAL);
        pitchSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            rotationY = Math.toRadians(newVal.doubleValue());
            draw();
        });
        VBox leftBox = new VBox(new Label("Pitch"), pitchSlider);
        root.setLeft(leftBox);

        Scene scene = new Scene(root, 800, 600);
        primaryStage.setScene(scene);
        primaryStage.show();

        draw(); // initial render
    }

    private Slider createColorSlider(int initial) {
        Slider slider = new Slider(0, 255, initial);
        slider.setShowTickLabels(true);
        slider.setShowTickMarks(true);
        slider.setMajorTickUnit(85);
        slider.setMinorTickCount(4);
        return slider;
    }

    private void addMouseHandlers() {
        canvas.setOnMousePressed((MouseEvent e) -> {
            lastX = e.getX();
            lastY = e.getY();
        });
        canvas.setOnMouseDragged((MouseEvent e) -> {
            double deltaX = - (e.getX() - lastX);
            double deltaY = - (e.getY() - lastY);
            rotationX -= Math.toRadians(deltaX * 0.5);  // adjust sensitivity if needed
            rotationY -= Math.toRadians(deltaY * 0.5);
            // constrain pitch rotation
            rotationY = Math.max(-Math.PI / 2, Math.min(Math.PI / 2, rotationY));
            lastX = e.getX();
            lastY = e.getY();
            draw();
        });
    }

    private void draw() {
        int width = (int) canvas.getWidth();
        int height = (int) canvas.getHeight();

        WritableImage image = new WritableImage(width, height);
        PixelWriter pw = image.getPixelWriter();
        // Clear image to black
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                pw.setColor(x, y, Color.BLACK);
            }
        }

        double[] zBuffer = new double[width * height];
        Arrays.fill(zBuffer, Double.NEGATIVE_INFINITY);

        // Create initial triangles (a tetrahedron) and inflate to approximate a sphere
        ArrayList<Triangle> tris = new ArrayList<>();
        tris.add(new Triangle(new Vertex(100, 100, 100), new Vertex(-100, -100, 100), new Vertex(-100, 100, -100), sphereColor));
        tris.add(new Triangle(new Vertex(100, 100, 100), new Vertex(-100, -100, 100), new Vertex(100, -100, -100), sphereColor));
        tris.add(new Triangle(new Vertex(-100, 100, -100), new Vertex(100, -100, -100), new Vertex(100, 100, 100), sphereColor));
        tris.add(new Triangle(new Vertex(-100, 100, -100), new Vertex(100, -100, -100), new Vertex(-100, -100, 100), sphereColor));

        final int INFLATION_LEVEL = 4;
        for (int i = 0; i < INFLATION_LEVEL; i++) {
            tris = inflate(tris);
        }

        // Build transformation matrices for heading and pitch rotations
        Matrix3 headingTransform = new Matrix3(new double[]{
                Math.cos(rotationX), 0, Math.sin(rotationX),
                0, 1, 0,
                -Math.sin(rotationX), 0, Math.cos(rotationX)
        });

        Matrix3 pitchTransform = new Matrix3(new double[]{
                1, 0, 0,
                0, Math.cos(rotationY), Math.sin(rotationY),
                0, -Math.sin(rotationY), Math.cos(rotationY)
        });

        Matrix3 transform = headingTransform.multiply(pitchTransform);

        // Render each triangle
        for (Triangle triangle : tris) {
            // Transform and translate vertices
            Vertex v1 = transform.transform(triangle.v1);
            Vertex v2 = transform.transform(triangle.v2);
            Vertex v3 = transform.transform(triangle.v3);
            v1.x += width / 2.0;  v1.y += height / 2.0;
            v2.x += width / 2.0;  v2.y += height / 2.0;
            v3.x += width / 2.0;  v3.y += height / 2.0;

            // Calculate the normal of the triangle
            Vertex ab = new Vertex(v2.x - v1.x, v2.y - v1.y, v2.z - v1.z);
            Vertex ac = new Vertex(v3.x - v1.x, v3.y - v1.y, v3.z - v1.z);
            Vertex norm = new Vertex(
                    ab.y * ac.z - ab.z * ac.y,
                    ab.z * ac.x - ab.x * ac.z,
                    ab.x * ac.y - ab.y * ac.x
            );
            double normLength = Math.sqrt(norm.x * norm.x + norm.y * norm.y + norm.z * norm.z);
            if (normLength != 0) {
                norm.x /= normLength; norm.y /= normLength; norm.z /= normLength;
            }
            double angleCos = Math.abs(norm.z);

            int minX = (int) Math.max(0, Math.ceil(Math.min(v1.x, Math.min(v2.x, v3.x))));
            int maxX = (int) Math.min(width - 1, Math.floor(Math.max(v1.x, Math.max(v2.x, v3.x))));
            int minY = (int) Math.max(0, Math.ceil(Math.min(v1.y, Math.min(v2.y, v3.y))));
            int maxY = (int) Math.min(height - 1, Math.floor(Math.max(v1.y, Math.max(v2.y, v3.y))));

            double triangleArea = (v1.y - v3.y) * (v2.x - v3.x) + (v2.y - v3.y) * (v3.x - v1.x);

            // Iterate over the bounding box and fill in the triangle using barycentric coordinates
            for (int y = minY; y <= maxY; y++) {
                for (int x = minX; x <= maxX; x++) {
                    double b1 = ((y - v3.y) * (v2.x - v3.x) + (v2.y - v3.y) * (v3.x - x)) / triangleArea;
                    double b2 = ((y - v1.y) * (v3.x - v1.x) + (v3.y - v1.y) * (v1.x - x)) / triangleArea;
                    double b3 = ((y - v2.y) * (v1.x - v2.x) + (v1.y - v2.y) * (v2.x - x)) / triangleArea;

                    if (b1 >= 0 && b1 <= 1 && b2 >= 0 && b2 <= 1 && b3 >= 0 && b3 <= 1) {
                        double depth = b1 * v1.z + b2 * v2.z + b3 * v3.z;
                        int zIndex = y * width + x;
                        if (zBuffer[zIndex] < depth) {
                            pw.setColor(x, y, getShade(triangle.color, angleCos));
                            zBuffer[zIndex] = depth;
                        }
                    }
                }
            }
        }

        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.drawImage(image, 0, 0);
    }

    // Gamma-corrected shading function (using JavaFX Color)
    public static Color getShade(Color color, double shade) {
        double redLinear = Math.pow(color.getRed(), 2.4) * shade;
        double greenLinear = Math.pow(color.getGreen(), 2.4) * shade;
        double blueLinear = Math.pow(color.getBlue(), 2.4) * shade;

        double red = Math.pow(redLinear, 1.0/2.4);
        double green = Math.pow(greenLinear, 1.0/2.4);
        double blue = Math.pow(blueLinear, 1.0/2.4);

        return new Color(clamp(red), clamp(green), clamp(blue), 1.0);
    }

    private static double clamp(double value) {
        return Math.max(0, Math.min(1, value));
    }

    // Inflate method (subdivides triangles and normalizes vertices)
    public static ArrayList<Triangle> inflate(ArrayList<Triangle> tris) {
        ArrayList<Triangle> result = new ArrayList<>();

        for (Triangle t : tris) {
            Vertex m1 = new Vertex((t.v1.x + t.v2.x) / 2, (t.v1.y + t.v2.y) / 2, (t.v1.z + t.v2.z) / 2);
            Vertex m2 = new Vertex((t.v2.x + t.v3.x) / 2, (t.v2.y + t.v3.y) / 2, (t.v2.z + t.v3.z) / 2);
            Vertex m3 = new Vertex((t.v1.x + t.v3.x) / 2, (t.v1.y + t.v3.y) / 2, (t.v1.z + t.v3.z) / 2);

            result.add(new Triangle(t.v1, m1, m3, t.color));
            result.add(new Triangle(t.v2, m1, m2, t.color));
            result.add(new Triangle(t.v3, m2, m3, t.color));
            result.add(new Triangle(m1, m2, m3, t.color));
        }

        // Normalize vertices so they lie on a sphere
        for (Triangle t : result) {
            for (Vertex v : new Vertex[]{t.v1, t.v2, t.v3}) {
                double l = Math.sqrt(v.x * v.x + v.y * v.y + v.z * v.z) / Math.sqrt(30000);
                v.x /= l;
                v.y /= l;
                v.z /= l;
            }
        }
        return result;
    }

    // --- Helper Classes ---

    public static class Vertex {
        double x, y, z;
        public Vertex(double x, double y, double z) {
            this.x = x; this.y = y; this.z = z;
        }
    }

    public static class Triangle {
        Vertex v1, v2, v3;
        Color color;
        public Triangle(Vertex v1, Vertex v2, Vertex v3, Color color) {
            this.v1 = v1; this.v2 = v2; this.v3 = v3;
            this.color = color;
        }
    }

    public static class Matrix3 {
        double[] m; // 3x3 matrix stored in row-major order
        public Matrix3(double[] m) {
            if(m.length != 9) throw new IllegalArgumentException("Matrix must have 9 elements");
            this.m = m;
        }

        public Matrix3 multiply(Matrix3 other) {
            double[] result = new double[9];
            for (int i = 0; i < 3; i++) {
                for (int j = 0; j < 3; j++) {
                    result[i * 3 + j] = 0;
                    for (int k = 0; k < 3; k++) {
                        result[i * 3 + j] += this.m[i * 3 + k] * other.m[k * 3 + j];
                    }
                }
            }
            return new Matrix3(result);
        }

        public Vertex transform(Vertex v) {
            double x = m[0] * v.x + m[1] * v.y + m[2] * v.z;
            double y = m[3] * v.x + m[4] * v.y + m[5] * v.z;
            double z = m[6] * v.x + m[7] * v.y + m[8] * v.z;
            return new Vertex(x, y, z);
        }
    }
}
