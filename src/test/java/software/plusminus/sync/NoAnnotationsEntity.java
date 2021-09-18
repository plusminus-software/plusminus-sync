package software.plusminus.sync;

import company.plusminus.json.model.Classable;
import lombok.Data;

import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Version;

@Data
public class NoAnnotationsEntity implements Classable {

    @Id
    @GeneratedValue
    private Long id;
    private String myField;
    @Version
    private Long version;
    private String tenant;
    private Boolean deleted = Boolean.FALSE;

}