package software.plusminus.sync.service.merger;

import com.fasterxml.jackson.databind.ser.PropertyWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;
import software.plusminus.json.model.ApiObject;
import software.plusminus.sync.dto.Sync;
import software.plusminus.sync.dto.SyncType;
import software.plusminus.sync.service.jsog.SyncJsonService;
import software.plusminus.sync.service.version.SyncVersionService;
import software.plusminus.util.FieldUtils;

import java.lang.reflect.Field;
import java.util.Optional;
import java.util.function.Predicate;

@Component
public class VersionMerger implements Merger {

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
        Field versionField = versionService.findVersionFieldOrException(current);
        Object currentVersion = FieldUtils.read(current, versionField);
        Object syncVersion = FieldUtils.read(sync.getObject(), versionField);
        if (ObjectUtils.nullSafeEquals(currentVersion, syncVersion)) {
            return;
        }
        
        Predicate<PropertyWriter> ignoreVersionFieldFilter = ignoreVersionFieldFilter();
        String currentJsog = jsonService.toJson(current, ignoreVersionFieldFilter);
        String syncObjectJsog = jsonService.toJson(sync.getObject(), ignoreVersionFieldFilter);
        boolean areEqualsIgnoringVersion = currentJsog.equals(syncObjectJsog);
        if (areEqualsIgnoringVersion) {
            FieldUtils.write(sync.getObject(), currentVersion, versionField);
        }
    }

    private Predicate<PropertyWriter> ignoreVersionFieldFilter() {
        return writer -> writer.getAnnotation(javax.persistence.Version.class) == null
                && writer.getAnnotation(org.springframework.data.annotation.Version.class) == null;
    }

}
