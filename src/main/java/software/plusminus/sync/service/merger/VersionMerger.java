package software.plusminus.sync.service.merger;

import com.fasterxml.jackson.databind.ser.PropertyWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;
import software.plusminus.json.model.ApiObject;
import software.plusminus.json.model.Classable;
import software.plusminus.sync.annotation.Uuid;
import software.plusminus.sync.dto.Sync;
import software.plusminus.sync.dto.SyncType;
import software.plusminus.sync.service.jsog.SyncJsonService;
import software.plusminus.sync.service.version.SyncVersionService;
import software.plusminus.util.AnnotationUtils;
import software.plusminus.util.FieldUtils;

import java.lang.reflect.Field;
import java.util.Optional;
import java.util.function.BiPredicate;
import javax.persistence.EmbeddedId;
import javax.persistence.IdClass;

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
        
        String currentJsog = jsonService.toJson(current, fieldFilter(current));
        String syncObjectJsog = jsonService.toJson(sync.getObject(), fieldFilter(sync.getObject()));
        boolean areEqualsIgnoringVersion = currentJsog.equals(syncObjectJsog);
        if (areEqualsIgnoringVersion) {
            FieldUtils.write(sync.getObject(), currentVersion, versionField);
        }
    }

    private BiPredicate<Object, PropertyWriter> fieldFilter(Object rootEntity) {
        return (pojo, writer) -> {
            if (pojo != rootEntity) {
                if (AnnotationUtils.findAnnotation("Entity", pojo) == null) {
                    return true;
                }
                return isIdField(writer) || isClassField(writer) || isUuidField(writer);
            }
            return !isVersionField(writer);
        };
    }
    
    private boolean isVersionField(PropertyWriter writer) {
        return writer.getAnnotation(javax.persistence.Version.class) != null
                || writer.getAnnotation(org.springframework.data.annotation.Version.class) != null;
    }
    
    private boolean isIdField(PropertyWriter writer) {
        return writer.getAnnotation(javax.persistence.Id.class) != null
                || writer.getAnnotation(org.springframework.data.annotation.Id.class) != null
                || writer.getAnnotation(IdClass.class) != null
                || writer.getAnnotation(EmbeddedId.class) != null;
    }
    
    private boolean isClassField(PropertyWriter writer) {
        return writer.getMember().getMember().getDeclaringClass() == Classable.class;
    }
    
    private boolean isUuidField(PropertyWriter writer) {
        return writer.getAnnotation(Uuid.class) != null;
    }

}
