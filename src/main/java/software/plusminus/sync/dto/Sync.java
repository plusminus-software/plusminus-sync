package software.plusminus.sync.dto;

import company.plusminus.json.model.Classable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor(staticName = "of")
public class Sync<T extends Classable> {
    private T object;
    private SyncType type;
}
