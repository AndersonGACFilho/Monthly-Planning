package com.planejamentomensal.shared.infra.process;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Stops the backend when the desktop shell that started it is gone.
 *
 * <p>Part of POR-08: closing the window stops the backend through the shutdown endpoint, but if the shell
 * crashes or is killed, this watcher still closes the application cleanly (lock released, WAL
 * checkpointed) instead of leaving an orphan process that keeps the database locked (POR-10, POR-11).
 * The shell passes its PID as {@code app.parent-pid}; without it (development) nothing is watched.</p>
 *
 * @since 1.0
 */
@Component
public class ParentProcessWatcher {

    private static final Logger LOG = LoggerFactory.getLogger(ParentProcessWatcher.class);

    private final ApplicationContext context;
    private final long parentPid;

    /**
     * Creates the watcher.
     *
     * @param context   application to close when the parent ends
     * @param parentPid PID of the shell, or {@code 0} when not started by the shell
     */
    public ParentProcessWatcher(ApplicationContext context, @Value("${app.parent-pid:0}") long parentPid) {
        this.context = context;
        this.parentPid = parentPid;
    }

    /**
     * Starts watching once the application is ready.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void watch() {
        if (parentPid <= 0) {
            return;
        }
        Optional<ProcessHandle> parent = ProcessHandle.of(parentPid);
        CompletableFuture<?> parentExit = parent.map(ProcessHandle::onExit)
                .map(future -> (CompletableFuture<?>) future)
                .orElseGet(() -> CompletableFuture.completedFuture(null));
        parentExit.thenRun(this::shutdown);
    }

    private void shutdown() {
        LOG.warn("Shell process {} ended; stopping the backend", parentPid);
        Thread stopper = new Thread(() -> System.exit(SpringApplication.exit(context, () -> 0)), "parent-exit");
        stopper.start();
    }
}
