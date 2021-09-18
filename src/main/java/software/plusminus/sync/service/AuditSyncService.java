package software.plusminus.sync.service;

import company.plusminus.data.service.data.DataService;
import company.plusminus.data.service.entity.EntityService;
import company.plusminus.json.model.Classable;
import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import software.plusminus.audit.annotation.Auditable;
import software.plusminus.audit.model.AuditLog;
import software.plusminus.audit.repository.AuditLogRepository;
import software.plusminus.audit.service.AuditLogService;
import software.plusminus.sync.annotation.Syncable;
import software.plusminus.sync.dto.Deleted;
import software.plusminus.sync.dto.Sync;
import software.plusminus.sync.dto.SyncType;
import software.plusminus.sync.exception.SyncException;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.persistence.Entity;

@Service
@ConditionalOnBean(AuditLogService.class)
public class AuditSyncService implements SyncService {

    @Autowired
    private EntityService entityService;
    @Autowired
    private DataService dataService;
    @Autowired
    private AuditLogRepository auditLogRepository;

    @Override
    public List<Sync<? extends Classable>> read(List<String> types, String ignoreDevice,
                                                Long fromAuditNumber, Integer size,
                                                Sort.Direction direction) {

        types = types.stream()
                .map(entityService::findClass)
                .peek(this::checkAnnotations)
                .map(Class::getName)
                .collect(Collectors.toList());

        Pageable pageable = PageRequest.of(0, size, Sort.by(direction, "number"));
        Page<AuditLog<? extends Classable>> page =
                auditLogRepository.findByEntityTypeInAndDeviceIsNotAndNumberGreaterThanAndCurrentTrue(
                        types, ignoreDevice, fromAuditNumber, pageable);

        return page.getContent().stream()
                .map(this::toSync)
                .collect(Collectors.toList());
    }

    @Override
    public <T extends Classable> List<? extends T> write(List<Sync<? extends T>> actions) {
        return actions.stream()
                .peek(action -> checkAnnotations(action.getObject().getClass()))
                .map(item -> {
                    T entity = item.getObject();
                    switch (item.getType()) {
                        case CREATE:
                            return dataService.create(entity);
                        case UPDATE:
                            return dataService.update(entity);
                        case PATCH:
                            return dataService.patch(entity);
                        case DELETE:
                            dataService.delete(entity);
                            return null;
                        default:
                            throw new SyncException("Can't sync: unknown " + item.getType() + " sync type");
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private Sync<? extends Classable> toSync(AuditLog<? extends Classable> auditLog) {
        Classable entity;
        switch (auditLog.getAction()) {
            case CREATE:
                entity = unproxy(auditLog.getEntity());
                return Sync.of(entity, SyncType.CREATE);
            case UPDATE:
                entity = unproxy(auditLog.getEntity());
                return Sync.of(entity, SyncType.UPDATE);
            case DELETE:
                return Sync.of(toDeleted(auditLog), SyncType.DELETE);
            default:
                throw new SyncException("Unknow auditLog action " + auditLog.getAction());
        }
    }

    private <T> T unproxy(T entity) {
        return (T) Hibernate.unproxy(entity);
    }

    private <T> void checkAnnotations(Class<T> type) {
        List<Class<? extends Annotation>> missedAnnotations = new ArrayList<>();
        if (!type.isAnnotationPresent(Entity.class)) {
            missedAnnotations.add(Entity.class);
        }
        if (AnnotationUtils.findAnnotation(type, Auditable.class) == null) {
            missedAnnotations.add(Auditable.class);
        }
        if (AnnotationUtils.findAnnotation(type, Syncable.class) == null) {
            missedAnnotations.add(Syncable.class);
        }

        if (!missedAnnotations.isEmpty()) {
            throw new SyncException("Synchronization error, required annotations are missed: " + missedAnnotations);
        }
    }

    private Deleted toDeleted(AuditLog auditLog) {
        Class<?> type;
        try {
            type = Class.forName(auditLog.getEntityType());
        } catch (ClassNotFoundException e) {
            throw new SyncException("Unknown type: " + auditLog.getEntityType(), e);
        }
        return Deleted.of(type.getSimpleName(), auditLog.getEntityId());
    }
}