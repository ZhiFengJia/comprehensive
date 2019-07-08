package com.jzf;


import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * @author Jia ZhiFeng <312290710@qq.com>
 * @date 2018/10/8 16:23:53
 */
public class ClassLoaderTest {
    public static void main(String[] args) throws ClassNotFoundException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        MyClassLoader classLoader = new MyClassLoader();
        Class<?> aClass = classLoader.loadClass("com.jzf.Test");

        Method method = aClass.getMethod("main", String[].class);
        method.invoke(null, new Object[]{new String[]{null}});
    }

    /**
     * -XX:+TraceClassLoading
     * 加载D盘下的class文件
     *
     * 将以下代码编译后放入D盘下测试:
     * public class Test {
     *     public static void main(String[] args) {
     *         ClassLoader classLoader = Test.class.getClassLoader();
     *         System.out.print("我的类加载器家族:" + classLoader);
     *         while ((classLoader = classLoader.getParent()) != null) {
     *             System.out.print("-->" + classLoader);
     *         }
     *         System.out.println("");
     *     }
     * }
     *
     */
    static class MyClassLoader extends ClassLoader {

//        public MyClassLoader() {
//            super(MyClassLoader.class.getClassLoader());
//        }

        @Override
        protected Class<?> findClass(String name) {
            byte[] bytes = new byte[0];
            try {
                bytes = getBytes(name);
            } catch (IOException e) {
                e.printStackTrace();
            }

            Class<?> aClass = null;
            if (bytes.length != 0) {
                aClass = defineClass(name, bytes, 0, bytes.length);
            }
            return aClass;
        }

        private byte[] getBytes(String name) throws IOException {
            String file = "D:\\" + name.replace('.', '\\') + ".class";
            System.out.println("正在加载类: " + file);

            InputStream inputStream = new FileInputStream(file);
            byte[] bytes = new byte[inputStream.available()];
            inputStream.read(bytes);
            return bytes;
        }
    }
}

