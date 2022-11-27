package software.plusminus.sync.service.merger;

import com.fasterxml.jackson.databind.ser.PropertyWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import software.plusminus.json.model.ApiObject;
import software.plusminus.sync.dto.Sync;
import software.plusminus.sync.dto.SyncType;
import software.plusminus.sync.service.jsog.SyncJsonService;

import java.util.function.Predicate;

@Component
public class EqualsMerger implements Merger {
    
    @Autowired
    private SyncJsonService jsonService;

    @Override
    public <T extends ApiObject> boolean supports(Sync<T> sync) {
        return sync.getType() == SyncType.CREATE || sync.getType() == SyncType.UPDATE;
    }

    @Override
    public <T extends ApiObject> void process(T current, Sync<T> sync) {
        Predicate<PropertyWriter> fieldFilter = fieldFilter(sync.getType());
        String currentJsog = jsonService.toJson(current, fieldFilter);
        String syncObjectJsog = jsonService.toJson(sync.getObject(), fieldFilter);
        boolean areEqual = currentJsog.equals(syncObjectJsog);
        if (areEqual) {
            sync.setObject(current);
            sync.setType(SyncType.TURN_BACK);
        }
    }
    
    @SuppressWarnings("squid:S1126")
    private Predicate<PropertyWriter> fieldFilter(SyncType type) {
        return writer -> {
            if (writer.getAnnotation(javax.persistence.Version.class) != null
                    || writer.getAnnotation(org.springframework.data.annotation.Version.class) != null) {
                return false;
            }
            if (type == SyncType.CREATE
                    && (writer.getAnnotation(org.springframework.data.annotation.Id.class) != null
                            || writer.getAnnotation(javax.persistence.Id.class) != null)) {
                return false;
            }
            return true;
        };
    }
}
