package uk.anbu.schemabroker.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import uk.anbu.schemabroker.model.SchemaLease;
import uk.anbu.schemabroker.model.SchemaPool;
import uk.anbu.schemabroker.repository.SchemaLeaseRepository;
import uk.anbu.schemabroker.repository.SchemaPoolRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.anbu.schemabroker.service.LeaseService.DEFAULT_GROUP_NAME;

@SpringBootTest
class LeaseServiceConcurrencyTest {

    @Autowired
    private LeaseService leaseService;

    @Autowired
    private SchemaPoolRepository poolRepository;

    @Autowired
    private SchemaLeaseRepository leaseRepository;

    @BeforeEach
    void setUp() {
        leaseRepository.deleteAll();
        poolRepository.deleteAll();
        poolRepository.saveAll(List.of(createPool("SCHEMA_01"), createPool("SCHEMA_02")));
    }

    @Test
    void acquireLease_concurrentRequests_chooseDistinctSchemas() throws Exception {
        for (int attempt = 0; attempt < 10; attempt++) {
            runConcurrentAcquisition();
            leaseRepository.deleteAll();
        }
    }

    private void runConcurrentAcquisition() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        Callable<Optional<SchemaLease>> acquireLeaseTask = () -> {
            ready.countDown();
            start.await();
            return leaseService.acquireLease(Thread.currentThread().getName(), "metadata", "127.0.0.1", "localhost", Instant.now());
        };

        try {
            Future<Optional<SchemaLease>> futureA = executor.submit(acquireLeaseTask);
            Future<Optional<SchemaLease>> futureB = executor.submit(acquireLeaseTask);
            ready.await();
            start.countDown();

            Optional<SchemaLease> leaseA = futureA.get(10, TimeUnit.SECONDS);
            Optional<SchemaLease> leaseB = futureB.get(10, TimeUnit.SECONDS);

            assertThat(leaseA).isPresent();
            assertThat(leaseB).isPresent();
            assertThat(leaseA.get().getSchemaName()).isNotEqualTo(leaseB.get().getSchemaName());

            Instant snapshot = Instant.now();
            List<SchemaLease> activeLeases = leaseRepository.findActiveLeasesNotExpired(snapshot);
            assertThat(activeLeases).hasSize(2);
            Set<String> schemaNames = activeLeases.stream()
                    .map(SchemaLease::getSchemaName)
                    .collect(java.util.stream.Collectors.toSet());
            assertThat(schemaNames).containsExactlyInAnyOrder("SCHEMA_01", "SCHEMA_02");
        } finally {
            executor.shutdownNow();
        }
    }

    private SchemaPool createPool(String schemaName) {
        SchemaPool pool = new SchemaPool();
        pool.setSchemaName(schemaName);
        pool.setGroupName(DEFAULT_GROUP_NAME);
        pool.setLoginUser("sa");
        pool.setJdbcUrl("jdbc:h2:mem:" + schemaName);
        pool.setEnabled(true);
        return pool;
    }
}

