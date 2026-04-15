package com.agentbanking.isoadapter.infrastructure.external;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicInteger;

public class StanGenerator {

    private static final AtomicInteger counter = new AtomicInteger(0);
    private static final int MAX_STAN = 999999;

    public static String nextStan() {
        int count = counter.incrementAndGet();
        if (count > MAX_STAN) {
            synchronized (counter) {
                counter.set(1);
                count = 1;
            }
        }
        String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("HHmmss"));
        return String.format("%s%06d", datePart, count);
    }

    public static String currentStan() {
        int count = counter.get();
        String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("HHmmss"));
        return String.format("%s%06d", datePart, count);
    }
}