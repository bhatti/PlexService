package com.plexobject.stock;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.plexobject.handler.ResponseDispatcher;

public class QuoteStreamer extends TimerTask {
    private static final Logger log = LoggerFactory
            .getLogger(QuoteStreamer.class);

    private int delay = 1000;
    private Map<String, Collection<ResponseDispatcher>> subscribers = new ConcurrentHashMap<>();
    private QuoteCache quoteCache = new QuoteCache();
    private final Timer timer = new Timer(true);

    public QuoteStreamer() {
        timer.schedule(this, delay, delay);
    }

    public void add(String symbol, ResponseDispatcher dispatcher) {
        symbol = symbol.toUpperCase();
        synchronized (symbol.intern()) {
            Collection<ResponseDispatcher> dispatchers = subscribers
                    .get(symbol);
            if (dispatchers == null) {
                dispatchers = new HashSet<ResponseDispatcher>();
                subscribers.put(symbol, dispatchers);
            }
            dispatchers.add(dispatcher);
            log.info("Adding subscription for " + symbol + ", dispatcher "
                    + dispatcher);
        }
    }

    public void remove(String symbol, ResponseDispatcher dispatcher) {
        symbol = symbol.toUpperCase();
        synchronized (symbol.intern()) {
            Collection<ResponseDispatcher> dispatchers = subscribers
                    .get(symbol);
            if (dispatchers != null) {
                dispatchers.remove(dispatcher);
                log.info("Removing subscription for " + symbol
                        + ", dispatcher " + dispatcher);
            }
        }
    }

    @Override
    public void run() {
        if (subscribers.size() == 0) {
            return;
        }
        for (Map.Entry<String, Collection<ResponseDispatcher>> e : subscribers
                .entrySet()) {
            Quote q = quoteCache.getLatestQuote(e.getKey());
            Collection<ResponseDispatcher> dispatchers = new ArrayList<>(
                    e.getValue());
            for (ResponseDispatcher d : dispatchers) {
                try {
                    d.send(q);
                    // log.info("Sending " + q + " to " + d);
                } catch (Exception ex) {
                    log.error(
                            "Failed to stream, removing it from future publications",
                            ex);
                    remove(e.getKey(), d);
                }
            }
        }
    }
}