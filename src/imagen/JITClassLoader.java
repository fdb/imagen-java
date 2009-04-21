package imagen;

import sun.misc.Unsafe;

import java.lang.reflect.Field;

public class JITClassLoader extends ClassLoader {
    private static boolean sunJVM;
    private static Object sunUnsafe;

    static {
        try {
            Field f = Unsafe.class.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            sunUnsafe = f.get(null);
            sunJVM = true;
        }
        catch (Throwable t) {
            t.printStackTrace();
            sunJVM = false;
        }
    }


    public JITClassLoader(ClassLoader classLoader) {
        super(classLoader);
    }

    public Class<?> defineClassX(String className, byte[] b, int off, int len) {
        if (sunJVM) {
            return ((Unsafe) sunUnsafe).defineClass(className, b, off, len);
        } else {
            return super.defineClass(className, b, off, len);
        }
    }
}