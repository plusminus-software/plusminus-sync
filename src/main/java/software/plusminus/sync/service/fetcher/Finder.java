package software.plusminus.sync.service.fetcher;

import software.plusminus.json.model.ApiObject;

import java.util.Optional;

public interface Finder {
    
    <T extends ApiObject> Optional<T> find(T object);
    
}
