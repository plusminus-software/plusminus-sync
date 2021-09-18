package software.plusminus.sync.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import company.plusminus.json.model.Classable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

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
