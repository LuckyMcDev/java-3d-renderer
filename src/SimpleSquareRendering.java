import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

public class SimpleSquareRendering extends JPanel implements ActionListener {
    private Timer timer;
    private Cube cube;
    /**
     * @param angleX Winkel der X-Achse
     * @param angleY Winkel der Y-Achse
     * @param angleZ Winkel der Z-Achse
     */
    private int angleX = 0;
    private int angleY = 0;
    private int angleZ = 0;

    public SimpleSquareRendering() {
        cube = new Cube();
        timer = new Timer(16, this);
        timer.start();
        addKeyListener(new TAdapter());
        setFocusable(true);
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);

        cube.draw(g2d, getWidth() / 2, getHeight() / 2, angleX, angleY, angleZ);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        repaint();
    }

    private class TAdapter extends KeyAdapter {
        @Override
        public void keyPressed(KeyEvent e) {
            int key = e.getKeyCode();

            if (key == KeyEvent.VK_LEFT) {
                angleY -= 5;
            }

            if (key == KeyEvent.VK_RIGHT) {
                angleY += 5;
            }

            if (key == KeyEvent.VK_UP) {
                angleX -= 5;
            }

            if (key == KeyEvent.VK_DOWN) {
                angleX += 5;
            }

            if (key == KeyEvent.VK_A) {
                angleZ -= 5;
            }

            if (key == KeyEvent.VK_D) {
                angleZ += 5;
            }
        }
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("Simple 3D Renderer");
        SimpleSquareRendering renderer = new SimpleSquareRendering();
        frame.add(renderer);
        frame.setSize(800, 600);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}

class Cube {
    private final int[][] vertices = {
        {-1, -1, -1},
        { 1, -1, -1},
        { 1,  1, -1},
        {-1,  1, -1},
        {-1, -1,  1},
        { 1, -1,  1},
        { 1,  1,  1},
        {-1,  1,  1}
    };

    private final int[][] edges = {
        {0, 1}, {1, 2}, {2, 3}, {3, 0},
        {4, 5}, {5, 6}, {6, 7}, {7, 4},
        {0, 4}, {1, 5}, {2, 6}, {3, 7}
    };

    public void draw(Graphics2D g2d, int centerX, int centerY, int angleX, int angleY, int angleZ) {
        double[][] transformedVertices = new double[8][3];

        for (int i = 0; i < vertices.length; i++) {
            int[] v = vertices[i];

            double x = v[0];
            double y = v[1];
            double z = v[2];

            double radiansX = Math.toRadians(angleX);
            double radiansY = Math.toRadians(angleY);
            double radiansZ = Math.toRadians(angleZ);

            double cosX = Math.cos(radiansX);
            double sinX = Math.sin(radiansX);
            double cosY = Math.cos(radiansY);
            double sinY = Math.sin(radiansY);
            double cosZ = Math.cos(radiansZ);
            double sinZ = Math.sin(radiansZ);

            double tempX = x;
            double tempY = y * cosX - z * sinX;
            double tempZ = y * sinX + z * cosX;

            x = tempX * cosY + tempZ * sinY;
            y = tempY;
            z = -tempX * sinY + tempZ * cosY;

            tempX = x * cosZ - y * sinZ;
            tempY = x * sinZ + y * cosZ;
            tempZ = z;

            transformedVertices[i][0] = tempX;
            transformedVertices[i][1] = tempY;
            transformedVertices[i][2] = tempZ;
        }

        for (int[] edge : edges) {
            int x1 = (int) (transformedVertices[edge[0]][0] * 100 + centerX);
            int y1 = (int) (transformedVertices[edge[0]][1] * 100 + centerY);
            int x2 = (int) (transformedVertices[edge[1]][0] * 100 + centerX);
            int y2 = (int) (transformedVertices[edge[1]][1] * 100 + centerY);

            g2d.drawLine(x1, y1, x2, y2);
        }
    }
}