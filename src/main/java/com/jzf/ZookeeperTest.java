package com.jzf;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.locks.InterProcessMutex;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * @author Jia ZhiFeng <312290710@qq.com>
 * @date 2019/7/23 17:38:39
 */
public class ZookeeperTest {
    private static final Logger logger = LoggerFactory.getLogger(ZookeeperTest.class);
    /**
     * ZK服务器地址
     */
    private static final String ZK_SERVER = "zookeeper1:2101,zookeeper2:2102,zookeeper3:2103";

    public static void main(String[] args) {
        CuratorFramework client = CuratorFrameworkFactory.newClient(ZK_SERVER,
                new ExponentialBackoffRetry(1000, 3));
        client.start();
        InterProcessMutex lock = new InterProcessMutex(client, "/locks");

        try {
            lock.acquire();
            logger.info("执行临界代码");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                lock.release();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        try {
            TimeUnit.MINUTES.sleep(10);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
