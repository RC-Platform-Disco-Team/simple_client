package client;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

import us.monoid.web.Resty;

/**
 * @author Maxim Neverov
 */
public class SimpleClient {

    private static final int nThreads = 3;

    public static void main(String[] args) throws Exception {
        final CountDownLatch startGate = new CountDownLatch(1);
        final CountDownLatch endGate = new CountDownLatch(nThreads);
        String port;
        if (args.length != 1) {
            port = "9000";
        } else {
            port = args[0];
        }
        System.out.println("connecting with: localhost/" + port);
        Set<String> globalIds = new HashSet<>();

        for (int i = 0; i < nThreads; i++) {
            Thread t = new Thread(() -> {
                try {
                    startGate.await();
                    try {
                        globalIds.addAll(doWork(port));
                    } finally {
                        endGate.countDown();
                    }
                } catch (InterruptedException ignored) {
                    ignored.printStackTrace();
                }
            });
            t.start();
        }
        startGate.countDown();
        endGate.await();
        print(globalIds, port);
        System.out.println("done");
    }

    private static void print(Set<String> ids, String port) {
        Path path = Paths.get(port + ".log");
        try (BufferedWriter writer = Files.newBufferedWriter(path, StandardOpenOption.CREATE)) {
            for (String num : ids) {
                try {
                    writer.write(num + "\n");
                } catch (IOException ignored) {
                    ignored.printStackTrace();
                }
            }
        } catch (IOException ignored) {
            ignored.printStackTrace();
        }
    }

    private static Set<String> doWork(String port) {
        Resty r = new Resty();
        int i = 0;

        Set<String> ids = new HashSet<>(500);
        while (i < 500) {
            try {
                ids.add(r.text("http://localhost:" + port + "/proxy").toString());
            } catch (Exception ignored) {
                ignored.printStackTrace();
            }
            i++;
            if (i % 10 == 0) {
                System.out.print(".");
            }
        }
        return ids;
    }
}
