package software.plusminus.sync;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import software.plusminus.audit.annotation.Auditable;
import software.plusminus.json.model.ApiObject;
import software.plusminus.sync.annotation.Syncable;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Version;

@Data
@Auditable
@Syncable
@Entity
public class InnerEntity implements ApiObject {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String myField;
    @Version
    private Long version;
    private String tenant;
    @JsonInclude(JsonInclude.Include.NON_DEFAULT)
    private Boolean deleted = Boolean.FALSE;

}