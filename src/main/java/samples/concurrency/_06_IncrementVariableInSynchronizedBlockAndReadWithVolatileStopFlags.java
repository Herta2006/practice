package samples.concurrency;

public class _06_IncrementVariableInSynchronizedBlockAndReadWithVolatileStopFlags {
    private static int counter;
    private static volatile boolean finish1;
    private static volatile boolean finish2;
    private static Object lock = new Object();

    private static void inc() {
        synchronized (lock) {
            int tmp = counter;
            counter = tmp + 1;
        } ;
    }

    private static int get() {
        return counter;
    }

    public static void main(String[] args) throws InterruptedException {
        // T0
        new Thread(() -> {
            // T1
            for (int i = 0; i < 10_000_000; i++) inc();
            finish1 = true;
        }).start();
        new Thread(() -> {
            // T2
            for (int i = 0; i < 10_000_000; i++) inc();
            finish2 = true;
        }).start();
        while (!finish1 || !finish2) System.out.println(get());
    }
}
