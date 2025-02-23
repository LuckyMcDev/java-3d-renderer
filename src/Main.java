import javafx.application.Application;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Orientation;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
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
 * Hauptklasse der JavaFX-Anwendung, die entweder eine 3D-Sphäre oder einen 3D-Würfel rendert.
 * <p>
 * Der Benutzer kann über verschiedene Steuerelemente (Slider und CheckBox) die Drehung (Heading und Pitch),
 * die Farbe (RGB) sowie die zu rendernde Form (Sphäre vs. Würfel) auswählen.
 * <p>
 * Das Rendering erfolgt über einen selbst implementierten Software-Renderer,
 * der die 3D-Objekte als Dreiecksnetz (Mesh) mithilfe eines Z-Buffers und baryzentrischer
 * Koordinaten zeichnet.
 */
@SuppressWarnings("unused")
public class Main extends Application {

    // Rotationswinkel in Radiant:
    // rotationX steuert die Drehung um die Y-Achse (Heading, horizontal)
    // rotationY steuert die Drehung um die X-Achse (Pitch, vertikal)
    private double rotationX = 0;
    private double rotationY = 0;

    // Farbe des zu rendernden Objekts, standardmäßig Weiß.
    private Color sphereColor = Color.WHITE;

    // Letzte bekannte Mausposition; wird genutzt, um die Änderung der Rotationswinkel per Drag & Drop zu berechnen.
    private double lastX = 0, lastY = 0;
    
    // Flag, das bestimmt, welches Objekt gerendert wird:
    // true: Sphäre; false: Würfel.
    private boolean renderSphere = true;
    
    // Die Zeichenfläche (Canvas), auf der das 3D-Objekt gerendert wird.
    private Canvas canvas;

    /**
     * Entry Point der JavaFX-Anwendung.
     *
     * @param args Kommandozeilenargumente (werden nicht verwendet)
     */
    public static void main(String[] args) {
        launch(args);
    }

    /**
     * Startet die Anwendung und initialisiert das Hauptfenster (Stage), die Benutzeroberfläche
     * sowie alle Steuerungselemente und Event-Handler.
     * <p>
     * Die Methode legt fest:
     * <ul>
     *   <li>die Zeichenfläche (Canvas) für das Rendering,</li>
     *   <li>die Slider zur Steuerung von Heading, Pitch und Farbe,</li>
     *   <li>eine CheckBox, um zwischen Sphäre und Würfel umzuschalten,</li>
     *   <li>und Maus-Event-Handler zur Interaktion per Drag und Drop.</li>
     * </ul>
     *
     * @param primaryStage Das Hauptfenster der Anwendung.
     */
    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("3D Renderer");
        primaryStage.setHeight(800);
        primaryStage.setWidth(1200);

        // Erstelle die Zeichenfläche (Canvas) zum Rendern
        canvas = new Canvas(800, 600);
        // Füge Maus-Event-Handler hinzu, um Interaktionen (Drag & Drop) zu ermöglichen.
        addMouseHandlers();

        // Erstelle das Hauptlayout mit einem BorderPane.
        BorderPane root = new BorderPane();
        // Platziere die Canvas in der Mitte des Layouts.
        root.setCenter(canvas);

