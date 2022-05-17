package software.plusminus.sync.service.listener;

import software.plusminus.json.model.ApiObject;
import software.plusminus.sync.dto.Sync;

public interface SyncListener {
    
    default <T extends ApiObject> void onRead(Sync<T> sync) {
    }
    
    default <T extends ApiObject> void onWrite(Sync<T> sync) {
    }
    
}
