
import javafx.application.Application;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Orientation;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Slider;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.WritableImage;
import javafx.scene.image.PixelWriter;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Hauptklasse der JavaFX-Anwendung.
 * <p>
 * Diese Anwendung rendert entweder ein einzelnes Dreieck oder ein Quadrat (als zwei Dreiecke),
 * je nach Benutzerauswahl. Über Schieberegler und Mausinteraktionen können Benutzer die 
 * Drehung (Heading und Pitch) sowie die Farbe (RGB) der Form steuern.
 * </p>
 */
@SuppressWarnings("unused")
public class Main extends Application {

    // Rotationswinkel in Radiant:
    // rotationX entspricht der horizontalen Drehung (Heading)
    // rotationY entspricht der vertikalen Drehung (Pitch)
    private double rotationX = 0;
    private double rotationY = 0;
    // Farbe der Form; initial auf Weiß gesetzt
    private Color sphereColor = Color.WHITE;
    // Letzte bekannte Mausposition (wird für Drag & Drop genutzt)
    private double lastX = 0, lastY = 0;

    // Zeichenfläche (Canvas) zum Rendern der 3D-Form
    private Canvas canvas;

    /**
     * Enum, das die auswählbaren Formen definiert.
     */
    public enum ShapeType {
        TRIANGLE,
        SQUARE
    }

    // Globale Variable, die die aktuell ausgewählte Form speichert.
    private ShapeType currentShape = ShapeType.TRIANGLE;

    /**
     * Main-Methode: Startet die JavaFX-Anwendung.
     *
     * @param args Kommandozeilenargumente
     */
    public static void main(String[] args) {
        launch(args);
    }

    /**
     * Start-Methode der JavaFX-Anwendung.
     * Hier wird das Hauptfenster (Stage) konfiguriert, die Benutzeroberfläche aufgebaut
     * und das initiale Rendering durchgeführt.
     *
     * @param primaryStage Das Hauptfenster der Anwendung.
     */
    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("3D Renderer - Toggle Shape");
        primaryStage.setHeight(800);
        primaryStage.setWidth(1200);

        // Erstelle eine Canvas, die als Zeichenfläche dient
        canvas = new Canvas(800, 600);
        // Füge Maus-Event-Handler hinzu, um Interaktionen (Drehungen) zu ermöglichen
        addMouseHandlers();

        // Verwende ein BorderPane als Layout-Manager
        BorderPane root = new BorderPane();
        // Setze die Canvas in die Mitte des Layouts
        root.setCenter(canvas);

        // ======================================================
        // Toggle-Steuerung: Umschalten zwischen Dreieck und Quadrat
        // ======================================================
        RadioButton triangleButton = new RadioButton("Triangle");
        RadioButton squareButton = new RadioButton("Square");
        ToggleGroup shapeToggleGroup = new ToggleGroup();
        triangleButton.setToggleGroup(shapeToggleGroup);
        squareButton.setToggleGroup(shapeToggleGroup);
        triangleButton.setSelected(true); // Standardmäßig Dreieck ausgewählt

        // Event-Handler, der die Form ändert und das Rendering aktualisiert
        triangleButton.setOnAction(e -> {
            currentShape = ShapeType.TRIANGLE;
            draw();
        });
        squareButton.setOnAction(e -> {
            currentShape = ShapeType.SQUARE;
            draw();
        });
        HBox shapeToggleBox = new HBox(10, new Label("Shape:"), triangleButton, squareButton);
        root.setTop(shapeToggleBox);
        // ======================================================

