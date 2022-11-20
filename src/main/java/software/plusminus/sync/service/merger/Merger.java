package software.plusminus.sync.service.merger;

import software.plusminus.json.model.ApiObject;
import software.plusminus.sync.dto.Sync;

public interface Merger {

    <T extends ApiObject> boolean supports(Sync<T> sync);

    <T extends ApiObject> void process(T current, Sync<T> sync);
    
}
