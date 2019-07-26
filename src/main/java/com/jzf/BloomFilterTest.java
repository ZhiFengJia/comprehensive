package com.jzf;

import com.google.common.hash.Funnels;
import com.google.common.hash.Hashing;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.Response;

import java.io.IOException;
import java.nio.charset.Charset;

/**
 * @author Jia ZhiFeng <312290710@qq.com>
 * @date 2019/7/26 13:36:31
 */
public class BloomFilterTest {
    private static final Logger logger = LoggerFactory.getLogger(BloomFilterTest.class);

    public static void main(String[] args) throws IOException {
        long dataNum = 10_000L;
        BloomFilter filterTest = new BloomFilter(dataNum, 0.0001);

        for (int i = 0; i < dataNum; i++) {
            filterTest.put("bf", 100 + i + "");
        }
        logger.info("{}数据量插入", dataNum);

        int hitCount = 0;
        for (int i = 0; i < dataNum * 2; i++) {
            boolean exist = filterTest.isExist("bf", 100 + i + "");
            if (exist) {
                hitCount++;
            }
        }
        logger.info("{}数据量命中数:{}\t未命中数:{}", dataNum * 2, hitCount,dataNum * 2 - hitCount);
        logger.info("命中:不一定存在;未命中:一定不存在");
    }
}

/**
 * 布隆过滤器
 */
class BloomFilter {
    private static final Logger logger = LoggerFactory.getLogger(BloomFilter.class);
    private JedisPool jedisPool;
    private Jedis jedis;
    /**
     * bit数组长度
     */
    private long numBits;
    /**
     * hash函数个数
     */
    private int numHashFunctions;

    /**
     * @param expectedInsertions 要存储的数据量
     * @param fpp                所能容忍错误率
     */
    BloomFilter(long expectedInsertions, double fpp) {
        numBits = (long) (-expectedInsertions * Math.log(fpp) / (Math.log(2) * Math.log(2)));
        numHashFunctions = Math.max(1, (int) Math.round((double) numBits / expectedInsertions * Math.log(2)));

        //测试连接redis
        jedisPool = new JedisPool("redis", 6379);
        jedis = jedisPool.getResource();
        jedis.auth("jzfJZF123");

        Long bf = jedis.del("bf");
        if (bf > 0) {
            logger.info("删除以往数据");
        }
    }

    /**
     * 将key存入redis bitmap
     */
    public void put(String where, String key) throws IOException {
        long[] indexs = getIndexs(key);
        //这里使用了Redis管道来降低过滤器运行当中访问Redis次数 降低Redis并发量
        Pipeline pipeline = jedis.pipelined();
        try {
            for (long index : indexs) {
                pipeline.setbit(where, index, true);
            }
            pipeline.sync();
            /**
             * 把数据存储到mysql中
             */
        } finally {
            pipeline.close();
        }
    }

    /**
     * 判断keys是否存在于集合where中
     *
     * @return true:不一定存在;false:一定不存在
     * @throws IOException
     */
    public boolean isExist(String where, String key) throws IOException {
        long[] indexs = getIndexs(key);
        boolean result;
        //这里使用了Redis管道来降低过滤器运行当中访问Redis次数 降低Redis并发量
        Pipeline pipeline = jedis.pipelined();
        try {
            for (long index : indexs) {
                pipeline.getbit(where, index);
            }
            result = !pipeline.syncAndReturnAll().contains(false);
        } finally {
            pipeline.close();

        }
        return result;
    }

    /**
     * 根据key计算一个hash值,方法来自guava
     */
    private long hash(String key) {
        Charset charset = Charset.forName("UTF-8");
        return Hashing.murmur3_128().hashObject(key, Funnels.stringFunnel(charset)).asLong();
    }

    /**
     * 根据key获取bitmap下标,方法来自guava
     */
    private long[] getIndexs(String key) {
        long hash1 = hash(key);
        long hash2 = hash1 >>> 16;
        long[] result = new long[numHashFunctions];
        for (int i = 0; i < numHashFunctions; i++) {
            long combinedHash = hash1 + i * hash2;
            if (combinedHash < 0) {
                combinedHash = ~combinedHash;
            }
            result[i] = combinedHash % numBits;
        }
        return result;
    }

    private long getCount() throws IOException {
        Pipeline pipeline = jedis.pipelined();
        Response<Long> bf = pipeline.bitcount("bf");
        pipeline.sync();
        Long count = bf.get();
        pipeline.close();
        return count;
    }
}