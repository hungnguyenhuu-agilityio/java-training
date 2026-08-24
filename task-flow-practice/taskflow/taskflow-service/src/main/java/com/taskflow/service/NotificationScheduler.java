package com.taskflow.service;

import com.taskflow.domain.Task;
import com.taskflow.domain.TaskRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * Background thread that logs a notification for every task due within the next 24 hours.
 *
 * <p>Runs on a single-thread {@link ScheduledExecutorService}. The scheduled task is
 * wrapped in {@code try/catch(Exception)} so a DB failure never silently kills the
 * executor (per the ScheduledExecutorService contract).
 *
 * <p>Log format: {@code [NOTIFICATION] Task "<title>" (id=X) is due within 24 hours.}
 */
public class NotificationScheduler {

    private static final Logger log = Logger.getLogger(NotificationScheduler.class.getName());

    private final TaskRepository taskDao;
    private final long intervalMinutes;
    private final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor();

    /**
     * @param taskDao         DAO used to query upcoming tasks
     * @param intervalMinutes how often to run the check (minutes)
     */
    public NotificationScheduler(TaskRepository taskDao, long intervalMinutes) {
        this.taskDao = taskDao;
        this.intervalMinutes = intervalMinutes;
    }

    /**
     * Starts the periodic due-date check.
     * The first run happens immediately (initial delay = 0).
     */
    public void start() {
        scheduler.scheduleAtFixedRate(this::checkDueTasks, 0, intervalMinutes, TimeUnit.MINUTES);
    }

    /**
     * Queries the DAO for tasks due within the next 24 hours and logs them.
     * Any exception is caught and logged as a warning — never propagated.
     */
    public void checkDueTasks() {
        try {
            LocalDate from = LocalDate.now();
            LocalDate to = from.plusDays(1);

            List<Task> dueSoon = taskDao.findDueSoon(from, to);
            for (Task task : dueSoon) {
                log.info(String.format(
                        "[NOTIFICATION] Task \"%s\" (id=%d) is due within 24 hours.",
                        task.getTitle(), task.getId()));
            }
        } catch (Exception e) {
            log.warning("due-date check failed: " + e.getMessage());
        }
    }

    /**
     * Shuts down the executor immediately (interrupts any running task).
     * Should be called from a JVM shutdown hook.
     */
    public void shutdown() {
        scheduler.shutdownNow();
    }
}
