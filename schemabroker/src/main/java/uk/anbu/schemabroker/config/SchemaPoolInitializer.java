package uk.anbu.schemabroker.config;

import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import uk.anbu.schemabroker.model.SchemaPool;
import uk.anbu.schemabroker.repository.SchemaPoolRepository;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
@Slf4j
public class SchemaPoolInitializer implements ApplicationRunner {

    private final SchemaPoolRepository poolRepository;
    private final Resource schemaResource;
    private final YAMLMapper mapper = new YAMLMapper();

    public SchemaPoolInitializer(SchemaPoolRepository poolRepository,
                                 @Value("${database.init.schemas}") Resource schemaResource) {
        this.poolRepository = poolRepository;
        this.schemaResource = schemaResource;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (!schemaResource.exists()) {
            log.warn("Schema initializer skipped because {} was not found", schemaResource.getDescription());
            return;
        }
        SchemaDefinitions definitions = loadDefinitions();
        if (definitions == null || definitions.getSchemas().isEmpty()) {
            log.warn("Schema initializer did not find any entries in {}", schemaResource.getDescription());
            return;
        }
        Map<String, SchemaPool> existingPools = poolRepository.findAll().stream()
                .collect(Collectors.toMap(SchemaPool::getSchemaName, pool -> pool));

        List<SchemaPool> toSave = new ArrayList<>();
        for (SchemaDefinition definition : definitions.getSchemas()) {
            SchemaPool pool = existingPools.remove(definition.getSchemaName());
            if (pool == null) {
                pool = new SchemaPool();
                pool.setSchemaName(definition.getSchemaName());
            }
            boolean updated = syncWithDefinition(pool, definition);
            if (updated || pool.getId() == null) {
                toSave.add(pool);
            }
        }

        for (SchemaPool orphan : existingPools.values()) {
            if (Boolean.TRUE.equals(orphan.getEnabled())) {
                orphan.setEnabled(false);
                toSave.add(orphan);
            }
        }

        if (!toSave.isEmpty()) {
            poolRepository.saveAll(toSave);
            log.info("Schema initializer updated {} pool entries", toSave.size());
        }
    }

    private SchemaDefinitions loadDefinitions() throws IOException {
        try (var in = schemaResource.getInputStream()) {
            return mapper.readValue(in, SchemaDefinitions.class);
        }
    }

    private boolean syncWithDefinition(SchemaPool pool, SchemaDefinition definition) {
        boolean changed = false;
        if (!Objects.equals(pool.getLoginUser(), definition.getLoginUser())) {
            pool.setLoginUser(definition.getLoginUser());
            changed = true;
        }
        if (!Objects.equals(pool.getJdbcUrl(), definition.getJdbcUrl())) {
            pool.setJdbcUrl(definition.getJdbcUrl());
            changed = true;
        }
        if (!Objects.equals(pool.getEnabled(), true)) {
            pool.setEnabled(true);
            changed = true;
        }
        return changed;
    }

    @Getter
    private static final class SchemaDefinitions {
        private List<SchemaDefinition> schemas = Collections.emptyList();

        public void setSchemas(List<SchemaDefinition> schemas) {
            this.schemas = schemas == null ? Collections.emptyList() : schemas;
        }
    }

    @Setter
    @Getter
    private static final class SchemaDefinition {
        private String schemaName;
        private String loginUser;
        private String jdbcUrl;

    }
}

