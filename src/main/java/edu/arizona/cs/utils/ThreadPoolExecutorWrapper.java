package edu.arizona.cs.utils;

import sun.nio.ch.ThreadPool;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by savan on 5/2/17.
 */
public class ThreadPoolExecutorWrapper {
    private static ThreadPoolExecutorWrapper sInstance;

    private ThreadPoolExecutor threadPoolExecutor;

    private ThreadPoolExecutorWrapper() {

    }

    public static ThreadPoolExecutorWrapper getInstance() {
        if(sInstance == null) {
            synchronized (ThreadPoolExecutorWrapper.class) {
                if(sInstance == null) {
                    sInstance = new ThreadPoolExecutorWrapper();
                }
            }
        }
        return sInstance;
    }

    public ThreadPoolExecutor getThreadPoolExecutor() {
        if(threadPoolExecutor != null) {
            return threadPoolExecutor;
        }
        threadPoolExecutor = createThreadPoolExecutor();
        return threadPoolExecutor;
    }

    private ThreadPoolExecutor createThreadPoolExecutor() {
        ThreadPoolExecutor threadPoolExecutor;
        threadPoolExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(
                10,
                new ThreadFactoryImpl("Feature extraction threads", Thread.MAX_PRIORITY-3));
        threadPoolExecutor.setCorePoolSize(10);
        threadPoolExecutor.setKeepAliveTime(1, TimeUnit.MINUTES);
        threadPoolExecutor.prestartAllCoreThreads();

        return threadPoolExecutor;
    }

    private static final class ThreadFactoryImpl implements ThreadFactory {

        private String threadName;
        private int threadPriority;

        public ThreadFactoryImpl(String threadName, int threadPriority) {
            this.threadName = threadName;
            this.threadPriority = threadPriority;
        }

        public Thread newThread(Runnable r) {
            Thread t = new Thread(r);
            t.setName(threadName);
            t.setPriority(threadPriority);
            return t;
        }
    }

}
