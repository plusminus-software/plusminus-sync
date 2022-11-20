package software.plusminus.sync.service.fetcher;

import software.plusminus.json.model.ApiObject;
import software.plusminus.sync.dto.Sync;

import java.util.Optional;

public interface Fetcher {
    
    <T extends ApiObject> Optional<T> fetch(Sync<T> sync);
    
}
