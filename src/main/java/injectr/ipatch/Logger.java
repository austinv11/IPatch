package injectr.ipatch;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Simple logger.
 */
public class Logger {

    private static final String PATTERN = "%s [%s][%s] %s%n";

    final boolean verbose;

    public Logger(boolean verbose) {
        this.verbose = verbose;
    }

    private void write(Level level, String msg) {
        if (!verbose && level.ordinal() < 1)
            return;

        if (level.ordinal() > 1) {
            System.err.printf(PATTERN,
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                    level,
                    Thread.currentThread().getName(),
                    msg);
        } else {
            System.out.printf(PATTERN,
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                    level,
                    Thread.currentThread().getName(),
                    msg);
        }
    }

    public void debug(String msg, Object... args) {
        write(Level.DEBUG, String.format(msg, args));
    }

    public void info(String msg, Object... args) {
        write(Level.INFO, String.format(msg, args));
    }

    public void warn(String msg, Object... args) {
        write(Level.WARNING, String.format(msg, args));
    }

    public void error(String msg, Object... args) {
        write(Level.ERROR, String.format(msg, args));
    }

    public void error(String msg, Throwable t) {
        synchronized (this) { //Prevents messages from getting out of sync
            write(Level.ERROR, msg);
            t.printStackTrace();
        }
    }

    enum Level {
        DEBUG, INFO, WARNING, ERROR
    }
}
