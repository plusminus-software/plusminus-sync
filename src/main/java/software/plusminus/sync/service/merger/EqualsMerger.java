package software.plusminus.sync.service.merger;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.springframework.data.annotation.Version;
import org.springframework.stereotype.Component;
import software.plusminus.json.model.ApiObject;
import software.plusminus.sync.dto.Sync;
import software.plusminus.sync.dto.SyncType;
import software.plusminus.util.EntityUtils;
import software.plusminus.util.FieldUtils;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
public class EqualsMerger implements Merger {
    
    @Override
    public <T extends ApiObject> boolean supports(Sync<T> sync) {
        return sync.getType() == SyncType.CREATE || sync.getType() == SyncType.UPDATE;
    }

    @Override
    public <T extends ApiObject> void process(T current, Sync<T> sync) {
        boolean areEqual = EqualsBuilder.reflectionEquals(current, sync.getObject(), excludingFields(sync));
        if (areEqual) {
            sync.setObject(current);
            sync.setType(SyncType.TURN_BACK);
        }
    }
    
    private List<String> excludingFields(Sync<?> sync) {
        List<String> fields = new ArrayList<>();
        
        Optional<Field> springVersionField = FieldUtils.findFirstWithAnnotation(
                sync.getObject().getClass(), Version.class);
        springVersionField.ifPresent(field -> fields.add(field.getName()));
        
        Optional<Field> jpaVersionField = FieldUtils.findFirstWithAnnotation(
                sync.getObject().getClass(), javax.persistence.Version.class);
        jpaVersionField.ifPresent(field -> fields.add(field.getName()));

        if (sync.getType() == SyncType.CREATE) {
            Optional<Field> idField = EntityUtils.findIdField(sync.getObject().getClass());
            idField.ifPresent(field -> fields.add(field.getName()));
        }

        return fields;
    }
}