        // Erstelle einen Slider für die Heading-Drehung (0 bis 360°).
        Slider headingSlider = new Slider(0, 360, 180);
        headingSlider.setMaxWidth(400);
        headingSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            // Aktualisiere den Rotationswinkel (in Radiant) und rendere neu.
            rotationX = Math.toRadians(newVal.doubleValue());
            draw();
        });

        // Erstelle Slider zur Farbauswahl für Rot, Grün und Blau.
        Slider redSlider = createColorSlider(255);
        Slider greenSlider = createColorSlider(255);
        Slider blueSlider = createColorSlider(255);

        // Gemeinsamer Listener, der bei Änderungen der Farbwerte die Objektfarbe aktualisiert.
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

        // Ordne die Farbslider in einer horizontalen Box (HBox) an.
        HBox colorBox = new HBox(10, new Label("Red"), redSlider,
                                        new Label("Green"), greenSlider,
                                        new Label("Blue"), blueSlider);
        
        // CheckBox zum Umschalten zwischen Sphäre und Würfel.
        CheckBox shapeToggle = new CheckBox("Cube");
        shapeToggle.setOnAction(e -> {
            // Setzt renderSphere auf false, wenn die CheckBox ausgewählt ist (Würfel rendern)
            renderSphere = !shapeToggle.isSelected();
            draw();
        });
        HBox shapeBox = new HBox(10, new Label("Shape:"), shapeToggle);

        // VBox zur Anordnung des Heading-Sliders, der Farbslider und der Shape-Auswahl.
        VBox bottomBox = new VBox(10, headingSlider, colorBox, shapeBox);
        root.setBottom(bottomBox);

        // Erstelle einen vertikalen Slider für die Pitch-Drehung (-90° bis 90°).
        Slider pitchSlider = new Slider(-90, 90, 0);
        pitchSlider.setOrientation(Orientation.VERTICAL);
        pitchSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            // Aktualisiere den Pitch-Winkel (in Radiant) und rendere neu.
            rotationY = Math.toRadians(newVal.doubleValue());
            draw();
        });
        VBox leftBox = new VBox(new Label("Pitch"), pitchSlider);
        root.setLeft(leftBox);

        // Erstelle die Szene und setze sie auf das Hauptfenster.
        Scene scene = new Scene(root, 800, 600);
        primaryStage.setScene(scene);
        primaryStage.show();

        // Führe ein initiales Rendering des aktuellen 3D-Objekts aus.
        draw();
    }

    /**
     * Erstellt einen Slider zur Einstellung eines Farbwertes.
     * <p>
     * Der Slider zeigt Tick-Marks und -Labels an, wobei der Wertebereich von 0 bis 255 geht.
     *
     * @param initial Der Startwert des Sliders.
     * @return Den konfigurierten Slider.
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
     * Fügt der Canvas Maus-Event-Handler hinzu, um die Interaktion per Drag & Drop zu ermöglichen.
     * <p>
     * Beim Drücken der Maus wird die Startposition gespeichert. Beim Ziehen wird anhand der
     * Änderung der Mausposition die Drehung (Heading und Pitch) des 3D-Objekts angepasst.
     */
    private void addMouseHandlers() {
        canvas.setOnMousePressed((MouseEvent e) -> {
            lastX = e.getX();
            lastY = e.getY();
        });
        canvas.setOnMouseDragged((MouseEvent e) -> {
            // Berechne die Differenz zur letzten bekannten Mausposition
            double deltaX = -(e.getX() - lastX);
            double deltaY = -(e.getY() - lastY);
            // Aktualisiere die Rotationswinkel, skaliert durch einen Faktor zur Empfindlichkeitsanpassung
            rotationX -= Math.toRadians(deltaX * 0.5);
            rotationY -= Math.toRadians(deltaY * 0.5);
            // Begrenze den Pitch-Winkel auf [-90°, 90°]
            rotationY = Math.max(-Math.PI / 2, Math.min(Math.PI / 2, rotationY));
            lastX = e.getX();
            lastY = e.getY();
            draw();
        });
    }

    /**
     * Führt das Rendering des aktuell ausgewählten 3D-Objekts (Sphäre oder Würfel) durch.
     * <p>
     * Der Ablauf:
     * <ol>
     *   <li>Erstellen eines Render-Puffers (WritableImage) und initialisieren eines Z-Buffers.</li>
     *   <li>Erzeugen des Dreiecksnetzes (Mesh) des 3D-Objekts:
     *     <ul>
     *       <li>Für die Sphäre: Start mit einem Tetraeder und wiederholtes "Inflaten" (Subdividieren) der Dreiecke.</li>
     *       <li>Für den Würfel: Direkte Definition der 12 Dreiecke, die alle 6 Seiten abdecken.</li>
     *     </ul>
     *   </li>
     *   <li>Erzeugen und Kombinieren von Transformationsmatrizen für Heading (Y-Achse) und Pitch (X-Achse).</li>
     *   <li>Transformation und Zentrierung der Eckpunkte der Dreiecke.</li>
     *   <li>Berechnung des Normalenvektors jedes Dreiecks zur Bestimmung des Beleuchtungsfaktors.</li>
     *   <li>Iterieren über die Pixel innerhalb der Begrenzungsbox jedes Dreiecks, Berechnung der baryzentrischen
     *       Koordinaten und Vergleich der Tiefenwerte mittels des Z-Buffers.</li>
     *   <li>Setzen der Pixelfarbe unter Anwendung der gamma-korrigierten Schattierung.</li>
     *   <li>Zeichnen des finalen Bildes auf die Canvas.</li>
     * </ol>
     */
    private void draw() {
        int width = (int) canvas.getWidth();
        int height = (int) canvas.getHeight();

        // Erstelle einen Render-Puffer als WritableImage und initialisiere alle Pixel mit Schwarz.
        WritableImage image = new WritableImage(width, height);
        PixelWriter pw = image.getPixelWriter();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                pw.setColor(x, y, Color.BLACK);
            }
        }
        // Initialisiere einen Z-Buffer für die Tiefeninformationen aller Pixel.
        double[] zBuffer = new double[width * height];
        Arrays.fill(zBuffer, Double.NEGATIVE_INFINITY);

        // Erzeuge das Dreiecksnetz (Mesh) des zu rendernden 3D-Objekts.
        ArrayList<Triangle> tris;
        if (renderSphere) {
            // Sphäre: Erstelle ein Tetraeder als Ausgangsform.
            tris = new ArrayList<>();
            tris.add(new Triangle(new Vertex(100, 100, 100), new Vertex(-100, -100, 100), new Vertex(-100, 100, -100), sphereColor));
            tris.add(new Triangle(new Vertex(100, 100, 100), new Vertex(-100, -100, 100), new Vertex(100, -100, -100), sphereColor));
            tris.add(new Triangle(new Vertex(-100, 100, -100), new Vertex(100, -100, -100), new Vertex(100, 100, 100), sphereColor));
            tris.add(new Triangle(new Vertex(-100, 100, -100), new Vertex(100, -100, -100), new Vertex(-100, -100, 100), sphereColor));

            // Wiederholtes "Inflaten", um aus dem Tetraeder eine kugelähnliche Oberfläche zu generieren.
            final int INFLATION_LEVEL = 4;
            for (int i = 0; i < INFLATION_LEVEL; i++) {
                tris = inflate(tris);
            }
        } else {
            // Würfel: Erzeuge das Dreiecksnetz, das den Würfel darstellt.
            tris = getCubeTriangles();
            // Setze die Farbe aller Würfel-Dreiecke auf den aktuellen Farbwert.
            for (Triangle t : tris) {
                t.color = sphereColor;
            }
        }

        // Erstelle Transformationsmatrizen:
        // Heading-Transformation (Rotation um die Y-Achse)
        Matrix3 headingTransform = new Matrix3(new double[]{
                Math.cos(rotationX), 0, Math.sin(rotationX),
                0, 1, 0,
                -Math.sin(rotationX), 0, Math.cos(rotationX)
        });
        // Pitch-Transformation (Rotation um die X-Achse)
        Matrix3 pitchTransform = new Matrix3(new double[]{
                1, 0, 0,
                0, Math.cos(rotationY), Math.sin(rotationY),
                0, -Math.sin(rotationY), Math.cos(rotationY)
        });
        // Kombiniere die beiden Transformationen.
        Matrix3 transform = headingTransform.multiply(pitchTransform);

        // Iteriere über alle Dreiecke und rendere diese.
        for (Triangle triangle : tris) {
            // Transformiere die Eckpunkte des Dreiecks.
            Vertex v1 = transform.transform(triangle.v1);
            Vertex v2 = transform.transform(triangle.v2);
            Vertex v3 = transform.transform(triangle.v3);
            // Zentriere das Objekt in der Mitte der Canvas.
            v1.x += width / 2.0;  v1.y += height / 2.0;
            v2.x += width / 2.0;  v2.y += height / 2.0;
            v3.x += width / 2.0;  v3.y += height / 2.0;

            // Berechne den Normalenvektor des Dreiecks via Kreuzprodukt der Kantenvektoren.
            Vertex ab = new Vertex(v2.x - v1.x, v2.y - v1.y, v2.z - v1.z);
            Vertex ac = new Vertex(v3.x - v1.x, v3.y - v1.y, v3.z - v1.z);
            Vertex norm = new Vertex(
                    ab.y * ac.z - ab.z * ac.y,
                    ab.z * ac.x - ab.x * ac.z,
                    ab.x * ac.y - ab.y * ac.x
            );
            double normLength = Math.sqrt(norm.x * norm.x + norm.y * norm.y + norm.z * norm.z);
            if (normLength != 0) {
                norm.x /= normLength; 
                norm.y /= normLength; 
                norm.z /= normLength;
            }
            // Der Beleuchtungsfaktor basiert auf dem Cosinus des Winkels zwischen dem Normalenvektor und der Blickrichtung (hier z-Achse).
            double angleCos = Math.abs(norm.z);

            // Berechne die Begrenzungsbox (Bounding Box) des Dreiecks, um den zu zeichnenden Bereich einzuschränken.
            int minX = (int) Math.max(0, Math.ceil(Math.min(v1.x, Math.min(v2.x, v3.x))));
            int maxX = (int) Math.min(width - 1, Math.floor(Math.max(v1.x, Math.max(v2.x, v3.x))));
            int minY = (int) Math.max(0, Math.ceil(Math.min(v1.y, Math.min(v2.y, v3.y))));
            int maxY = (int) Math.min(height - 1, Math.floor(Math.max(v1.y, Math.max(v2.y, v3.y))));
            double triangleArea = (v1.y - v3.y) * (v2.x - v3.x) + (v2.y - v3.y) * (v3.x - v1.x);

            // Iteriere über jeden Pixel in der Bounding Box und berechne baryzentrische Koordinaten,
            // um festzustellen, ob der Pixel innerhalb des Dreiecks liegt.
            for (int y = minY; y <= maxY; y++) {
                for (int x = minX; x <= maxX; x++) {
                    double b1 = ((y - v3.y) * (v2.x - v3.x) + (v2.y - v3.y) * (v3.x - x)) / triangleArea;
                    double b2 = ((y - v1.y) * (v3.x - v1.x) + (v3.y - v1.y) * (v1.x - x)) / triangleArea;
                    double b3 = ((y - v2.y) * (v1.x - v2.x) + (v1.y - v2.y) * (v2.x - x)) / triangleArea;
                    if (b1 >= 0 && b1 <= 1 && b2 >= 0 && b2 <= 1 && b3 >= 0 && b3 <= 1) {
                        // Interpoliere den Tiefenwert (z-Wert) des Pixels
                        double depth = b1 * v1.z + b2 * v2.z + b3 * v3.z;
                        int zIndex = y * width + x;
                        // Aktualisiere den Pixel nur, wenn er näher an der Kamera liegt als bisherige Einträge im Z-Buffer.
                        if (zBuffer[zIndex] < depth) {
                            pw.setColor(x, y, getShade(triangle.color, angleCos));
                            zBuffer[zIndex] = depth;
                        }
                    }
                }
            }
        }

        // Zeichne das fertig gerenderte Bild auf die Canvas.
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.drawImage(image, 0, 0);
    }

    /**
     * Berechnet eine gamma-korrigierte Schattierung für eine gegebene Farbe.
     * <p>
     * Diese Methode passt die Helligkeit der Farbe basierend auf dem
     * Beleuchtungsfaktor (shade) an und korrigiert anschließend den Gamma-Wert,
     * um eine realistische Farbdarstellung zu erzielen.
     *
     * @param color Die Originalfarbe.
     * @param shade Der Beleuchtungsfaktor (üblicherweise der Cosinus des Winkels
     *              zwischen Normalenvektor und Blickrichtung).
     * @return Die angepasste, gamma-korrigierte Farbe.
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
     * @param value Der zu beschränkende Wert.
     * @return Der auf den Bereich [0, 1] begrenzte Wert.
     */
    private static double clamp(double value) {
        return Math.max(0, Math.min(1, value));
    }

    /**
     * Verfeinert (inflatiert) die Liste der Dreiecke.
     * <p>
     * Jedes gegebene Dreieck wird in vier kleinere unterteilt. Die neu entstehenden
     * Eckpunkte (Vertices) werden anschließend normalisiert, sodass sie auf der Oberfläche
     * einer Kugel liegen. Dadurch entsteht eine annähernd kugelartige Darstellung.
     *
     * @param tris Liste der ursprünglichen Dreiecke.
     * @return Eine neue Liste der verfeinerten Dreiecke.
     */
    public static ArrayList<Triangle> inflate(ArrayList<Triangle> tris) {
        ArrayList<Triangle> result = new ArrayList<>();

        // Berechne für jedes Dreieck die Mittelpunkte der Kanten und unterteile es in vier kleinere.
        for (Triangle t : tris) {
            Vertex m1 = new Vertex((t.v1.x + t.v2.x) / 2, (t.v1.y + t.v2.y) / 2, (t.v1.z + t.v2.z) / 2);
            Vertex m2 = new Vertex((t.v2.x + t.v3.x) / 2, (t.v2.y + t.v3.y) / 2, (t.v2.z + t.v3.z) / 2);
            Vertex m3 = new Vertex((t.v1.x + t.v3.x) / 2, (t.v1.y + t.v3.y) / 2, (t.v1.z + t.v3.z) / 2);

            result.add(new Triangle(t.v1, m1, m3, t.color));
            result.add(new Triangle(t.v2, m1, m2, t.color));
            result.add(new Triangle(t.v3, m2, m3, t.color));
            result.add(new Triangle(m1, m2, m3, t.color));
        }

        // Normalisiere alle Eckpunkte, sodass sie auf einer Kugeloberfläche liegen.
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

    /**
     * Erzeugt ein Dreiecksnetz, das einen Würfel darstellt.
     * <p>
     * Der Würfel wird als 12 Dreiecke (2 pro Seite) dargestellt. Die Eckpunkte
     * des Würfels werden so definiert, dass der Würfel seinen Mittelpunkt im Ursprung hat
     * und die halbe Kantenlänge 100 beträgt.
     *
     * @return Eine Liste von Dreiecken, die alle Seiten des Würfels abdecken.
     */
    private ArrayList<Triangle> getCubeTriangles() {
        ArrayList<Triangle> cubeTris = new ArrayList<>();
        // Definiere die 8 Eckpunkte eines Würfels
        Vertex v0 = new Vertex(-100, -100, -100);
        Vertex v1 = new Vertex(100, -100, -100);
        Vertex v2 = new Vertex(100, 100, -100);
        Vertex v3 = new Vertex(-100, 100, -100);
        Vertex v4 = new Vertex(-100, -100, 100);
        Vertex v5 = new Vertex(100, -100, 100);
        Vertex v6 = new Vertex(100, 100, 100);
        Vertex v7 = new Vertex(-100, 100, 100);

        // Front face (z positiv)
        cubeTris.add(new Triangle(v4, v5, v6, sphereColor));
        cubeTris.add(new Triangle(v4, v6, v7, sphereColor));
        // Back face (z negativ) – Reihenfolge so wählen, dass die Normalen nach außen zeigen
        cubeTris.add(new Triangle(v0, v3, v2, sphereColor));
        cubeTris.add(new Triangle(v0, v2, v1, sphereColor));
        // Left face (x negativ)
        cubeTris.add(new Triangle(v0, v4, v7, sphereColor));
        cubeTris.add(new Triangle(v0, v7, v3, sphereColor));
        // Right face (x positiv)
        cubeTris.add(new Triangle(v1, v2, v6, sphereColor));
        cubeTris.add(new Triangle(v1, v6, v5, sphereColor));
        // Top face (y positiv)
        cubeTris.add(new Triangle(v3, v7, v6, sphereColor));
        cubeTris.add(new Triangle(v3, v6, v2, sphereColor));
        // Bottom face (y negativ)
        cubeTris.add(new Triangle(v0, v5, v4, sphereColor));
        cubeTris.add(new Triangle(v0, v1, v5, sphereColor));

        return cubeTris;
    }

    // --- Hilfsklassen ---

    /**
     * Repräsentiert einen Punkt (Vertex) im 3D-Raum.
     * <p>
     * Diese Klasse speichert die x-, y- und z-Koordinaten eines Punkts und dient als
     * Basiseinheit für die Darstellung von 3D-Objekten.
     */
    public static class Vertex {
        double x, y, z;

        /**
         * Konstruktor für einen 3D-Punkt.
         *
         * @param x Die x-Koordinate.
         * @param y Die y-Koordinate.
         * @param z Die z-Koordinate.
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
     * Ein Dreieck wird durch drei Eckpunkte (Vertices) definiert und besitzt eine Farbe.
     * Es bildet die grundlegende geometrische Einheit zum Aufbau komplexerer 3D-Objekte.
     */
    public static class Triangle {
        Vertex v1, v2, v3;
        Color color;

        /**
         * Konstruktor für ein Dreieck.
         *
         * @param v1    Der erste Eckpunkt.
         * @param v2    Der zweite Eckpunkt.
         * @param v3    Der dritte Eckpunkt.
         * @param color Die Farbe des Dreiecks.
         */
        public Triangle(Vertex v1, Vertex v2, Vertex v3, Color color) {
            this.v1 = v1;
            this.v2 = v2;
            this.v3 = v3;
            this.color = color;
        }
    }

    /**
     * Stellt eine 3x3-Matrix für 3D-Transformationen dar.
     * <p>
     * Diese Matrix wird im row-major order gespeichert und ermöglicht Operationen wie
     * Matrix-Multiplikation und die Transformation von 3D-Punkten (Vertices).
     */
    public static class Matrix3 {
        double[] m; // Array mit 9 Elementen, das die Matrix darstellt.

        /**
         * Konstruktor für die 3x3-Matrix.
         *
         * @param m Ein Array mit exakt 9 Elementen, das die Matrix definiert.
         * @throws IllegalArgumentException wenn das Array nicht genau 9 Elemente enthält.
         */
        public Matrix3(double[] m) {
            if (m.length != 9)
                throw new IllegalArgumentException("Matrix muss 9 Elemente haben");
            this.m = m;
        }

        /**
         * Multipliziert diese Matrix mit einer anderen 3x3-Matrix.
         * <p>
         * Das Ergebnis ist eine neue Matrix, die der Matrix-Multiplikation entspricht.
         *
         * @param other Die Matrix, mit der multipliziert werden soll.
         * @return Eine neue Matrix als Ergebnis der Multiplikation.
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
         * <p>
         * Die Transformation erfolgt gemäß der Matrixgleichung:
         * <code>v' = M * v</code>.
         *
         * @param v Der zu transformierende Punkt.
         * @return Ein neuer Vertex, der das Ergebnis der Transformation darstellt.
         */
        public Vertex transform(Vertex v) {
            double x = m[0] * v.x + m[1] * v.y + m[2] * v.z;
            double y = m[3] * v.x + m[4] * v.y + m[5] * v.z;
            double z = m[6] * v.x + m[7] * v.y + m[8] * v.z;
            return new Vertex(x, y, z);
        }
    }
}
