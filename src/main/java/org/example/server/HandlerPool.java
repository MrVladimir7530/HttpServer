package org.example.server;

import java.util.Collection;
import java.util.concurrent.ArrayBlockingQueue;

public class HandlerPool<E> extends ArrayBlockingQueue<E> {
    public HandlerPool(int capacity) {
        super(capacity);
    }

    public HandlerPool(int capacity, boolean fair) {
        super(capacity, fair);
    }

    public HandlerPool(int capacity, boolean fair, Collection<? extends E> c) {
        super(capacity, fair, c);
    }
}
