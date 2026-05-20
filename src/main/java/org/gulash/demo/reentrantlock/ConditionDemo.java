package org.gulash.demo.reentrantlock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * <h3>Взаимодействие потоков через Condition</h3>
 *
 * {@link Condition} заменяет использование методов {@code Object.wait()}, {@code notify()} и {@code notifyAll()}.
 * Он привязан к конкретному {@link ReentrantLock} и позволяет организовать несколько очередей ожидания
 * для одного замка.
 *
 * <p><b>Преимущества перед wait/notify:</b></p>
 * <ul>
 *     <li>Наглядность: можно создать несколько условий, например {@code notFull} и {@code notEmpty}.</li>
 *     <li>Безопасность: {@code signal()} будит только те потоки, которые ждут именно этого условия.</li>
 *     <li>Возможность ожидания с таймаутом и прерыванием.</li>
 * </ul>
 *
 * <p><b>Подводные камни:</b></p>
 * <ul>
 *     <li>Всегда вызывайте {@code await()} в цикле {@code while}, проверяющем логическое условие. 
 *     Это защищает от "ложных пробуждений" (spurious wakeups).</li>
 *     <li>Метод {@code await()} автоматически освобождает замок и заново захватывает его при пробуждении.</li>
 * </ul>
 *
 * <pre>{@code
 * ConditionDemo sharedQueue = new ConditionDemo(5);
 * // Один поток вызывает produce(), другой consume()
 * }</pre>
 */
public class ConditionDemo {
    private static final Logger log = LoggerFactory.getLogger(ConditionDemo.class);

    private final ReentrantLock lock = new ReentrantLock();
    // Создаем условия из экземпляра замка
    private final Condition notFull = lock.newCondition();
    private final Condition notEmpty = lock.newCondition();

    private final Object[] buffer;
    private int count, putptr, takeptr;

    public ConditionDemo(int capacity) {
        this.buffer = new Object[capacity];
    }

    /**
     * Помещает элемент в буфер. Если буфер полон, поток засыпает.
     */
    public void produce(Object x) throws InterruptedException {
        lock.lock();
        try {
            // Использование while - критически важная практика
            while (count == buffer.length) {
                log.info("Буфер полон. Поток-производитель ждет...");
                notFull.await(); // Освобождает замок и ждет сигнала
            }
            buffer[putptr] = x;
            if (++putptr == buffer.length) putptr = 0;
            ++count;
            log.info("Произведено: {}. Элементов в буфере: {}", x, count);

            // Сигнализируем потребителям, что данные появились
            notEmpty.signal();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Извлекает элемент из буфера. Если буфер пуст, поток засыпает.
     */
    public Object consume() throws InterruptedException {
        lock.lock();
        try {
            while (count == 0) {
                log.info("Буфер пуст. Поток-потребитель ждет...");
                notEmpty.await();
            }
            Object x = buffer[takeptr];
            if (++takeptr == buffer.length) takeptr = 0;
            --count;
            log.info("Потреблено: {}. Элементов в буфере: {}", x, count);

            // Сигнализируем производителям, что место освободилось
            notFull.signal();
            return x;
        } finally {
            lock.unlock();
        }
    }
}
