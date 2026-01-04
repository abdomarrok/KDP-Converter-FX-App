package com.boilerplate.app.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Singleton service for managing application-wide thread pools.
 * Prevents resource leaks and ensures graceful shutdown.
 */
public class ThreadPoolService {
    private static final Logger logger = LogManager.getLogger(ThreadPoolService.class);
    private static ThreadPoolService instance;

    // For CPU-intensive tasks (PDF generation, JSON parsing)
    private final ExecutorService computeExecutor;

    // For IO-intensive tasks (Image downloading, DB operations)
    private final ExecutorService ioExecutor;

    // Scheduled tasks (Periodic cleanup)
    private final ScheduledExecutorService scheduledExecutor;

    private ThreadPoolService() {
        int cores = Runtime.getRuntime().availableProcessors();
        logger.info("Initializing ThreadPoolService with {} compute threads", cores);

        this.computeExecutor = Executors.newFixedThreadPool(cores, new NamedThreadFactory("compute-pool"));
        this.ioExecutor = Executors.newCachedThreadPool(new NamedThreadFactory("io-pool")); // Expandable
        this.scheduledExecutor = Executors.newSingleThreadScheduledExecutor(new NamedThreadFactory("scheduler"));
    }

    public static synchronized ThreadPoolService getInstance() {
        if (instance == null) {
            instance = new ThreadPoolService();
        }
        return instance;
    }

    public ExecutorService getComputeExecutor() {
        return computeExecutor;
    }

    public ExecutorService getIoExecutor() {
        return ioExecutor;
    }

    public ScheduledExecutorService getScheduledExecutor() {
        return scheduledExecutor;
    }

    public java.util.concurrent.Future<?> submitCompute(Runnable task) {
        return computeExecutor.submit(task);
    }

    public <T> Future<T> submitCompute(Callable<T> task) {
        return computeExecutor.submit(task);
    }

    public java.util.concurrent.Future<?> submitIo(Runnable task) {
        return ioExecutor.submit(task);
    }

    public <T> Future<T> submitIo(Callable<T> task) {
        return ioExecutor.submit(task);
    }

    /**
     * Gracefully shutdown all thread pools.
     */
    public void shutdown() {
        logger.info("Shutting down ThreadPoolService...");
        shutdownPool(computeExecutor, "Compute");
        shutdownPool(ioExecutor, "IO");
        shutdownPool(scheduledExecutor, "Scheduler");
    }

    private void shutdownPool(ExecutorService pool, String name) {
        try {
            pool.shutdown();
            if (!pool.awaitTermination(5, TimeUnit.SECONDS)) {
                logger.warn("{} pool did not terminate in time, forcing shutdown...", name);
                pool.shutdownNow();
            }
        } catch (InterruptedException e) {
            logger.error("{} pool shutdown interrupted", name);
            pool.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    // Helper for named threads
    private static class NamedThreadFactory implements ThreadFactory {
        private final String prefix;
        private final AtomicInteger counter = new AtomicInteger(1);

        public NamedThreadFactory(String prefix) {
            this.prefix = prefix;
        }

        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, prefix + "-" + counter.getAndIncrement());
            t.setDaemon(true); // Allow app to exit even if tasks are running
            return t;
        }
    }
}
