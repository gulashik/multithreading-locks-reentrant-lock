package org.gulash.demo.reentrantlock.demo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;


public class DeadlockDemo {
    private static final Logger log = LoggerFactory.getLogger(DeadlockDemo.class);


    public static void main(String[] args) throws InterruptedException {
        final ReentrantLock lockA = new ReentrantLock();
        final ReentrantLock lockB = new ReentrantLock();

        final Friend personA = new Friend("Max", lockA, lockB);
        final Friend personB = new Friend("Olga", lockB, lockA);

        final Thread threadA = new Thread(() -> {
            log.info(Thread.currentThread().getName() + " work started");
            personA.bow(personB);
            log.info(Thread.currentThread().getName() + " work finished");
        });
        threadA.start();

        final Thread threadB = new Thread(() -> {
            log.info(Thread.currentThread().getName() + " work started");
            personB.bow(personA);
            log.info(Thread.currentThread().getName() + " work finished");
        });
        threadB.start();

        threadA.join();
        threadB.join();
    }


}

class Friend {
    private static final Logger log = LoggerFactory.getLogger(Friend.class);

    private final ReentrantLock lockThis;
    private final ReentrantLock lockOther;

    private final String name;

    public Friend(String name, ReentrantLock lockThis, ReentrantLock lockOther) {
        this.name = name;
        this.lockThis = lockThis;
        this.lockOther = lockOther;
    }

    public String getName() {
        return this.name;
    }

    public void bow(Friend bower) {
        while (true) {
            boolean lockAcquiredThis = false;
            boolean lockAcquiredOther = false;
            try {

                log.info("Waiting for locks");
                lockAcquiredThis = lockThis.tryLock(ThreadLocalRandom.current().nextInt(1, 30), TimeUnit.MILLISECONDS);
                lockAcquiredOther = lockOther.tryLock(ThreadLocalRandom.current().nextInt(1, 3), TimeUnit.MILLISECONDS);
                log.info("Locking acquired this: {}, other: {}", lockAcquiredThis, lockAcquiredOther);

                if (!lockAcquiredThis || !lockAcquiredOther) continue;

                log.info("{}: {}" + "  has bowed to me!", this.name, bower.getName());
                bower.bowBack(this);

                return;

            } catch (InterruptedException e) {
                log.info("Interrupted while waiting for locks");
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                log.error("Unexpected error while waiting for locks", e);
            } finally {
                log.info("Unlocking locks this: {}, other: {}", lockAcquiredThis, lockAcquiredOther);
                if (lockAcquiredThis) lockThis.unlock();
                if (lockAcquiredOther) lockOther.unlock();
            }
        }
    }

    public void bowBack(Friend bower) throws InterruptedException {
        Thread.sleep(ThreadLocalRandom.current().nextInt(1, 201));
        log.info("{}: {}" + " has bowed back to me!", this.name, bower.getName());
    }
}
