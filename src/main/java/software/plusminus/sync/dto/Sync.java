package software.plusminus.sync.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import software.plusminus.json.model.Classable;

@Data
@NoArgsConstructor
@AllArgsConstructor(staticName = "of")
public class Sync<T extends Classable> {
    private T object;
    private SyncType type;
}
