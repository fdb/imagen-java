package imagen;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.lang.reflect.Method;

public class CodeWriter implements Opcodes {
    ClassWriter cw;
    MethodVisitor mv;
    JITClassLoader classLoader = new JITClassLoader(Thread.currentThread().getContextClassLoader());

    public Class compileCode(String code) {
        cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);

        int bigRandomNumber = (int) (Math.random() * 50000);
        String randomClassName = "F" + bigRandomNumber;

        cw.visit(V1_5, ACC_PUBLIC + ACC_SUPER, randomClassName, null, "java/lang/Object", new String[]{"imagen/PointFilter"});
        // Build constructor
        MethodVisitor m = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
        m.visitCode();
        m.visitVarInsn(Opcodes.ALOAD, 0);
        m.visitMethodInsn(INVOKESPECIAL, "java/lang/Object",
                "<init>", "()V");
        m.visitInsn(RETURN);

        m.visitMaxs(1, 1);
        m.visitEnd();
        // Build filter method
        String methodDescriptor = Type.getMethodDescriptor(Type.INT_TYPE, new Type[]{Type.INT_TYPE, Type.INT_TYPE});
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "filter", methodDescriptor, null, null);
        mv.visitCode();

        String[] lines = code.split("\n");
        for (String line : lines) {
            line = line.trim();
            // Ignore empty lines
            if (line.length() == 0) continue;
            // Ignore comments
            if (line.startsWith("#")) continue;
            String[] tokens = line.split("\\s+");
            String cmd = tokens[0];
            // The big switch
            if (cmd.equals("load")) {
                int register = Integer.parseInt(tokens[1]);
                mv.visitVarInsn(ILOAD, register);
            } else if (cmd.equals("push")) {
                int value = Integer.parseInt(tokens[1]);
                mv.visitIntInsn(BIPUSH, value);
            } else if (cmd.equals("add")) {
                mv.visitInsn(IADD);
            } else if (cmd.equals("return")) {
                mv.visitInsn(IRETURN);
            } else {
                throw new RuntimeException("Unknown command " + cmd);
            }
        }

        mv.visitMaxs(2, 0);
        mv.visitEnd();
        cw.visitEnd();
        byte[] byteCode = cw.toByteArray(); // Sorry for the name, I couldn't resist.
        return classLoader.defineClassX(randomClassName, byteCode, 0, byteCode.length);
    }

    public void test() {
        cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);

        cw.visit(V1_5, ACC_PUBLIC + ACC_SUPER, "MyFilter", null, "java/lang/Object", null);

        String methodDescriptor = Type.getMethodDescriptor(Type.INT_TYPE, new Type[]{Type.INT_TYPE, Type.INT_TYPE});
        System.out.println("methodDescriptor = " + methodDescriptor);

        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC | ACC_STATIC, "testMethod", methodDescriptor, null, null);
        mv.visitCode();

        mv.visitVarInsn(ILOAD, 0);
        mv.visitVarInsn(ILOAD, 1);
        mv.visitInsn(IADD);
        mv.visitInsn(IRETURN);
        mv.visitMaxs(1, 1);
        mv.visitEnd();
        cw.visitEnd();

        byte[] code = cw.toByteArray();
        Class cls = classLoader.defineClassX("TestClass", code, 0, code.length);
        System.out.println("cls = " + cls.getSimpleName());
        try {
            Method m = cls.getMethod("testMethod", Integer.TYPE, Integer.TYPE);
            Object res = m.invoke(null, 10, 45);
            System.out.println("res = " + res);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void test2() {
        //cwt.test();
        String code = "load 0\nload 1\nadd\npush 5\nadd\nreturn\n";
        Class cls = compileCode(code);
        try {
            Method m = cls.getMethod("testMethod", Integer.TYPE, Integer.TYPE);
            Object res = m.invoke(null, 10, 45);
            System.out.println("res = " + res);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public static void main(String[] args) {
        CodeWriter cwt = new CodeWriter();
        cwt.test2();
    }
}
