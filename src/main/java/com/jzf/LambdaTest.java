package com.jzf;

import java.lang.invoke.CallSite;
import java.lang.invoke.LambdaMetafactory;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.function.Function;

/**
 * {@link LambdaMetafactory}
 * public static CallSite metafactory(MethodHandles.Lookup caller,
 *                                        String invokedName,
 *                                        MethodType invokedType,
 *                                        MethodType samMethodType,
 *                                        MethodHandle implMethod,
 *                                        MethodType instantiatedMethodType)
 * invokedName:要实现的方法的名称。当与'invokedynamic'一起使用时，这由'invokedynamic'结构的'name and type'提供，并由VM自动堆积。
 * invokedType: CallSite的预期签名。参数类型表示捕获变量的类型;返回类型是要实现的接口。当与'invokedynamic'一起使用时，这由'invokedynamic'结构的'name and type'提供，并由VM自动堆积。
 * 				如果实现方法是实例方法，并且此签名具有任何参数，则调用签名中的第一个参数必须与接收方对应。
 * samMethodType: 函数对象要实现的方法的签名和返回类型。
 * implMethod: 描述在调用时应该调用的实现方法的直接方法句柄(适当地调整参数类型、返回类型，并将捕获的参数前置到调用参数中)。
 * instantiatedMethodType: 应该在调用时动态强制的签名和返回类型。这可能与samMethodType相同，也可能是它的专门化。
 *
 * return: 一个CallSite，其目标可用于执行捕获，生成按invokedType命名的接口实例
 * @author Jia ZhiFeng <312290710@qq.com>
 * @date 2019/07/4 16:23:53
 */
public class LambdaTest {

    /**
     * 分析lambda表达式的实现过程
     *
     * @param args
     * @throws Throwable
     */
    public static void main(String[] args) throws Throwable {
        final MethodHandles.Lookup lookup = MethodHandles.lookup();
        final CallSite site = LambdaMetafactory.metafactory(lookup,
                "apply",
                MethodType.methodType(Function.class),
                MethodType.methodType(Object.class, Object.class),
                lookup.findStatic(LambdaTest.class, "lambda$main$custom", MethodType.methodType(URL.class, String.class)),
                MethodType.methodType(URL.class, String.class));

        Function<String, URL> function = (Function) site.getTarget().invokeExact();

        System.out.println("实现1: 等同于使用lambda表达式,模拟'invokedynamic'指令");
        Arrays.asList("http://www.baidu.com", "http://www.google.com")
                .stream()
                .map(function)
                .toArray();

        System.out.println("实现2: 使用lambda表达式,运行期生成匿名类");
        Arrays.asList("http://www.baidu.com", "http://www.google.com")
                .stream()
                .map(str -> {
                    System.out.println(str);
                    try {
                        return new URL(str);
                    } catch (MalformedURLException e) {
                        e.printStackTrace();
                    }
                    return null;
                }).toArray();

        System.out.println("实现3: 使用普通匿名类,编译期生成匿名类");
        Arrays.asList("http://www.baidu.com", "http://www.google.com")
                .stream()
                .map(new Function<String, URL>() {
                    @Override
                    public URL apply(String str) {
                        System.out.println(str);
                        try {
                            return new URL(str);
                        } catch (MalformedURLException e) {
                            e.printStackTrace();
                        }
                        return null;
                    }
                }).toArray();
    }

    private static URL lambda$main$custom(String str) {
        System.out.println(str);
        try {
            return new URL(str);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        return null;
    }
}