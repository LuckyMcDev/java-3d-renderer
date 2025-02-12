package main.java;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;

public class Main {
    private static double rotationX = 0;
    private static double rotationY = 0;
    private static Color sphereColor = Color.WHITE;
    private static int lastX = 0, lastY = 0;

    public static void main(String[] args) {
        JFrame frame = new JFrame();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());
        frame.setSize(800, 600);
        frame.setTitle("3D Renderer");
        frame.setIconImage(new ImageIcon("icon.png").getImage());

        // Main panel for rendering
        JPanel renderPanel = new JPanel() {
            public void paintComponent(Graphics graphics) {
                super.paintComponent(graphics);
                Graphics2D graph2 = (Graphics2D) graphics;
                graph2.setColor(Color.BLACK);
                graph2.fillRect(0, 0, getWidth(), getHeight());

                ArrayList<Triangle> tris = new ArrayList<>();
                tris.add(new Triangle(new Vertex(100, 100, 100), new Vertex(-100, -100, 100), new Vertex(-100, 100, -100), sphereColor));
                tris.add(new Triangle(new Vertex(100, 100, 100), new Vertex(-100, -100, 100), new Vertex(100, -100, -100), sphereColor));
                tris.add(new Triangle(new Vertex(-100, 100, -100), new Vertex(100, -100, -100), new Vertex(100, 100, 100), sphereColor));
                tris.add(new Triangle(new Vertex(-100, 100, -100), new Vertex(100, -100, -100), new Vertex(-100, -100, 100), sphereColor));

                final int INFLATION_LEVEL = 4;
                for (int i = 0; i < INFLATION_LEVEL; i++) {
                    tris = inflate(tris);
                }

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

                BufferedImage img = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);
                double[] zBuffer = new double[img.getWidth() * img.getHeight()];
                Arrays.fill(zBuffer, Double.NEGATIVE_INFINITY);

                for (Triangle triangle : tris) {
                    Vertex v1 = transform.transform(triangle.v1);
                    v1.x += getWidth() / 2.0;
                    v1.y += getHeight() / 2.0;
                    Vertex v2 = transform.transform(triangle.v2);
                    v2.x += getWidth() / 2.0;
                    v2.y += getHeight() / 2.0;
                    Vertex v3 = transform.transform(triangle.v3);
                    v3.x += getWidth() / 2.0;
                    v3.y += getHeight() / 2.0;

                    Vertex ab = new Vertex(v2.x - v1.x, v2.y - v1.y, v2.z - v1.z);
                    Vertex ac = new Vertex(v3.x - v1.x, v3.y - v1.y, v3.z - v1.z);

                    Vertex norm = new Vertex(
                            ab.y * ac.z - ab.z * ac.y,
                            ab.z * ac.x - ab.x * ac.z,
                            ab.x * ac.y - ab.y * ac.x
                    );

                    double normalLength = Math.sqrt(norm.x * norm.x + norm.y * norm.y + norm.z * norm.z);
                    norm.x /= normalLength;
                    norm.y /= normalLength;
                    norm.z /= normalLength;

                    double angleCos = Math.abs(norm.z);

                    int minX = (int) Math.max(0, Math.ceil(Math.min(v1.x, Math.min(v2.x, v3.x))));
                    int maxX = (int) Math.min(img.getWidth() - 1, Math.floor(Math.max(v1.x, Math.max(v2.x, v3.x))));
                    int minY = (int) Math.max(0, Math.ceil(Math.min(v1.y, Math.min(v2.y, v3.y))));
                    int maxY = (int) Math.min(img.getHeight() - 1, Math.floor(Math.max(v1.y, Math.max(v2.y, v3.y))));

                    double triangleArea = (v1.y - v3.y) * (v2.x - v3.x) + (v2.y - v3.y) * (v3.x - v1.x);

                    for (int y = minY; y <= maxY; y++) {
                        for (int x = minX; x <= maxX; x++) {
                            double b1 = ((y - v3.y) * (v2.x - v3.x) + (v2.y - v3.y) * (v3.x - x)) / triangleArea;
                            double b2 = ((y - v1.y) * (v3.x - v1.x) + (v3.y - v1.y) * (v1.x - x)) / triangleArea;
                            double b3 = ((y - v2.y) * (v1.x - v2.x) + (v1.y - v2.y) * (v2.x - x)) / triangleArea;

                            if (b1 >= 0 && b1 <= 1 && b2 >= 0 && b2 <= 1 && b3 >= 0 && b3 <= 1) {
                                double depth = b1 * v1.z + b2 * v2.z + b3 * v3.z;
                                int zIndex = y * img.getWidth() + x;

                                if (zBuffer[zIndex] < depth) {
                                    img.setRGB(x, y, getShade(triangle.color, angleCos).getRGB());
                                    zBuffer[zIndex] = depth;
                                }
                            }
                        }
                    }
                }

                graph2.drawImage(img, 0, 0, null);
            }
        };

        frame.add(renderPanel, BorderLayout.CENTER);

        // Add Mouse Listeners for rotation with the mouse
        renderPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                lastX = e.getX();
                lastY = e.getY();
            }
        });

        renderPanel.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                int deltaX = e.getX() - lastX;
                int deltaY = e.getY() - lastY;

                // Update the rotation angles based on mouse movement
                rotationX += Math.toRadians(deltaX * 0.5);  // Adjust sensitivity here
                rotationY -= Math.toRadians(deltaY * 0.5);  // Adjust sensitivity here

                // Keep the pitch rotation within bounds
                if (rotationY < -Math.PI / 2) rotationY = -Math.PI / 2;
                if (rotationY > Math.PI / 2) rotationY = Math.PI / 2;

                lastX = e.getX();
                lastY = e.getY();
                renderPanel.repaint();
            }
        });

        // Slider for rotation on X (heading)
        JSlider headingSlider = new JSlider(0, 360, 180);
        headingSlider.addChangeListener(e -> {
            rotationX = Math.toRadians(headingSlider.getValue());
            renderPanel.repaint();
        });

        // Slider for rotation on Y (pitch)
        JSlider pitchSlider = new JSlider(SwingConstants.VERTICAL, -90, 90, 0);
        pitchSlider.addChangeListener(e -> {
            rotationY = Math.toRadians(pitchSlider.getValue());
            renderPanel.repaint();
        });

        // RGB sliders for changing the color of the sphere
        JSlider redSlider = new JSlider(0, 255, 255);
        redSlider.setMajorTickSpacing(85);
        redSlider.setMinorTickSpacing(25);
        redSlider.setPaintTicks(true);
        redSlider.setPaintLabels(true);

        JSlider greenSlider = new JSlider(0, 255, 255);
        greenSlider.setMajorTickSpacing(85);
        greenSlider.setMinorTickSpacing(25);
        greenSlider.setPaintTicks(true);
        greenSlider.setPaintLabels(true);

        JSlider blueSlider = new JSlider(0, 255, 255);
        blueSlider.setMajorTickSpacing(85);
        blueSlider.setMinorTickSpacing(25);
        blueSlider.setPaintTicks(true);
        blueSlider.setPaintLabels(true);

        // Listener to update the color based on RGB values
        redSlider.addChangeListener(e -> {
            sphereColor = new Color(redSlider.getValue(), greenSlider.getValue(), blueSlider.getValue());
            renderPanel.repaint();
        });

        greenSlider.addChangeListener(e -> {
            sphereColor = new Color(redSlider.getValue(), greenSlider.getValue(), blueSlider.getValue());
            renderPanel.repaint();
        });

        blueSlider.addChangeListener(e -> {
            sphereColor = new Color(redSlider.getValue(), greenSlider.getValue(), blueSlider.getValue());
            renderPanel.repaint();
        });

        // Control panel for sliders
        JPanel controlPanel = new JPanel();
        controlPanel.setLayout(new BoxLayout(controlPanel, BoxLayout.Y_AXIS));
        controlPanel.add(headingSlider);
        controlPanel.add(new JLabel("Red"));
        controlPanel.add(redSlider);
        controlPanel.add(new JLabel("Green"));
        controlPanel.add(greenSlider);
        controlPanel.add(new JLabel("Blue"));
        controlPanel.add(blueSlider);

        // Control panel for pitch slider
        JPanel controlPanel2 = new JPanel();
        controlPanel2.add(pitchSlider);

        // Adding panels to the frame
        frame.add(controlPanel, BorderLayout.SOUTH);
        frame.add(controlPanel2, BorderLayout.WEST);

        frame.setVisible(true);
    }

    // Method for shading the color based on light angle
    public static Color getShade(Color color, double shade) {
        double redLinear = Math.pow(color.getRed(), 2.4) * shade;
        double greenLinear = Math.pow(color.getGreen(), 2.4) * shade;
        double blueLinear = Math.pow(color.getBlue(), 2.4) * shade;

        int red = (int) Math.pow(redLinear, 1 / 2.4);
        int green = (int) Math.pow(greenLinear, 1 / 2.4);
        int blue = (int) Math.pow(blueLinear, 1 / 2.4);

        return new Color(red, green, blue);
    }

    // Inflate method to create additional triangles
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
}
