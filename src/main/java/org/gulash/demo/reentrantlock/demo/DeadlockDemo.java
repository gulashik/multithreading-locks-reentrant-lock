package org.gulash.demo.reentrantlock.demo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;


/**
 * Демонстрация предотвращения взаимной блокировки (deadlock) с использованием {@link ReentrantLock#tryLock()}.
 * <p>
 * В классическом примере с "кланяющимися друзьями" deadlock возникает, если два потока
 * одновременно пытаются захватить мониторы друг друга. Использование tryLock с таймаутом
 * позволяет избежать вечного ожидания, отпуская уже захваченные замки при неудаче.
 */
public class DeadlockDemo {
    private static final Logger log = LoggerFactory.getLogger(DeadlockDemo.class);

    /**
     * Точка входа в демонстрацию.
     * Создает двух "друзей" и запускает два потока, имитирующих их взаимодействие.
     */
    public static void main(String[] args) throws InterruptedException {
        // Lock храниться локально в каждом
//        final FriendSelfLock personA = new FriendSelfLock("Max");
//        final FriendSelfLock personB = new FriendSelfLock("Olga");

        // Если сами делаем lock
        final ReentrantLock lock = new ReentrantLock();
        final Friend personA = new Friend("Max", lock);
        final Friend personB = new Friend("Olga", lock);

        final Thread threadA = new Thread(() -> {
            log.info("{} work started", Thread.currentThread().getName());
            personA.bow(personB);
            log.info("{} work finished", Thread.currentThread().getName());
        });
        threadA.start();

        final Thread threadB = new Thread(() -> {
            log.info("{} work started", Thread.currentThread().getName());
            personB.bow(personA);
            log.info("{} work finished", Thread.currentThread().getName());
        });
        threadB.start();

        threadA.join();
        threadB.join();
    }
}

/**
 * Класс, где каждый объект имеет свой собственный замок.
 * Используется для демонстрации захвата нескольких замков.
 */
class FriendSelfLock {
    private static final Logger log = LoggerFactory.getLogger(Friend.class);

    private final ReentrantLock lock = new ReentrantLock();
    private final String name;

    public FriendSelfLock(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    /**
     * Выполняет "поклон" другому другу.
     * Пытается захватить как свой замок, так и замок оппонента.
     * Если не удается захватить оба, освобождает захваченные и пробует снова.
     *
     * @param bower Друг, которому кланяемся.
     */
    public void bow(FriendSelfLock bower) {
        while (true) {
            boolean lockAcquiredThis = false;
            boolean lockAcquiredOther = false;
            try {
                log.info("{} is waiting for locks", this.name);
                lockAcquiredThis = this.lock.tryLock(ThreadLocalRandom.current().nextInt(1, 50), TimeUnit.MILLISECONDS);
                lockAcquiredOther = bower.lock.tryLock(ThreadLocalRandom.current().nextInt(1, 50), TimeUnit.MILLISECONDS);

                if (lockAcquiredThis && lockAcquiredOther) {
                    log.info("Locks acquired by {}: this: {}, other: {}", this.name, lockAcquiredThis, lockAcquiredOther);
                    log.info("{}: {} has bowed to me!", this.name, bower.getName());
                    bower.bowBack(this);
                    return; // Exit the loop
                } else {
                    log.warn("Could not acquire both locks for {}, retrying...", this.name);
                }

            } catch (InterruptedException e) {
                log.info("Interrupted while waiting for locks");
                Thread.currentThread().interrupt();
                return; // Exit the loop
            } finally {
                if (lockAcquiredOther) bower.lock.unlock();
                if (lockAcquiredThis) this.lock.unlock();
            }
        }
    }

    /**
     * Ответный поклон. Вызывается внутри bow под защитой замков.
     *
     * @param bower Друг, который кланяется в ответ.
     */
    public void bowBack(FriendSelfLock bower) {
        try {
            Thread.sleep(ThreadLocalRandom.current().nextInt(1, 100));
            log.info("{}: {} has bowed back to me!", this.name, bower.getName());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

/**
 * Класс, использующий внешний (общий) замок.
 */
class Friend {
    private static final Logger log = LoggerFactory.getLogger(Friend.class);
    /** Общий замок*/
    private final ReentrantLock lock;
    private final String name;

    public Friend(String name, ReentrantLock lock) {
        this.name = name;
        this.lock = lock;
    }

    public String getName() {
        return this.name;
    }

    /**
     * Выполняет поклон под защитой общего замка.
     *
     * @param bower Друг, которому кланяемся.
     */
    public void bow(Friend bower) {
        while (true) {
            boolean lockAcquired = false;
            try {
                log.info("{} is waiting for locks", this.name);
                lockAcquired = this.lock.tryLock(ThreadLocalRandom.current().nextInt(1, 50), TimeUnit.MILLISECONDS);

                if (lockAcquired) {
                    log.info("Locks acquired by {}", this.name);
                    log.info("{}: {} has bowed to me!", this.name, bower.getName());
                    bower.bowBack(this);
                    return; // Exit the loop
                } else {
                    log.warn("Could not acquire both locks for {}, retrying...", this.name);
                }

            } catch (InterruptedException e) {
                log.info("Interrupted while waiting for locks");
                Thread.currentThread().interrupt();
                return; // Exit the loop
            } finally {
                if (lockAcquired) bower.lock.unlock();
            }
        }
    }

    /**
     * Ответный поклон под общим замком.
     *
     * @param bower Друг, который кланяется в ответ.
     */
    public void bowBack(Friend bower) {
        try {
            Thread.sleep(ThreadLocalRandom.current().nextInt(1, 100));
            log.info("{}: {} has bowed back to me!", this.name, bower.getName());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
