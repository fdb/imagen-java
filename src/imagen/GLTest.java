package imagen;

import com.sun.opengl.util.texture.Texture;
import com.sun.opengl.util.texture.TextureIO;

import javax.media.opengl.*;
import javax.media.opengl.glu.GLU;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;

public class GLTest implements GLEventListener, ChangeListener {

    public static Dimension FRAME_SIZE = new Dimension(1000, 800);
    JFrame frame;
    BufferedImage imageIn;
    GLJPanel glPanel;
    Texture baseTexture;
    boolean codeDirty = true;
    private int theShader;
    private List<UniformVariable> uniforms;
    JSlider slider;


    public GLTest() throws IOException {

        frame = new JFrame();
        glPanel = new GLJPanel(new GLCapabilities());
        glPanel.addGLEventListener(this);
        slider = new JSlider(-255, 255, 0);
        slider.addChangeListener(this);
        frame.getContentPane().add(slider, BorderLayout.NORTH);
        frame.getContentPane().add(glPanel);
        frame.setSize(FRAME_SIZE);
        frame.setVisible(true);
    }

    public void init(GLAutoDrawable drawable) {
        GL gl = drawable.getGL();
        gl.glActiveTexture(GL.GL_TEXTURE0);
        baseTexture = loadTexture("house_m.jpg");

        gl.glEnable(GL.GL_BLEND);
        gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);

        // determine which areas of the polygon are to be rendered
        gl.glEnable(GL.GL_ALPHA_TEST);

        // Texture environment parameters
        gl.glTexParameterf(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MIN_FILTER, GL.GL_LINEAR);
        gl.glTexParameterf(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MAG_FILTER, GL.GL_LINEAR);
        gl.glTexParameterf(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_S, GL.GL_CLAMP);
        gl.glTexParameterf(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_T, GL.GL_CLAMP);

        // only render if alpha > 0
        gl.glAlphaFunc(GL.GL_GREATER, 0);

        gl.glShadeModel(GL.GL_SMOOTH);

        // set erase color
        gl.glClearColor(0.0f, 0.0f, 0.0f, 1.0f); // black
        // set drawing color and point size
        gl.glColor3f(0.0f, 0.0f, 0.0f);
        gl.glPointSize(4.0f); //a 'dot' is 4 by 4 pixels
    }

    public void display(GLAutoDrawable drawable) {
        GL gl = drawable.getGL();
        gl.glLoadIdentity();
        gl.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        gl.glClear(GL.GL_COLOR_BUFFER_BIT);
        gl.glColor3f(1.0f, 0.0f, 0.0f);
        viewOrtho(gl, frame.getWidth(), frame.getHeight());
        useShaderProgram(gl);

        gl.glBegin(GL.GL_QUADS);
        gl.glTexCoord2f(0.0f, 0.0f);
        gl.glVertex2f(-0.8f, -0.8f);
        gl.glTexCoord2f(1.0f, 0.0f);
        gl.glVertex2f(0.8f, -0.8f);
        gl.glTexCoord2f(1.0f, 1.0f);
        gl.glVertex2f(0.8f, 0.8f);
        gl.glTexCoord2f(0.0f, 1.0f);
        gl.glVertex2f(-0.8f, 0.8f);
        gl.glEnd();
    }

    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
        GL gl = drawable.getGL();
        GLU glu = new GLU();
        gl.glViewport(0, 0, width, height);
        gl.glMatrixMode(GL.GL_PROJECTION);
        gl.glLoadIdentity();
        glu.gluOrtho2D(0.0, FRAME_SIZE.width, FRAME_SIZE.height, 0);
    }

    public void displayChanged(GLAutoDrawable glAutoDrawable, boolean b, boolean b1) {
        // Do nothing
    }

    /**
     * Texture loader utilizes JOGL's provided utilities to produce a texture.
     *
     * @param fileName relative filename from execution point
     * @return a texture binded to the OpenGL context
     */

    public static Texture loadTexture(String fileName) {
        Texture text = null;
        try {
            text = TextureIO.newTexture(new File(fileName), false);
            text.setTexParameteri(GL.GL_TEXTURE_MAG_FILTER, GL.GL_NEAREST);
            text.setTexParameteri(GL.GL_TEXTURE_MIN_FILTER, GL.GL_NEAREST);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            System.out.println("Error loading texture " + fileName);
        }
        return text;
    }

    private static void viewOrtho(GL gl, int width, int height) {
        gl.glMatrixMode(GL.GL_PROJECTION);
        gl.glPushMatrix();
        gl.glLoadIdentity();
        gl.glOrtho(-1, 1, 1, -1, -1, 1);
        gl.glMatrixMode(GL.GL_MODELVIEW);
        gl.glPushMatrix();
        gl.glLoadIdentity();
    }

    private void useShaderProgram(GL gl) {
        if (codeDirty) {
            String shaderSource = "uniform sampler2D baseImage;\n" +
                    "uniform float value;\n" +
                    "void main()\n" +
                    "{\n" +
                    "  vec4 base = texture2D(baseImage, gl_TexCoord[0].st);\n" +
                    "  gl_FragColor = vec4(base.r + value, base.g + value, base.b + value, base.a + value);\n" +
                    "}\n";
            try {
                // todo: this always creates a new shader program, without deleting the old one. Bad?
                // gl.glDeleteProgram(theShader);
                theShader = ShaderUtils.createFragmentProgram(gl, shaderSource);
                uniforms = ShaderUtils.getUniformVariables(gl, theShader);
                //errorArea.setText("");
            } catch (ShaderUtils.ShaderException ex) {
                System.out.println("ex = " + ex);
            }
            codeDirty = false;
        }
        gl.glUseProgram(theShader);
        int baseImage = gl.glGetUniformLocation(theShader, "baseImage");
        gl.glUniform1i(baseImage, 0); // GL_TEXTURE0
        for (UniformVariable v : uniforms) {
            v.commitValue(gl);
        }
    }

    public static void main(String[] args) throws IOException {
        new GLTest();
    }

    public void stateChanged(ChangeEvent e) {
        UniformVariable.Float v = (UniformVariable.Float) uniforms.get(0);
        v.setValue(slider.getValue() / 255F);
        glPanel.repaint();
    }
}
