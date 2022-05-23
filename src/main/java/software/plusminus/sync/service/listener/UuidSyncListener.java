package software.plusminus.sync.service.listener;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import software.plusminus.data.repository.DataRepository;
import software.plusminus.json.model.ApiObject;
import software.plusminus.sync.annotation.Uuid;
import software.plusminus.sync.dto.Sync;
import software.plusminus.util.ClassUtils;
import software.plusminus.util.FieldUtils;

import java.lang.reflect.Field;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Component
public class UuidSyncListener implements SyncListener {

    @Autowired
    private DataRepository repository;
    
    @Override
    @Transactional
    public <T extends ApiObject> void onRead(Sync<T> sync) {
        T entity = sync.getObject();
        Set allObjects = FieldUtils.getDeepFieldValues(entity, field -> !ClassUtils.isJvmClass(field.getType()));
        allObjects.add(entity);
        allObjects.forEach(this::addMissedUuid);
    }
    
    private void addMissedUuid(Object entity) {
        Optional<Field> uuidField = FieldUtils.findFirstWithAnnotation(entity.getClass(), Uuid.class);
        if (!uuidField.isPresent()) {
            return;
        }
        Object uuid = FieldUtils.read(entity, uuidField.get());
        if (uuid == null) {
            UUID generatedUuid = UUID.randomUUID();
            FieldUtils.write(entity, generatedUuid, uuidField.get());
            repository.save(entity);
        }
    }
}
