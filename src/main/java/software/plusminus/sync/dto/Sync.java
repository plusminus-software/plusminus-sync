package software.plusminus.sync.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import software.plusminus.json.model.ApiObject;
import software.plusminus.json.model.Jsog;

import java.util.UUID;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Null;

@Data
@NoArgsConstructor
@AllArgsConstructor(staticName = "of")
public class Sync<T extends ApiObject> implements Jsog {
    
    private T object;
    
    private SyncType type;
    
    @NotNull(groups = Read.class)
    @Null(groups = Write.class)
    private Long index;

    @Null(groups = Write.class)
    private UUID transactionId;
    
}
