package com.pmtool.concurrency;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class EventBus {

    private static final Logger LOGGER = Logger.getLogger(EventBus.class.getName());
    private static volatile EventBus instance;

    private final Map<Class<?>, List<Consumer<Object>>> listeners = new ConcurrentHashMap<>();
    private final AsyncWorkerPool workerPool;

    private EventBus(AsyncWorkerPool workerPool) {
        this.workerPool = workerPool;
    }

    public static EventBus getInstance() {
        if (instance == null) {
            synchronized (EventBus.class) {
                if (instance == null) instance = new EventBus(AsyncWorkerPool.getInstance());
            }
        }
        return instance;
    }

    @SuppressWarnings("unchecked")
    public <T> void subscribe(Class<T> eventType, Consumer<T> listener) {
        listeners.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>())
                .add(obj -> listener.accept((T) obj));
    }

    public void publishAsync(Object event) {
        if (event == null) return;
        workerPool.execute(() -> publishSync(event));
    }

    public void publishSync(Object event) {
        if (event == null) return;
        List<Consumer<Object>> handlers = listeners.get(event.getClass());
        if (handlers != null) {
            for (Consumer<Object> handler : handlers) {
                try {
                    handler.accept(event);
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Error handling event: " + e.getMessage(), e);
                }
            }
        }
    }
}
