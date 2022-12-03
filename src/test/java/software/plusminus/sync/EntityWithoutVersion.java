package software.plusminus.sync;

import lombok.Data;
import software.plusminus.json.model.ApiObject;
import software.plusminus.sync.annotation.Syncable;
import software.plusminus.sync.annotation.Uuid;

import java.util.UUID;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

@Data
@Syncable
@Entity
public class EntityWithoutVersion implements ApiObject {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Uuid
    private UUID uuid;
    private String myField;
    
}
