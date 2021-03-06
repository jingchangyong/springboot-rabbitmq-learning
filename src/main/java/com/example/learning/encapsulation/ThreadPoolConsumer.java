package com.example.learning.encapsulation;


import com.example.learning.common.Constants;
import com.example.learning.common.DetailRes;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by littlersmall on 16/5/23.
 * <p>
 * <p>
 * 在对消息处理的过程中，我们期望多线程并行执行来增加效率，因此对consumer做了一个线程池的封装。
 */
@Slf4j
public class ThreadPoolConsumer<T> {
    private ExecutorService executor;
    private volatile boolean stop = false;
    private final ThreadPoolConsumerBuilder<T> infoHolder;

    //构造器
    public static class ThreadPoolConsumerBuilder<T> {
        int threadCount;
        long intervalMils;
        MQAccessBuilder mqAccessBuilder;
        String exchange;
        String routingKey;
        String queue;
        String type = "direct";
        MessageProcess<T> messageProcess;

        public ThreadPoolConsumerBuilder<T> setThreadCount(int threadCount) {
            this.threadCount = threadCount;

            return this;
        }

        public ThreadPoolConsumerBuilder<T> setIntervalMils(long intervalMils) {
            this.intervalMils = intervalMils;

            return this;
        }

        public ThreadPoolConsumerBuilder<T> setMQAccessBuilder(MQAccessBuilder mqAccessBuilder) {
            this.mqAccessBuilder = mqAccessBuilder;

            return this;
        }

        public ThreadPoolConsumerBuilder<T> setExchange(String exchange) {
            this.exchange = exchange;

            return this;
        }

        public ThreadPoolConsumerBuilder<T> setRoutingKey(String routingKey) {
            this.routingKey = routingKey;

            return this;
        }

        public ThreadPoolConsumerBuilder<T> setQueue(String queue) {
            this.queue = queue;

            return this;
        }

        public ThreadPoolConsumerBuilder<T> setType(String type) {
            this.type = type;

            return this;
        }

        public ThreadPoolConsumerBuilder<T> setMessageProcess(MessageProcess<T> messageProcess) {
            this.messageProcess = messageProcess;

            return this;
        }

        public ThreadPoolConsumer<T> build() {
            return new ThreadPoolConsumer<T>(this);
        }
    }

    private ThreadPoolConsumer(ThreadPoolConsumerBuilder<T> threadPoolConsumerBuilder) {
        this.infoHolder = threadPoolConsumerBuilder;
        executor = Executors.newFixedThreadPool(threadPoolConsumerBuilder.threadCount);
    }

    //1 构造messageConsumer
    //2 执行consume
    public void start() throws IOException {
        for (int i = 0; i < infoHolder.threadCount; i++) {
            //1
            final MessageConsumer messageConsumer = infoHolder.mqAccessBuilder.buildMessageConsumer(infoHolder.exchange,
                    infoHolder.routingKey, infoHolder.queue, infoHolder.messageProcess, infoHolder.type);

            executor.execute(new Runnable() {
                @Override
                public void run() {
                    while (!stop) {
                        try {
                            //2
                            DetailRes detailRes = messageConsumer.consume();

                            if (infoHolder.intervalMils > 0) {
                                try {
                                    Thread.sleep(infoHolder.intervalMils);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                    log.info("interrupt ", e);
                                }
                            }

                            if (!detailRes.isSuccess()) {
                                log.info("run error " + detailRes.getErrMsg());
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            log.info("run exception ", e);
                        }
                    }
                }
            });
        }

        /**
         * jvm中增加一个关闭的钩子，当jvm关闭的时候，会执行系统中已经设置的所有通过方法addShutdownHook添加的钩子，
         * 当系统执行完这些钩子后，jvm才会关闭。
         * 所以这些钩子可以在jvm关闭的时候进行内存清理、对象销毁等操作。
         */
        Runtime.getRuntime().addShutdownHook(new Thread(this::stop));
    }

    public void stop() {

        this.stop = true;

        try {
            Thread.sleep(Constants.ONE_SECOND);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
