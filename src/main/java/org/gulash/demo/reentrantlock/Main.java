package org.gulash.demo.reentrantlock;

import org.gulash.demo.reentrantlock.demo.AdvancedFeaturesDemo;
import org.gulash.demo.reentrantlock.demo.BasicLockDemo;
import org.gulash.demo.reentrantlock.demo.ConditionDemo;
import org.gulash.demo.reentrantlock.demo.TryLockDemo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * Главный класс для запуска демонстраций.
 */
public class Main {
    private static final Logger log = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) throws InterruptedException {
        log.info("Начало демонстрации ReentrantLock");

        // 1. Базовый пример
        log.info("--- 1. Базовый пример ---");
        BasicLockDemo basic = new BasicLockDemo();
        Thread basicLockThread = new Thread(basic::performSafeAction);

        basic.manualLock();
        //--
        basicLockThread.start();
        //--
        Thread.sleep(100);
        basic.manualUnlock();

        basicLockThread.join();

        // 2. TryLock
        log.info("--- 2. TryLock пример ---");
        TryLockDemo tryLock = new TryLockDemo();
        tryLock.tryPerformTask();
        
        // Поток-преграда
        Thread blocker = new Thread(() -> tryLock.lockForDuration(1000), "BlockerThread");
        blocker.start();
        Thread.sleep(50); // Даем время захватить
        
        tryLock.tryPerformTask(); // Должен не зайти
        tryLock.tryPerformTaskWithTimeout(500, TimeUnit.MILLISECONDS); // Должен выйти по таймауту

        // Дождемся освобождения для чистоты эксперимента
        blocker.join();
        log.info("Блокирующий поток завершил работу.");
        tryLock.tryPerformTask(); // Теперь должен успешно захватить

        // 3. Reentrancy
        log.info("--- 3. Reentrancy пример ---");
        AdvancedFeaturesDemo advanced = new AdvancedFeaturesDemo();
        advanced.outerMethod();
        advanced.showLockStatus();

        // 4. Condition
        log.info("--- 4. Condition пример (Producer-Consumer) ---");
        ConditionDemo sharedBuffer = new ConditionDemo(2);

        Thread producer = new Thread(() -> {
            try {
                for (int i = 1; i <= 5; i++) {
                    sharedBuffer.produce("Item-" + i);
                    Thread.sleep(50);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "Producer");

        Thread consumer = new Thread(() -> {
            try {
                for (int i = 1; i <= 5; i++) {
                    sharedBuffer.consume();
                    Thread.sleep(300); // Потребитель медленнее
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "Consumer");

        producer.start();
        consumer.start();

        producer.join();
        consumer.join();

        log.info("Демонстрация завершена. См. тесты для более глубокого погружения.");
    }
}
