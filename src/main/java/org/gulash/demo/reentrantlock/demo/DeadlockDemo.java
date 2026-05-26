package org.gulash.demo.reentrantlock.demo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;


public class DeadlockDemo {
    private static final Logger log = LoggerFactory.getLogger(DeadlockDemo.class);


    public static void main(String[] args) throws InterruptedException {
        final Friend personA = new Friend("Max");
        final Friend personB = new Friend("Olga");

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

class Friend {
    private static final Logger log = LoggerFactory.getLogger(Friend.class);

    private final ReentrantLock lock = new ReentrantLock();
    private final String name;

    public Friend(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    public void bow(Friend bower) {
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

    public void bowBack(Friend bower) {
        try {
            Thread.sleep(ThreadLocalRandom.current().nextInt(1, 100));
            log.info("{}: {} has bowed back to me!", this.name, bower.getName());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
