package com.jzf;

/**
 * 测试死锁
 *
 * @author Jia ZhiFeng <312290710@qq.com>
 * @date 2019/6/13 14:06:13
 */
@SuppressWarnings("all")
public class DeadLockTest {

    public static void main(String[] args) {
        byte[] lock1 = new byte[0];
        byte[] lock2 = new byte[0];

        new Thread(() -> {
            synchronized (lock1) {
                System.out.println(Thread.currentThread().getName() + "获取lock1,等待获取lock2");
                synchronized (lock2) {
                    System.out.println(Thread.currentThread().getName() + "获取lock2");
                }
            }
        }).start();

        new Thread(() -> {
            synchronized (lock2) {
                System.out.println(Thread.currentThread().getName() + "获取lock2,等待获取lock1");
                synchronized (lock1) {
                    System.out.println(Thread.currentThread().getName() + "获取lock1");
                }
            }
        }).start();
    }
}
