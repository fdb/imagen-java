package imagen;

import javax.media.opengl.GL;

/**
 * Reperesents a "uniform" in GLSL.
 */
public abstract class UniformVariable {
    protected int program;
    protected String name;
    protected int type;
    protected int size; // for arrays

    public UniformVariable(int program, String name, int type, int size) {
        this.type = type;
        this.program = program;
        this.size = size;
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public int getType() {
        return type;
    }

    public int getSize() {
        return size;
    }

    public String toString() {
        return name;
    }

    public abstract void commitValue(GL gl);

    public static class Float extends UniformVariable {

        private float value;

        public Float(int program, String name, int type) {
            super(program, name, type, 1);
            assert (type == GL.GL_FLOAT);
        }

        public float getValue() {
            return value;
        }

        public void setValue(float value) {
            this.value = value;
        }

        public void commitValue(GL gl) {
            int loc = gl.glGetUniformLocation(program, name);
            gl.glUniform1f(loc, value);
        }

    }

    public static class FloatVec2 extends UniformVariable {

        private float[] values;

        public FloatVec2(int program, String name, int type, int size) {
            super(program, name, type, size);
            assert (type == GL.GL_FLOAT_VEC2);
        }

        public float[] getValue() {
            return values;
        }

        public void setValue(float[] values) {
            assert (values.length == size);
            this.values = values;
        }

        public void commitValue(GL gl) {
            int loc = gl.glGetUniformLocation(program, name);
            gl.glUniform2fv(loc, size, values, 0);
        }

    }

}