        // Untere Steuerung: Heading-Slider und RGB-Farbschieberegler
        Slider headingSlider = new Slider(0, 360, 180);
        headingSlider.setMaxWidth(400);
        headingSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            rotationX = Math.toRadians(newVal.doubleValue());
            draw();
        });

        // Erstelle Slider für die Farbauswahl (Rot, Grün, Blau)
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

        // Linke Steuerung: Pitch-Slider (vertikal)
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

        draw(); // Initiales Rendering
    }

    /**
     * Hilfsmethode zur Erstellung eines Farbschiebereglers.
     *
     * @param initial Anfangswert des Sliders (z. B. 255 für volle Helligkeit)
     * @return Konfigurierter Slider für Farbwerte (0 bis 255)
     */
    private Slider createColorSlider(int initial) {
        Slider slider = new Slider(0, 255, initial);
        slider.setShowTickLabels(true);
        slider.setShowTickMarks(true);
        slider.setMajorTickUnit(85);
        slider.setMinorTickCount(4);
        return slider;
    }

    /**
     * Fügt der Canvas Maus-Event-Handler hinzu, um Interaktionen per Drag & Drop zu ermöglichen.
     * Beim Drücken der Maus wird die Startposition gespeichert, und beim Ziehen wird die Rotation
     * der Form anhand der Mausbewegung angepasst.
     */
    private void addMouseHandlers() {
        canvas.setOnMousePressed((MouseEvent e) -> {
            lastX = e.getX();
            lastY = e.getY();
        });
        canvas.setOnMouseDragged((MouseEvent e) -> {
            double deltaX = -(e.getX() - lastX);
            double deltaY = -(e.getY() - lastY);
            rotationX -= Math.toRadians(deltaX * 0.5);  // Sensitivität anpassen
            rotationY -= Math.toRadians(deltaY * 0.5);
            rotationY = Math.max(-Math.PI / 2, Math.min(Math.PI / 2, rotationY));
            lastX = e.getX();
            lastY = e.getY();
            draw();
        });
    }

    /**
     * Führt das Rendering der aktuellen 3D-Form durch.
     * <p>
     * Abhängig von der Benutzerauswahl (currentShape) wird entweder ein einzelnes Dreieck oder ein
     * Quadrat (aufgeteilt in zwei Dreiecke) erstellt. Anschließend werden die Transformationen (Rotation)
     * angewendet und das Objekt mittels eines Z-Buffers gerendert.
     * </p>
     */
    private void draw() {
        int width = (int) canvas.getWidth();
        int height = (int) canvas.getHeight();

        // Erstelle ein Bild, das als Render-Puffer dient
        WritableImage image = new WritableImage(width, height);
        PixelWriter pw = image.getPixelWriter();
        // Hintergrund auf Schwarz setzen
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                pw.setColor(x, y, Color.BLACK);
            }
        }

        // Initialisiere einen Z-Buffer zur Tiefenspeicherung
        double[] zBuffer = new double[width * height];
        Arrays.fill(zBuffer, Double.NEGATIVE_INFINITY);

        // Erzeuge die Form als Liste von Dreiecken abhängig von der Auswahl:
        // - TRIANGLE: Ein einzelnes Dreieck
        // - SQUARE: Ein Quadrat, das aus zwei Dreiecken besteht
        ArrayList<Triangle> tris = new ArrayList<>();
        if (currentShape == ShapeType.TRIANGLE) {
            // Erzeugt ein Dreieck mit den angegebenen Vertices
            tris.add(new Triangle(
                    new Vertex(0, -100, 0),
                    new Vertex(100, 100, 0),
                    new Vertex(-100, 100, 0),
                    sphereColor));
        } else if (currentShape == ShapeType.SQUARE) {
            // Erzeugt ein Quadrat, dargestellt als zwei Dreiecke
            Vertex topLeft = new Vertex(-100, 100, 0);
            Vertex topRight = new Vertex(100, 100, 0);
            Vertex bottomRight = new Vertex(100, -100, 0);
            Vertex bottomLeft = new Vertex(-100, -100, 0);
            tris.add(new Triangle(topLeft, topRight, bottomRight, sphereColor));
            tris.add(new Triangle(topLeft, bottomRight, bottomLeft, sphereColor));
        }

        // Transformation: Erzeuge Rotationsmatrizen für Heading (Y-Achse) und Pitch (X-Achse)
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

        // Transformation und Rendering der Dreiecke:
        for (Triangle triangle : tris) {
            // Wende die Transformation auf jeden Vertex an
            Vertex v1 = transform.transform(triangle.v1);
            Vertex v2 = transform.transform(triangle.v2);
            Vertex v3 = transform.transform(triangle.v3);
            // Verschiebe die Vertices in die Mitte der Canvas
            v1.x += width / 2.0;  v1.y += height / 2.0;
            v2.x += width / 2.0;  v2.y += height / 2.0;
            v3.x += width / 2.0;  v3.y += height / 2.0;

            // Berechne den Normalenvektor des Dreiecks (Kreuzprodukt zweier Kanten)
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

            // Bestimme die Bounding Box des Dreiecks
            int minX = (int) Math.max(0, Math.ceil(Math.min(v1.x, Math.min(v2.x, v3.x))));
            int maxX = (int) Math.min(width - 1, Math.floor(Math.max(v1.x, Math.max(v2.x, v3.x))));
            int minY = (int) Math.max(0, Math.ceil(Math.min(v1.y, Math.min(v2.y, v3.y))));
            int maxY = (int) Math.min(height - 1, Math.floor(Math.max(v1.y, Math.max(v2.y, v3.y))));

            double triangleArea = (v1.y - v3.y) * (v2.x - v3.x) + (v2.y - v3.y) * (v3.x - v1.x);

            // Rendern des Dreiecks mittels baryzentrischer Koordinaten
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

    /**
     * Berechnet eine gamma-korrigierte Schattierung für eine gegebene Farbe.
     *
     * @param color Die Originalfarbe.
     * @param shade Schattierungsfaktor (bspw. Cosinus des Winkels zur Blickrichtung).
     * @return Angepasste Farbe mit Gamma-Korrektur.
     */
    public static Color getShade(Color color, double shade) {
        double redLinear = Math.pow(color.getRed(), 2.4) * shade;
        double greenLinear = Math.pow(color.getGreen(), 2.4) * shade;
        double blueLinear = Math.pow(color.getBlue(), 2.4) * shade;

        double red = Math.pow(redLinear, 1.0 / 2.4);
        double green = Math.pow(greenLinear, 1.0 / 2.4);
        double blue = Math.pow(blueLinear, 1.0 / 2.4);

        return new Color(clamp(red), clamp(green), clamp(blue), 1.0);
    }

    /**
     * Beschränkt einen Wert auf den Bereich [0, 1].
     *
     * @param value Der zu begrenzende Wert.
     * @return Wert, begrenzt auf [0, 1].
     */
    private static double clamp(double value) {
        return Math.max(0, Math.min(1, value));
    }

    /**
     * Erzeugt eine Liste von verfeinerten Dreiecken.
     * <p>
     * Hinweis: Diese Methode wird in der aktuellen Toggle-Implementierung nicht verwendet,
     * da nur einfache Formen (Dreieck bzw. Quadrat) gezeichnet werden. Wird ein komplexerer
     * 3D-Körper benötigt (z. B. zur Erzeugung einer Kugeloberfläche), kann diese Methode 
     * genutzt werden, um ein Tetraeder schrittweise zu "inflaten".
     * </p>
     *
     * @param tris Liste der Ausgangsdreiecke.
     * @return Liste der verfeinerten Dreiecke.
     */
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

    // --- Hilfsklassen ---

    /**
     * Repräsentiert einen Punkt (Vertex) im 3D-Raum.
     */
    public static class Vertex {
        double x, y, z;

        /**
         * Konstruktor für einen 3D-Punkt.
         *
         * @param x x-Koordinate
         * @param y y-Koordinate
         * @param z z-Koordinate
         */
        public Vertex(double x, double y, double z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }

    /**
     * Repräsentiert ein Dreieck im 3D-Raum.
     * <p>
     * Ein Dreieck besteht aus drei Vertices und einer Farbe.
     * </p>
     */
    public static class Triangle {
        Vertex v1, v2, v3;
        Color color;

        /**
         * Konstruktor für ein Dreieck.
         *
         * @param v1    Erster Eckpunkt
         * @param v2    Zweiter Eckpunkt
         * @param v3    Dritter Eckpunkt
         * @param color Farbe des Dreiecks
         */
        public Triangle(Vertex v1, Vertex v2, Vertex v3, Color color) {
            this.v1 = v1;
            this.v2 = v2;
            this.v3 = v3;
            this.color = color;
        }
    }

    /**
     * Klasse zur Darstellung einer 3x3-Matrix für 3D-Transformationen.
     * <p>
     * Die Matrix wird in Zeilen-Reihenfolge (row-major order) gespeichert.
     * </p>
     */
    public static class Matrix3 {
        double[] m; // Array mit 9 Elementen

        /**
         * Konstruktor für die Matrix.
         *
         * @param m Array mit 9 Elementen, das die Matrix definiert.
         * @throws IllegalArgumentException wenn das Array nicht genau 9 Elemente enthält.
         */
        public Matrix3(double[] m) {
            if (m.length != 9) throw new IllegalArgumentException("Matrix muss 9 Elemente haben");
            this.m = m;
        }

        /**
         * Multipliziert diese Matrix mit einer anderen Matrix.
         *
         * @param other Die Matrix, mit der multipliziert werden soll.
         * @return Neue Matrix als Ergebnis der Multiplikation.
         */
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

        /**
         * Transformiert einen 3D-Punkt (Vertex) mit dieser Matrix.
         *
         * @param v Zu transformierender Punkt.
         * @return Neuer Vertex nach der Transformation.
         */
        public Vertex transform(Vertex v) {
            double x = m[0] * v.x + m[1] * v.y + m[2] * v.z;
            double y = m[3] * v.x + m[4] * v.y + m[5] * v.z;
            double z = m[6] * v.x + m[7] * v.y + m[8] * v.z;
            return new Vertex(x, y, z);
        }
    }
}