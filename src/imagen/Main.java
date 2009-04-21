package imagen;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * What is the simplest thing that can possibly work?
 */
public class Main implements ChangeListener, DocumentListener {


    public static final int NWORKERS = 2;
    private static final int SLOWDOWN = 1;

    ExecutorService service = Executors.newFixedThreadPool(NWORKERS);

    ArrayList<PixelWorker> workers;
    BufferedImage imageIn;
    BufferedImage imageOut;
    ImageView inView;
    ImageView outView;
    String source;

    long totalTime;
    int invocations;
    double average;

    JFrame frame;
    JLabel averageLabel;
    JTextArea codeArea;
    public static int filterValue = 0;
    public PointFilter filter;

    public Main() throws IOException {
        source = "load 1\nload 2\nadd\nreturn\n";
        compileFilter(source);

        workers = new ArrayList<PixelWorker>(NWORKERS);
        for (int i = 0; i < NWORKERS; i++) {
            PixelWorker w = new PixelWorker(null, null, 0, 0);
            workers.add(w);
        }

        imageIn = ImageIO.read(new File("house_m.jpg"));
        if (imageIn.getType() != BufferedImage.TYPE_INT_ARGB) {
            int width = imageIn.getWidth();
            int height = imageIn.getHeight();
            int[] pixels = (int[]) imageIn.getRGB(0, 0, width, height, null, 0, width);
            BufferedImage convertedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            convertedImage.getRaster().setDataElements(0, 0, width, height, pixels);
            imageIn = convertedImage;
        }

        imageOut = process(imageIn);


        frame = new JFrame();
        frame.getContentPane().setLayout(new BorderLayout());

        JSlider slider = new JSlider(-255, 255, 0);
        slider.addChangeListener(this);
        averageLabel = new JLabel();
        JPanel topFlow = new JPanel(new FlowLayout());
        topFlow.add(slider);
        topFlow.add(averageLabel);
        frame.getContentPane().add(topFlow, BorderLayout.NORTH);

        inView = new ImageView(imageIn);
        outView = new ImageView(imageOut);
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, inView, outView);
        split.setDividerLocation(0.5);
        split.setDividerLocation(500);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().add(split, BorderLayout.CENTER);

        codeArea = new JTextArea(source);
        codeArea.setSize(300, 300);
        codeArea.setMinimumSize(new Dimension(300, 300));
        codeArea.setPreferredSize(new Dimension(300, 300));
        codeArea.getDocument().addDocumentListener(this);
        frame.getContentPane().add(codeArea, BorderLayout.SOUTH);
        frame.setSize(1000, 800);
        frame.setVisible(true);
    }

    public void stateChanged(ChangeEvent e) {
        JSlider slider = (JSlider) e.getSource();
        filterValue = slider.getValue();
        imageOut = process(imageIn);
        outView.image = imageOut;
        outView.repaint();
        averageLabel.setText(String.format("%.5f", calculateAverage()));
    }

    public static void main(String[] args) throws IOException {
        new Main();
    }

    private double calculateAverage() {
        return totalTime / (double) invocations;
    }

    public BufferedImage process(BufferedImage inImage) {
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


        int totalPixels = inPixels.length;
        int pixelsPerWorker = inPixels.length / NWORKERS;
        int offset = 0;
        int length = pixelsPerWorker;

        for (int i = 0; i < NWORKERS; i++) {
            if (i == NWORKERS - 1) { // Last worker gets less pixels
                length = totalPixels - offset - 1;
            }
            PixelWorker w = workers.get(i);
            w.inPixels = inPixels;
            w.outPixels = outPixels;
            w.offset = offset;
            w.length = length;
            offset += pixelsPerWorker;
        }

        long time = System.nanoTime();
        try {
            service.invokeAll(workers, 5000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        time = System.nanoTime() - time;
        totalTime += time;
        invocations++;

        outRaster.setDataElements(0, 0, width, height, outPixels);

        return outImage;
    }

    public static void process(vec4 v1, vec4 out) {
        int v = filterValue;
        for (int i = 0; i < SLOWDOWN; i++) {
            out.r = v1.r + v;
            out.g = v1.g + v;
            out.b = v1.b + v;
            out.a = v1.a + v;
        }
    }

    public void insertUpdate(DocumentEvent e) {
        recompileFilter();
    }

    public void removeUpdate(DocumentEvent e) {
        recompileFilter();
    }

    public void changedUpdate(DocumentEvent e) {
        recompileFilter();
    }

    public void compileFilter(String source) {
        CodeWriter cw = new CodeWriter();
        Class pointFilterClass = cw.compileCode(source);
        try {
            Object obj = pointFilterClass.newInstance();
            filter = (PointFilter) obj;
        } catch (ClassCastException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    public void recompileFilter() {
        source = codeArea.getText();
        try {
            compileFilter(source);
            imageOut = process(imageIn);
            outView.image = imageOut;
            outView.repaint();
            averageLabel.setText(String.format("%.5f", calculateAverage()));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public class PixelWorker implements Callable<Object> {
        int[] inPixels;
        int[] outPixels;
        int offset;
        int length;

        public PixelWorker(int[] inPixels, int[] outPixels, int offset, int length) {
            this.inPixels = inPixels;
            this.outPixels = outPixels;
            this.offset = offset;
            this.length = length;
        }

        public Object call() throws Exception {
            vec4 inVec = new vec4();
            vec4 outVec = new vec4();
            for (int pos = offset + length - 1; pos >= offset; --pos) {
                int rgb = inPixels[pos];
                // TODO: filterValue should not be here.
                outPixels[pos] = filter.filter(rgb, filterValue);
//                inVec.a = rgb & 0xff000000;
//                inVec.r = (rgb >> 16) & 0xff;
//                inVec.g = (rgb >> 8) & 0xff;
//                inVec.b = rgb & 0xff;
//                process(inVec, outVec);
//                outPixels[pos] = outVec.a | (outVec.r << 16) | (outVec.g << 8) | outVec.b;
            }
            return null;
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
            Graphics2D g2 = (Graphics2D) g;
            // Get the width of the view
            int viewWidth = (int) getBounds().getWidth();
            int imageWidth = image.getWidth(null);
            float factor = viewWidth / (float) imageWidth;
            g2.scale(factor, factor);
            g.drawImage(image, 0, 0, null);
        }
    }

}
