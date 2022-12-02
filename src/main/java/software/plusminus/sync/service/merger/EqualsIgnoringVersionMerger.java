package software.plusminus.sync.service.merger;

import com.fasterxml.jackson.databind.ser.PropertyWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import software.plusminus.json.model.ApiObject;
import software.plusminus.sync.dto.Sync;
import software.plusminus.sync.dto.SyncType;
import software.plusminus.sync.service.jsog.SyncJsonService;
import software.plusminus.sync.service.version.SyncVersionService;

import java.lang.reflect.Field;
import java.util.Optional;
import java.util.function.Predicate;

@Component
public class EqualsIgnoringVersionMerger implements Merger {

    @Autowired
    private SyncJsonService jsonService;
    @Autowired
    private SyncVersionService versionService;

    @Override
    public <T extends ApiObject> boolean supports(Sync<T> sync) {
        if (sync.getType() != SyncType.CREATE
                && sync.getType() != SyncType.UPDATE) {
            return false;
        }
        Optional<Field> versionField = versionService.findVersionField(sync.getObject());
        return versionField.isPresent();
    }

    @Override
    public <T extends ApiObject> void process(T current, Sync<T> sync) {
        if (areFullyEquals(current, sync)) {
            return;
        }
        Predicate<PropertyWriter> ignoreVersionFieldFilter = ignoreVersionFieldFilter();
        String currentJsog = jsonService.toJson(current, ignoreVersionFieldFilter);
        String syncObjectJsog = jsonService.toJson(sync.getObject(), ignoreVersionFieldFilter);
        boolean areEqualsIgnoringVersion = currentJsog.equals(syncObjectJsog);
        if (areEqualsIgnoringVersion) {
            versionService.populateVersion(current, sync.getObject());
        }
    }

    private <T extends ApiObject> boolean areFullyEquals(T current, Sync<T> sync) {
        String currentJsog = jsonService.toJson(current, p -> true);
        String syncObjectJsog = jsonService.toJson(sync.getObject(), p -> true);
        return currentJsog.equals(syncObjectJsog);
    }

    private Predicate<PropertyWriter> ignoreVersionFieldFilter() {
        return writer -> writer.getAnnotation(javax.persistence.Version.class) == null
                && writer.getAnnotation(org.springframework.data.annotation.Version.class) == null;
    }

}
