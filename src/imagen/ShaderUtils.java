package imagen;

import javax.media.opengl.GL;
import java.util.ArrayList;
import java.util.List;

public final class ShaderUtils {

    private ShaderUtils() {
    }

    public static int createFragmentProgram(GL gl, String fragmentShaderSource) {
        int shader = compileShader(gl, GL.GL_FRAGMENT_SHADER, fragmentShaderSource);
        return createProgram(gl, new int[]{shader});
    }

    public static int compileShader(GL gl, int shaderType, String source) {
        int shader = gl.glCreateShader(shaderType);
        gl.glShaderSource(shader, 1, new String[]{source}, null, 0);
        gl.glCompileShader(shader);
        int[] retval = new int[]{-1};
        gl.glGetShaderiv(shader, GL.GL_COMPILE_STATUS, retval, 0);
        if (retval[0] == 0) { // Error
            String msg = shaderInfoLog(gl, shader);
            gl.glDeleteShader(shader);
            throw new ShaderException("compile", msg);
        }
        // System.out.println("retval = " + retval[0]);
        return shader;
    }

    public static int createProgram(GL gl, int[] shaders) {
        int program = gl.glCreateProgram();
        for (int shader : shaders) {
            gl.glAttachShader(program, shader);
        }
        gl.glLinkProgram(program);
        int[] retval = new int[]{-1};
        gl.glGetProgramiv(program, GL.GL_LINK_STATUS, retval, 0);
        if (retval[0] == 0) { // Error
            String msg = programInfoLog(gl, program);
            gl.glDeleteProgram(program);
            throw new ShaderException("link", msg);
        } else {
            return program;
        }
    }

    public static String shaderInfoLog(GL gl, int shader) {
        int[] retval = new int[]{-1};
        gl.glGetShaderiv(shader, GL.GL_INFO_LOG_LENGTH, retval, 0);
        int length = retval[0];
        if (length > 0) {
            byte[] info = new byte[length];
            gl.glGetShaderInfoLog(shader, length, retval, 0, info, 0);
            return new String(info, 0, retval[0]);
        } else {
            return "";
        }
    }

    public static String programInfoLog(GL gl, int program) {
        int[] retval = new int[]{-1};
        gl.glGetProgramiv(program, GL.GL_INFO_LOG_LENGTH, retval, 0);
        int length = retval[0];
        if (length > 0) {
            byte[] info = new byte[length];
            gl.glGetProgramInfoLog(program, length, retval, 0, info, 0);
            return new String(info, 0, retval[0]);
        } else {
            return "";
        }
    }

    public static List<UniformVariable> getUniformVariables(GL gl, int program) {
        int[] retval = new int[]{-1};
        gl.glGetProgramiv(program, GL.GL_ACTIVE_UNIFORMS, retval, 0);
        int activeUniforms = retval[0];

        gl.glGetProgramiv(program, GL.GL_ACTIVE_UNIFORM_MAX_LENGTH, retval, 0);
        int maxLength = retval[0];
        ArrayList<UniformVariable> uniforms = new ArrayList<UniformVariable>(maxLength);

        for (int index = 0; index < activeUniforms; index++) {
            int[] lengthPtr = new int[]{-1};
            int[] sizePtr = new int[]{-1};
            int[] typePtr = new int[]{-1};
            byte[] namePtr = new byte[maxLength];
            gl.glGetActiveUniform(program, index, maxLength, lengthPtr, 0, sizePtr, 0, typePtr, 0, namePtr, 0);
            String name = new String(namePtr, 0, lengthPtr[0]);
            int type = typePtr[0];
            int size = sizePtr[0];
            switch (type) {
                case GL.GL_FLOAT:
                    uniforms.add(new UniformVariable.Float(program, name, type));
                    break;
                case GL.GL_FLOAT_VEC2:
                    uniforms.add(new UniformVariable.FloatVec2(program, name, type, size));
                    break;
                case GL.GL_SAMPLER_2D:
                    break; // Don't handle this.
                default:
                    throw new RuntimeException("Don't know how to handle type " + type);
            }

        }

        return uniforms;
    }

    public static class ShaderException extends RuntimeException {
        public String type;
        public String msg;

        ShaderException(String type, String msg) {
            this.type = type;
            this.msg = msg;
        }

        public String toString() {
            return "Error during " + type + ": " + msg;
        }
    }
}
