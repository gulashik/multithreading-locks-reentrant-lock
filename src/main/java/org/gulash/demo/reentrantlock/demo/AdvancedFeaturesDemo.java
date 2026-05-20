package org.gulash.demo.reentrantlock.demo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.locks.ReentrantLock;

/**
 * <h3>Концепция Reentrancy и инспекция состояния</h3>
 *
 * Слово "Reentrant" в названии означает "повторно входимый". Это значит, что если поток уже
 * удерживает этот замок, он может захватить его снова без блокировки самого себя.
 *
 * <p><b>Зачем это нужно:</b></p>
 * Позволяет вызывать один синхронизированный метод из другого синхронизированного метода
 * того же объекта (или другого объекта, использующего тот же замок), не боясь самоблокировки.
 *
 * <p><b>Счетчик удержаний (Hold Count):</b></p>
 * Каждый раз, когда поток вызывает {@code lock()}, счетчик увеличивается. 
 * Каждый раз при {@code unlock()} — уменьшается. 
 * Замок освобождается для других потоков только когда счетчик станет равен 0.
 *
 */
public class AdvancedFeaturesDemo {
    private static final Logger log = LoggerFactory.getLogger(AdvancedFeaturesDemo.class);

    // Включаем режим честности для демонстрации. 
    // Очередь будет работать по принципу FIFO.
    private final ReentrantLock lock = new ReentrantLock(true);

    public void outerMethod() {
        lock.lock();
        try {
            log.info("Внешний метод. Hold Count: {}", lock.getHoldCount());
            innerMethod();
            log.info("Снова внешний метод после внутреннего. Hold Count: {}", lock.getHoldCount());
        } finally {
            lock.unlock();
            log.info("Внешний метод завершен. Hold Count после unlock: {}", lock.getHoldCount());
        }
    }

    private void innerMethod() {
        lock.lock();
        try {
            log.info("Внутренний метод. Hold Count: {}", lock.getHoldCount());
        } finally {
            lock.unlock();
            log.info("Внутренний метод завершил unlock. Hold Count: {}", lock.getHoldCount());
        }
    }

    /**
     * Демонстрация методов инспекции.
     * Полезно для отладки или мониторинга состояния системы.
     */
    public void showLockStatus() {
        log.info("Is Fair: {}", lock.isFair());
        log.info("Is Locked: {}", lock.isLocked());
        log.info("Is Locked by current thread: {}", lock.isHeldByCurrentThread());
        log.info("Queued threads estimate: {}", lock.getQueueLength());
        log.info("Has queued threads: {}", lock.hasQueuedThreads());
    }
}
