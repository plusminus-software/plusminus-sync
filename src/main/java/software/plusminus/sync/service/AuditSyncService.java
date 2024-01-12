package software.plusminus.sync.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.plusminus.audit.annotation.Auditable;
import software.plusminus.audit.model.AuditLog;
import software.plusminus.audit.model.DataAction;
import software.plusminus.audit.repository.AuditLogRepository;
import software.plusminus.audit.service.AuditLogService;
import software.plusminus.audit.service.DeviceContext;
import software.plusminus.data.service.data.DataService;
import software.plusminus.data.service.entity.EntityService;
import software.plusminus.json.model.ApiObject;
import software.plusminus.sync.annotation.Syncable;
import software.plusminus.sync.dto.Deleted;
import software.plusminus.sync.dto.Sync;
import software.plusminus.sync.dto.SyncType;
import software.plusminus.sync.exception.SyncException;
import software.plusminus.sync.service.fetcher.SyncTransactionService;
import software.plusminus.sync.service.listener.SyncListener;
import software.plusminus.sync.service.listener.SyncPostListener;
import software.plusminus.tenant.context.TenantContext;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.persistence.Entity;

import static javax.management.timer.Timer.ONE_MINUTE;

@Service
@ConditionalOnBean(AuditLogService.class)
@SuppressWarnings({"squid:S3864", "checkstyle:ClassFanOutComplexity"})
public class AuditSyncService implements SyncService {

    @Autowired
    private EntityService entityService;
    @Autowired
    private DataService dataService;
    @Autowired
    private SyncTransactionService transactionService;
    @Autowired
    private AuditLogRepository auditLogRepository;
    @Autowired
    private DeviceContext deviceContext;
    @Autowired
    private TenantContext tenantContext;
    @Autowired
    private List<SyncListener> listeners;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired(required = false)
    private List<SyncPostListener> postListeners = Collections.emptyList();
    private final Cache<Long, AuditLog<? extends ApiObject>> cache = Caffeine.newBuilder()
            .maximumSize(1_000)
            .build();

    @Scheduled(fixedDelay = ONE_MINUTE)
    public void refresh() {
        try {
            tenantContext.disable();
            transactionService.run(() -> {
                if (cache.asMap().isEmpty()) {
                    Page<AuditLog<? extends ApiObject>> page = auditLogRepository.findByCurrentTrue(
                            PageRequest.of(0, 1_000, Sort.Direction.DESC, "number"));
                    page.forEach(auditLog -> {
                        fetchEntity(auditLog);
                        cache.put(auditLog.getNumber(), auditLog);
                    });
                    return;
                }
                List<AuditLog<?>> cachedAuditLogs = new ArrayList<>(cache.asMap().values());
                cachedAuditLogs.forEach(cachedAuditLog -> {
                    AuditLog<? extends ApiObject> current =
                            auditLogRepository.findByEntityTypeAndEntityIdAndCurrentTrue(cachedAuditLog.getEntityType(),
                                    cachedAuditLog.getEntityId());
                    if (current != null && !Objects.equals(current.getNumber(), cachedAuditLog.getNumber())) {
                        fetchEntity(current);
                        cache.invalidate(cachedAuditLog.getNumber());
                        cache.put(current.getNumber(), current);
                    }
                });
            });
        } finally {
            tenantContext.enable();
        }
    }

    @Override
    public List<Sync<? extends ApiObject>> read(List<String> types, boolean excludeCurrentDevice,
                                                Long offset, Integer size,
                                                Sort.Direction direction) {

        types = types.stream()
                .map(entityService::findClass)
                .peek(this::checkAnnotations)
                .map(Class::getName)
                .collect(Collectors.toList());

        Pageable pageable = PageRequest.of(0, size, Sort.by(direction, "number"));
        Page<AuditLog<? extends ApiObject>> page;
        if (excludeCurrentDevice) {
            String ignoreDevice = deviceContext.currentDevice();
            page = auditLogRepository.findByEntityTypeInAndDeviceIsNotAndNumberGreaterThanAndCurrentTrue(
                    types, ignoreDevice, offset, pageable);
        } else {
            page = auditLogRepository.findByEntityTypeInAndNumberGreaterThanAndCurrentTrue(
                    types, offset, pageable);
        }

        return page.getContent().stream()
                .map(this::toSync)
                .peek(sync -> listeners.forEach(l -> l.onRead(sync)))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public List<? extends ApiObject> write(List<Sync<? extends ApiObject>> syncs) {
        return syncs.stream()
                .sorted((left, right) -> {
                    if (left.getType() == right.getType()) {
                        return 0;
                    } else if (left.getType() == SyncType.CREATE) {
                        return -1;
                    } else if (right.getType() == SyncType.CREATE) {
                        return 1;
                    }
                    return 0;
                })
                .peek(sync -> listeners.forEach(l -> l.onWrite(sync)))
                .map(sync -> {
                    checkAnnotations(sync.getObject().getClass());
                    ApiObject entity = sync.getObject();
                    switch (sync.getType()) {
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
                            throw new SyncException("Can't sync: unknown " + sync.getType() + " sync type");
                    }
                })
                .filter(Objects::nonNull)
                .peek(entity -> postListeners.forEach(l -> l.afterWrite(entity)))
                .collect(Collectors.toList());
    }

    private Sync<? extends ApiObject> toSync(AuditLog<? extends ApiObject> auditLog) {
        AuditLog<? extends ApiObject> cached = cache.getIfPresent(auditLog.getNumber());
        if (cached == null) {
            fetchEntity(auditLog);
            cache.put(auditLog.getNumber(), auditLog);
        } else {
            auditLog = cached;
        }
        ApiObject object;
        switch (auditLog.getAction()) {
            case CREATE:
                object = unproxy(auditLog.getEntity());
                return Sync.of(object, SyncType.CREATE, auditLog.getNumber(), auditLog.getTransactionId());
            case UPDATE:
                object = unproxy(auditLog.getEntity());
                return Sync.of(object, SyncType.UPDATE, auditLog.getNumber(), auditLog.getTransactionId());
            case DELETE:
                return Sync.of(toDeleted(auditLog), SyncType.DELETE, auditLog.getNumber(), auditLog.getTransactionId());
            default:
                throw new SyncException("Unknown auditLog action " + auditLog.getAction());
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

    private Deleted toDeleted(AuditLog<?> auditLog) {
        Class<?> type;
        try {
            type = Class.forName(auditLog.getEntityType());
        } catch (ClassNotFoundException e) {
            throw new SyncException("Unknown type: " + auditLog.getEntityType(), e);
        }
        return Deleted.of(type.getSimpleName(), auditLog.getEntityId());
    }

    private <T extends ApiObject> void fetchEntity(AuditLog<T> auditLog) {
        try {
            Sync<? extends ApiObject> sync = Sync.of(
                    auditLog.getAction() != DataAction.DELETE
                            ? auditLog.getEntity()
                            : toDeleted(auditLog),
                    null, null, null);
            objectMapper.writeValueAsString(sync);
        } catch (JsonProcessingException e) {
            throw new SyncException("Exception during cache update of AuditLog #"
                    + auditLog.getNumber());
        }
    }
}