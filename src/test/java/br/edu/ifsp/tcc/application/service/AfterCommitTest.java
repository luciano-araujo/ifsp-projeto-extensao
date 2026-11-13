package br.edu.ifsp.tcc.application.service;

import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AfterCommitTest {

    @Test
    void run_shouldRunRightAwayWithoutATransaction() {
        AtomicInteger calls = new AtomicInteger();

        AfterCommit.run(calls::incrementAndGet);

        assertEquals(1, calls.get());
    }

    @Test
    void run_shouldWaitForTheCommitInsideATransaction() {
        AtomicInteger calls = new AtomicInteger();
        TransactionSynchronizationManager.initSynchronization();
        try {
            AfterCommit.run(calls::incrementAndGet);
            assertEquals(0, calls.get());

            TransactionSynchronizationManager.getSynchronizations().forEach(s -> s.afterCommit());
            assertEquals(1, calls.get());
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }
}
