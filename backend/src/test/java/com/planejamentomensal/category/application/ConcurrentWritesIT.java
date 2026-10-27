package com.planejamentomensal.category.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "app.home=${java.io.tmpdir}/pm-concurrent-${random.uuid}")
class ConcurrentWritesIT {

    @Autowired
    private SaveCategoryUseCase save;

    @Autowired
    private CategoryQueries queries;

    @Test
    @DisplayName("POR-11: concurrent write transactions queue instead of failing with SQLITE_BUSY")
    void concurrentSeedsDoNotFail() throws Exception {
        // Given two requests seeding the initial categories at the same time (React StrictMode does this)
        ExecutorService pool = Executors.newFixedThreadPool(4);
        Callable<Integer> seed = () -> save.seedInitial(Locale.forLanguageTag("pt-BR")).size();
        // When they run concurrently
        List<Future<Integer>> results = pool.invokeAll(List.of(seed, seed, seed, seed));
        pool.shutdown();
        // Then none fails and only one creates the categories
        int created = 0;
        for (Future<Integer> result : results) {
            created += result.get();
        }
        assertThat(created).isEqualTo(InitialCategories.ALL.size());
        assertThat(queries.all()).hasSize(InitialCategories.ALL.size() + 1);
    }
}
