package software.plusminus.sync;

import lombok.Data;
import software.plusminus.audit.annotation.Auditable;
import software.plusminus.sync.annotation.Syncable;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Version;

@Data
@Auditable
@Syncable
@Entity
public class TestEntity2 {

    @Id
    @GeneratedValue
    private Long id;
    private String myField;
    @Version
    private Long version;
    private String tenant;
    private Boolean deleted = Boolean.FALSE;

}