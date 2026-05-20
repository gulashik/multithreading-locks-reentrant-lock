package org.gulash.demo.reentrantlock;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Тесты, демонстрирующие важные аспекты и типичные ошибки при работе с ReentrantLock.
 */
class ReentrantLockPitfallsTest {
    private static final Logger log = LoggerFactory.getLogger(ReentrantLockPitfallsTest.class);

    @Test
    @DisplayName("Демонстрация lockInterruptibly: возможность прервать поток, ждущий замок")
    void testLockInterruptibly() throws InterruptedException {
        ReentrantLock lock = new ReentrantLock();
        lock.lock(); // Захватываем замок в главном потоке

        AtomicBoolean wasInterrupted = new AtomicBoolean(false);
        Thread waitingThread = new Thread(() -> {
            try {
                log.info("Поток пытается захватить замок через lockInterruptibly()...");
                lock.lockInterruptibly();
                try {
                    log.info("Замок захвачен (не должно произойти)");
                } finally {
                    lock.unlock();
                }
            } catch (InterruptedException e) {
                log.info("Поток был успешно прерван во время ожидания замка!");
                wasInterrupted.set(true);
            }
        });

        waitingThread.start();
        Thread.sleep(100);
        waitingThread.interrupt(); // Прерываем поток
        waitingThread.join(1000);

        assertTrue(wasInterrupted.get(), "Поток должен был поймать InterruptedException");
        assertTrue(lock.isLocked(), "Замок все еще должен быть у главного потока");
        lock.unlock();
    }

    @Test
    @DisplayName("Подводный камень: IllegalMonitorStateException при unlock() без владения замком")
    void testUnlockWithoutLock() {
        ReentrantLock lock = new ReentrantLock();
        assertThrows(IllegalMonitorStateException.class, lock::unlock,
                "Вызов unlock() без предварительного lock() должен бросать исключение");
    }

    @Test
    @DisplayName("Демонстрация справедливости (Fairness): FIFO порядок")
    void testFairness() throws InterruptedException {
        // Справедливый замок гарантирует, что потоки получат доступ в порядке очереди
        ReentrantLock fairLock = new ReentrantLock(true);
        int threadCount = 3;
        CountDownLatch readyLatch = new CountDownLatch(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        StringBuilder executionOrder = new StringBuilder();

        fairLock.lock(); // Главный поток занимает замок

        for (int i = 0; i < threadCount; i++) {
            final int id = i;
            new Thread(() -> {
                readyLatch.countDown();
                try {
                    startLatch.await(); // Ждем общей команды "старт"
                    fairLock.lock();
                    try {
                        executionOrder.append(id);
                        log.info("Thread {} acquired lock", id);
                    } finally {
                        fairLock.unlock();
                    }
                } catch (InterruptedException ignored) {}
            }).start();
            Thread.sleep(50); // Небольшая задержка, чтобы гарантировать порядок постановки в очередь
        }

        readyLatch.await();
        startLatch.countDown();
        Thread.sleep(100);
        fairLock.unlock(); // Отпускаем, чтобы пошла цепная реакция

        Thread.sleep(200); // Даем всем отработать
        log.info("Order of acquisition: {}", executionOrder);
        assertEquals("012", executionOrder.toString(), "Потоки должны были захватить замок в порядке очереди (0, 1, 2)");
    }

    @Test
    @DisplayName("Демонстрация Reentrancy: повторный захват тем же потоком")
    void testReentrancy() {
        ReentrantLock lock = new ReentrantLock();
        lock.lock();
        try {
            assertEquals(1, lock.getHoldCount());
            lock.lock();
            try {
                assertEquals(2, lock.getHoldCount());
            } finally {
                lock.unlock();
            }
            assertEquals(1, lock.getHoldCount());
        } finally {
            lock.unlock();
        }
        assertEquals(0, lock.getHoldCount());
    }
}
