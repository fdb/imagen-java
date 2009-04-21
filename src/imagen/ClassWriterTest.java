package imagen;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.lang.reflect.Method;

public class ClassWriterTest implements Opcodes {
    ClassWriter cw;
    MethodVisitor mv;
    JITClassLoader classLoader;

    public void test() {
        classLoader = new JITClassLoader(Thread.currentThread().getContextClassLoader());
        cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);

        cw.visit(V1_5, ACC_PUBLIC + ACC_SUPER, "TestClass", null, "java/lang/Object", null);

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

    public static void main(String[] args) {
        ClassWriterTest cwt = new ClassWriterTest();
        cwt.test();
    }
}
