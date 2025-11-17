package utils.schedular;

import akka.actor.ActorSystem;
import akka.actor.Cancellable;
import com.typesafe.config.Config;
import jakarta.annotation.PreDestroy;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.ExecutionContextExecutor;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.*;
import java.time.Instant;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import java.nio.file.attribute.BasicFileAttributes;

@Singleton
public class TempFileCleaner {

    private static final Logger log = LoggerFactory.getLogger(TempFileCleaner.class);
    private final Cancellable cancellable;
    private final Path dir;
    private final long expiryMinutes;
    private final String filePattern; // regex

    @Inject
    public TempFileCleaner(ActorSystem actorSystem,
                           ExecutionContextExecutor executor,
                           Config config) {
        // read values from application.conf (with sensible defaults)
        String tmpDir = config.hasPath("tempcleaner.dir") ? config.getString("tempcleaner.dir") : "/tmp";
        this.expiryMinutes = config.hasPath("tempcleaner.expiryMinutes") ? config.getLong("tempcleaner.expiryMinutes") : 10L;
        long intervalMinutes = config.hasPath("tempcleaner.intervalMinutes") ? config.getLong("tempcleaner.intervalMinutes") : 5L;
        this.filePattern = config.hasPath("tempcleaner.pattern") ? config.getString("tempcleaner.pattern") : "^\\+~JF.*\\.tmp$";

        this.dir = Paths.get(tmpDir);

        Duration initialDelay = Duration.ofSeconds(0);
        Duration interval = Duration.ofMinutes(intervalMinutes);

        Runnable task = () -> {
            try {
                clean();
            } catch (Exception e) {
                // log and keep scheduler alive
                System.err.println("TempFileCleaner: error during clean: " + e.getMessage());
                e.printStackTrace();
            }
        };

        this.cancellable = actorSystem.scheduler()
                .scheduleAtFixedRate(initialDelay, interval, task, executor);

        System.out.println("TempFileCleaner scheduled: dir=" + tmpDir + ", expiryMinutes=" + expiryMinutes +
                ", pattern=" + filePattern + ", intervalMinutes=" + intervalMinutes);
    }

    private void clean() {
        if (!Files.exists(dir) || !Files.isDirectory(dir)) {
            return;
        }

        Instant cutoff = Instant.now().minusSeconds(TimeUnit.MINUTES.toSeconds(expiryMinutes));

        try (Stream<Path> paths = Files.list(dir)) {
            paths
                    .filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().matches(filePattern))
                    .forEach(p -> {
                        try {
                            BasicFileAttributes attrs = Files.readAttributes(p, BasicFileAttributes.class);
                            Instant lastModified = attrs.lastModifiedTime().toInstant();
                            if (lastModified.isBefore(cutoff)) {
                                boolean deleted = tryDeleteWithLock(p);
                                if (deleted) {
                                    System.out.println("TempFileCleaner deleted: " + p);
                                } else {
                                    System.out.println("TempFileCleaner skipped (in-use/failed): " + p);
                                }
                            }
                        } catch (IOException ex) {
                            System.err.println("TempFileCleaner: cannot inspect file " + p + " : " + ex.getMessage());
                        }
                    });
        } catch (IOException e) {
            System.err.println("TempFileCleaner: failed to list files in " + dir + " : " + e.getMessage());
        }
    }

    private boolean tryDeleteWithLock(Path p) {
        try (FileChannel channel = FileChannel.open(p, StandardOpenOption.WRITE)) {
            FileLock lock = null;
            try {
                lock = channel.tryLock();
                if (lock == null) {
                    return false;
                }
                lock.release();
                lock = null;
                return Files.deleteIfExists(p);
            } catch (Throwable t) {
                return false;
            } finally {
                if (lock != null && lock.isValid()) {
                    try { lock.release(); }
                    catch (IOException ignored) {
                        // Nothing to do in this case
                    }
                }
            }
        } catch (IOException e) {
            return false;
        }
    }

    @PreDestroy
    public void stop() {
        if (cancellable != null && !cancellable.isCancelled()) {
            cancellable.cancel();
        }
    }
}
