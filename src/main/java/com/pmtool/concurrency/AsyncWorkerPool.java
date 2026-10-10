package com.pmtool.concurrency;

import com.pmtool.config.AppConfig;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

public final class AsyncWorkerPool {

    private static final Logger LOGGER = Logger.getLogger(AsyncWorkerPool.class.getName());
    private static volatile AsyncWorkerPool instance;
    private final ThreadPoolExecutor executor;

    private AsyncWorkerPool() {
        int coreThreads = AppConfig.getInt("worker.pool.coreThreads", 4);
        int maxThreads = AppConfig.getInt("worker.pool.maxThreads", 8);
        int queueCapacity = AppConfig.getInt("worker.pool.queueCapacity", 500);

        BlockingQueue<Runnable> workQueue = new LinkedBlockingQueue<>(queueCapacity);
        ThreadFactory daemonFactory = new ThreadFactory() {
            private final AtomicInteger threadIndex = new AtomicInteger(1);
            @Override public Thread newThread(Runnable r) {
                Thread t = new Thread(r, "PMTool-Worker-" + threadIndex.getAndIncrement());
                t.setDaemon(true);
                return t;
            }
        };

        this.executor = new ThreadPoolExecutor(coreThreads, maxThreads, 60L, TimeUnit.SECONDS, workQueue, daemonFactory, new ThreadPoolExecutor.CallerRunsPolicy());
        LOGGER.info(String.format("AsyncWorkerPool initialized (core: %d, max: %d, queue: %d)", coreThreads, maxThreads, queueCapacity));
    }

    public static AsyncWorkerPool getInstance() {
        if (instance == null) {
            synchronized (AsyncWorkerPool.class) {
                if (instance == null) instance = new AsyncWorkerPool();
            }
        }
        return instance;
    }

    public void execute(Runnable task) { if (task != null) executor.execute(task); }
    public <T> Future<T> submit(Callable<T> callable) { return executor.submit(callable); }
    public int getActiveCount() { return executor.getActiveCount(); }
    public long getCompletedTaskCount() { return executor.getCompletedTaskCount(); }

    public void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) executor.shutdownNow();
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
