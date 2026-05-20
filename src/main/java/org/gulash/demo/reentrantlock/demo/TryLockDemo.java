package org.gulash.demo.reentrantlock.demo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * <h3>Использование tryLock для избежания блокировок</h3>
 *
 * {@code tryLock()} — это мощный инструмент, который позволяет потоку попытаться захватить замок,
 * не засыпая на неопределенный срок.
 *
 * <p><b>Варианты использования:</b></p>
 * <ul>
 *     <li>Мгновенная попытка: {@code lock.tryLock()} — возвращает {@code true}, если замок свободен.</li>
 *     <li>Попытка с таймаутом: {@code lock.tryLock(5, TimeUnit.SECONDS)} — ждет указанное время.</li>
 * </ul>
 *
 * <p><b>Где это полезно:</b></p>
 * <ul>
 *     <li>Предотвращение Deadlock: если поток не может захватить все нужные замки, он может освободить уже захваченные и попробовать снова.</li>
 *     <li>UI приложения: чтобы не "фриизить" интерфейс, если ресурс занят долгой фоновой задачей.</li>
 *     <li>Сценарии "выполнить если возможно": например, фоновая очистка кэша.</li>
 * </ul>
 */
public class TryLockDemo {
    private static final Logger log = LoggerFactory.getLogger(TryLockDemo.class);
    private final ReentrantLock lock = new ReentrantLock();

    /**
     * Пытается выполнить задачу, если замок свободен.
     */
    public void tryPerformTask() {
        log.info("Пытаюсь захватить замок мгновенно...");
        if (lock.tryLock()) {
            try {
                log.info("Успех! Работаю...");
                Thread.sleep(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                lock.unlock();
                log.info("Замок освобожден.");
            }
        } else {
            log.warn("Замок занят другим потоком. Ухожу делать другие дела.");
        }
    }

    /**
     * Пытается захватить замок в течение заданного времени.
     */
    public boolean tryPerformTaskWithTimeout(long timeout, TimeUnit unit) throws InterruptedException {
        log.info("Пытаюсь захватить замок с таймаутом {} {}...", timeout, unit);
        
        // ВАЖНО: tryLock(timeout, unit) реагирует на Thread.interrupt()
        if (lock.tryLock(timeout, unit)) {
            try {
                log.info("Замок захвачен после ожидания.");
                Thread.sleep(100);
                return true;
            } finally {
                lock.unlock();
                log.info("Замок освобожден.");
            }
        } else {
            log.warn("Не удалось дождаться освобождения замка.");
            return false;
        }
    }

    // Для демонстрации займем замок снаружи на время
    public void lockForDuration(long durationMs) {
        lock.lock();
        try {
            log.info("Замок захвачен потоком {} на {} мс", Thread.currentThread().getName(), durationMs);
            Thread.sleep(durationMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            lock.unlock();
            log.info("Замок освобожден потоком {}", Thread.currentThread().getName());
        }
    }
}
