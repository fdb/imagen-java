package imagen;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;

/**
 * What is the simples thing that can possibly work?
 */
public class Main implements ChangeListener {


    BufferedImage imageIn;
    BufferedImage imageOut;
    ImageView inView;
    ImageView outView;
    JFrame frame;

    public Main() throws IOException {
        imageIn = ImageIO.read(new File("house_m.jpg"));
        if (imageIn.getType() != BufferedImage.TYPE_INT_ARGB) {
            int width = imageIn.getWidth();
            int height = imageIn.getHeight();
            int[] pixels = (int[]) imageIn.getRGB(0, 0, width, height, null, 0, width);
            BufferedImage convertedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            convertedImage.getRaster().setDataElements(0, 0, width, height, pixels);
            imageIn = convertedImage;
        }

        imageOut = process(imageIn, 0);

        frame = new JFrame();
        frame.getContentPane().setLayout(new BorderLayout());

        JSlider slider = new JSlider(-255, 255, 0);
        slider.addChangeListener(this);
        frame.getContentPane().add(slider, BorderLayout.NORTH);

        inView = new ImageView(imageIn);
        outView = new ImageView(imageOut);
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, inView, outView);
        split.setDividerLocation(0.5);
        split.setDividerLocation(500);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().add(split, BorderLayout.CENTER);
        frame.setSize(1000, 800);
        frame.setVisible(true);

    }

    public void stateChanged(ChangeEvent e) {
        JSlider slider = (JSlider) e.getSource();
        int value = slider.getValue();
        imageOut = process(imageIn, value);
        outView.image = imageOut;
        outView.repaint();

    }

    public static void main(String[] args) throws IOException {
        new Main();
    }

    public static BufferedImage process(BufferedImage inImage, int value) {
        int width = inImage.getWidth();
        int height = inImage.getHeight();
        BufferedImage outImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        WritableRaster outRaster = outImage.getRaster();

        int inType = inImage.getType();
        int[] inPixels;
        if (inType != BufferedImage.TYPE_INT_ARGB)
            throw new AssertionError("Image type should be TYPE_INT_ARGB");
        inPixels = (int[]) inImage.getRaster().getDataElements(0, 0, width, height, null);
        int[] outPixels = new int[inPixels.length];

        vec4 inVec = new vec4();
        vec4 outVec = new vec4();
        vec4 addVec = new vec4(value, value, value, 0);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int index = y * width + x;

                int rgb = inPixels[index];
                inVec.a = rgb & 0xff000000;
                inVec.r = (rgb >> 16) & 0xff;
                inVec.g = (rgb >> 8) & 0xff;
                inVec.b = rgb & 0xff;

                add(inVec, addVec, outVec);
                outPixels[index] = outVec.a | (outVec.r << 16) | (outVec.g << 8) | outVec.b;
            }
        }
        outRaster.setDataElements(0, 0, width, height, outPixels);

        return outImage;
    }

    public static void add(vec4 v1, vec4 v2, vec4 out) {
        for (int i = 0; i < 200; i++) {
            out.r = v1.r + v2.r;
            out.g = v1.g + v2.g;
            out.b = v1.b + v2.b;
            out.a = v1.a + v2.a;
        }
    }

    public static class vec4 {
        public int r, g, b, a;

        vec4() {
        }

        vec4(int r, int g, int b) {
            this.r = r;
            this.g = g;
            this.b = b;
        }

        vec4(int r, int g, int b, int a) {
            this.r = r;
            this.g = g;
            this.b = b;
            this.a = a;
        }

        vec4(int[] values) {
            switch (values.length) {
                case 1:
                    this.r = values[0];
                    break;
                case 2:
                    this.r = values[0];
                    this.g = values[1];
                    break;
                case 3:
                    this.r = values[0];
                    this.g = values[1];
                    this.b = values[2];
                    break;
                case 4:
                    this.r = values[0];
                    this.g = values[1];
                    this.b = values[2];
                    this.a = values[3];
                    break;
            }
        }

        float[] asFloatArray() {
            return new float[]{r, g, b, a};
        }

        int[] asIntArray() {
            return new int[]{r, g, b, a};
        }

    }

    public class ImageView extends JComponent {
        public Image image;

        public ImageView(Image image) {
            this.image = image;
        }

        @Override
        public void paint(Graphics g) {
            g.drawImage(image, 0, 0, null);
        }
    }

}
