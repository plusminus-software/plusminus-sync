package software.plusminus.sync.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import software.plusminus.json.model.Classable;

@Data
@NoArgsConstructor
@AllArgsConstructor(staticName = "of")
public class Deleted implements Classable {

    @JsonIgnore
    private String className;
    private Object id;

    @JsonProperty("class")
    @Override
    public String getClazz() {
        return className;
    }
}
