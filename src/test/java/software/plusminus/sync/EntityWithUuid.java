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
import javax.persistence.Version;

@Data
@Syncable
@Entity
public class EntityWithUuid  implements ApiObject {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Uuid
    private UUID uuid;
    @Version
    private Long version;
    private String myField;
    
}
