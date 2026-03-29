package uk.anbu.schemabroker.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
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

    @Column(name = "schema_name", nullable = false, length = 100)
    private String schemaName;

    @Column(name = "group_name", nullable = false, length = 100)
    private String groupName;

    @Column(name = "login_user", nullable = false, length = 100)
    private String loginUser;

    @Column(name = "jdbc_url", length = 4000)
    private String jdbcUrl;

    @Column(name = "enabled")
    private Boolean enabled = true;

}

