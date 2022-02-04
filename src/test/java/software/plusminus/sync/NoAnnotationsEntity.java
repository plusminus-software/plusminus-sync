package software.plusminus.sync;

import lombok.Data;
import software.plusminus.json.model.ApiObject;

import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Version;

@Data
public class NoAnnotationsEntity implements ApiObject {

    @Id
    @GeneratedValue
    private Long id;
    private String myField;
    @Version
    private Long version;
    private String tenant;
    private Boolean deleted = Boolean.FALSE;

}