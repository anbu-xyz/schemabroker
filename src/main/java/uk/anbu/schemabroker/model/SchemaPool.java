package uk.anbu.schemabroker.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "schema_pool")
@Getter
@Setter
@NoArgsConstructor
public class SchemaPool {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "schema_name", nullable = false, unique = true, length = 100)
    private String schemaName;

    @Column(name = "jdbc_url", length = 255)
    private String jdbcUrl;

    @Column(name = "enabled")
    private Boolean enabled = true;

}

