package org.gulash.demo.reentrantlock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.locks.ReentrantLock;

/**
 * <h3>Базовое использование ReentrantLock</h3>
 *
 * {@link ReentrantLock} — это расширенная альтернатива блоку {@code synchronized}.
 * Он реализует интерфейс {@link java.util.concurrent.locks.Lock} и предоставляет более гибкие механизмы
 * управления блокировками.
 *
 * <p><b>Основные отличия от {@code synchronized}:</b></p>
 * <ul>
 *     <li>Возможность попытки захвата блокировки без бесконечного ожидания ({@code tryLock}).</li>
 *     <li>Возможность прерывания ожидания захвата блокировки ({@code lockInterruptibly}).</li>
 *     <li>Поддержка "справедливости" (Fairness policy).</li>
 *     <li>Возможность использования нескольких условий ({@link java.util.concurrent.locks.Condition}) на один замок.</li>
 * </ul>
 *
 * <p><b>Критически важно:</b> Захват блокировки должен сопровождаться блоком {@code try-finally},
 * где в блоке {@code finally} вызывается {@code unlock()}. Если этого не сделать, блокировка никогда
 * не будет освобождена в случае исключения, что приведет к Deadlock всего приложения.</p>
 *
 * <pre>{@code
 * BasicLockDemo demo = new BasicLockDemo();
 * demo.performSafeAction();
 * }</pre>
 *
 * @see ReentrantLock
 */
public class BasicLockDemo {
    private static final Logger log = LoggerFactory.getLogger(BasicLockDemo.class);

    // ReentrantLock по умолчанию "нечестный" (non-fair). 
    // Это дает лучшую производительность за счет того, что новый поток может "проскочить" 
    // перед потоками, которые уже ждут в очереди, если замок освободился в нужный момент.
    private final ReentrantLock lock = new ReentrantLock();
    private int counter = 0;

    /**
     * Пример безопасного изменения общего ресурса.
     * <p>
     * <b>Best Practice:</b> Вызывайте {@code lock()} ПЕРЕД блоком {@code try}. 
     * Если {@code lock()} выбросит исключение (что маловероятно для обычного захвата, 
     * но важно для {@code lockInterruptibly()}), мы не должны вызывать {@code unlock()} в {@code finally}.
     */
    public void performSafeAction() {
        log.info("Попытка захвата блокировки...");
        lock.lock(); // Поток засыпает здесь, если замок занят другим потоком
        try {
            log.info("Блокировка захвачена. Выполнение критической секции.");
            counter++;
            // Имитация полезной нагрузки
            Thread.sleep(100);
            log.info("Текущее значение счетчика: {}", counter);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Поток был прерван", e);
        } finally {
            // Обязательное освобождение замка
            lock.unlock();
            log.info("Блокировка освобождена.");
        }
    }

    /**
     * Возвращает текущее значение счетчика.
     * Заметьте: чтение тоже должно быть защищено, если мы хотим видеть актуальное значение 
     * в многопоточной среде (или поле должно быть volatile, но замок уже обеспечивает Memory Barrier).
     */
    public int getCounter() {
        lock.lock();
        try {
            return counter;
        } finally {
            lock.unlock();
        }
    }
}
