package software.plusminus.sync;

import lombok.Data;
import software.plusminus.json.model.ApiObject;

import javax.persistence.Embeddable;

@Data
@Embeddable
public class EmbeddedObject implements ApiObject {
    
    private Long embeddedId;
    private String embeddedString;

}