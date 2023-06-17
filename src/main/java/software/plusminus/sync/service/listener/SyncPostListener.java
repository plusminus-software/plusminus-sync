package software.plusminus.sync.service.listener;

import software.plusminus.json.model.ApiObject;

public interface SyncPostListener {
    
    <T extends ApiObject> void afterWrite(T entity);
    
}
