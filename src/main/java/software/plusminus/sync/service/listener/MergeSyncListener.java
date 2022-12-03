package software.plusminus.sync.service.listener;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.plusminus.data.repository.DataRepository;
import software.plusminus.json.model.ApiObject;
import software.plusminus.sync.dto.Sync;
import software.plusminus.sync.dto.SyncType;
import software.plusminus.sync.exception.SyncException;
import software.plusminus.sync.service.fetcher.Finder;
import software.plusminus.sync.service.fetcher.SyncTransactionService;
import software.plusminus.sync.service.merger.Merger;
import software.plusminus.sync.service.version.SyncVersionService;
import software.plusminus.util.EntityUtils;
import software.plusminus.util.FieldUtils;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@ConditionalOnProperty(value = "plusminus.sync.merge", matchIfMissing = true)
@Component
public class MergeSyncListener implements SyncListener {

    @Autowired
    private List<Merger> mergers;
    @Autowired
    private List<Finder> finders;
    @Autowired
    private SyncTransactionService transactionService;
    @Autowired
    private SyncVersionService versionService;
    @Autowired
    private DataRepository repository;

    @Override
    public <T extends ApiObject> void onWrite(Sync<T> sync) {
        List<Merger> foundMergers = mergers.stream()
                .filter(m -> m.supports(sync))
                .collect(Collectors.toList());
        if (foundMergers.isEmpty()) {
            return;
        }

        Optional<T> current = findCurrent(sync);
        if (!current.isPresent()) {
            return;
        }
        foundMergers.forEach(m -> m.process(current.get(), sync));
    }

    private <T extends ApiObject> Optional<T> findCurrent(Sync<T> sync) {
        if (sync.getType() == SyncType.CREATE) {
            return findOnCreate(sync);
        } else if (sync.getType() == SyncType.UPDATE) {
            return findById(sync.getObject());
        }
        return Optional.empty();
    }
    
    private <T extends ApiObject> Optional<T> findOnCreate(Sync<T> sync) {
        return finders.stream()
                .map(f -> transactionService.newTransaction(() -> f.find(sync.getObject())))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(current -> {
                    Optional<T> byId = findById(current);
                    if (!byId.isPresent()) {
                        throw new SyncException("Entity must be present");
                    }
                    return byId.get();
                })
                .peek(current -> {
                    populateId(current, sync.getObject());
                    populateZeroVersionIfNeeded(current, sync.getObject());
                    sync.setType(SyncType.UPDATE);
                })
                .findFirst();
    }
    
    private <T extends ApiObject> Optional<T> findById(T object) {
        Class<T> type = (Class<T>) object.getClass();
        Object id = EntityUtils.findId(object);
        if (id == null) {
            throw new SyncException("Id must not be null on UPDATE sync");
        }
        T current = repository.findById(type, id);
        return Optional.ofNullable(current);
    }

    private <T extends ApiObject> void populateId(T source, T target) {
        Field idField = EntityUtils.findIdField(source.getClass())
                .orElseThrow(() -> new SyncException("Class " + source.getClass() + " should contain @Id field"));
        Object id = FieldUtils.read(source, idField);
        if (id == null) {
            throw new SyncException("Id can not be null for the existing entity");
        }
        FieldUtils.write(target, id, idField);
    }
    
    private <T extends ApiObject> void populateZeroVersionIfNeeded(T source, T target) {
        Optional<Field> field = versionService.findVersionField(source);
        if (!field.isPresent()) {
            return;
        }
        Object sourceVersion = FieldUtils.read(source, field.get());
        if (!(sourceVersion instanceof Number)) {
            return;
        }
        
        if (((Number) sourceVersion).longValue() == 0) {
            FieldUtils.write(target, sourceVersion, field.get());
        }
    }
}
