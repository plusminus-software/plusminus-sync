package software.plusminus.sync.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import software.plusminus.audit.model.AuditLog;
import software.plusminus.audit.model.DataAction;
import software.plusminus.audit.repository.AuditLogRepository;
import software.plusminus.audit.service.DeviceContext;
import software.plusminus.check.util.JsonUtils;
import software.plusminus.data.service.data.DataService;
import software.plusminus.data.service.entity.EntityService;
import software.plusminus.json.model.ApiObject;
import software.plusminus.sync.InnerEntity;
import software.plusminus.sync.NoAnnotationsEntity;
import software.plusminus.sync.TestEntity;
import software.plusminus.sync.dto.Deleted;
import software.plusminus.sync.dto.Sync;
import software.plusminus.sync.dto.SyncType;
import software.plusminus.sync.exception.SyncException;
import software.plusminus.sync.service.listener.SyncListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class AuditSyncServiceTest {

    @Mock
    private DeviceContext deviceContext;
    @Mock
    private DataService dataService;
    @Mock
    private EntityService entityService;
    @Spy
    private List<SyncListener> listeners = new ArrayList<>();
    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();
    @Mock
    private AuditLogRepository auditLogRepository;

    @InjectMocks
    private AuditSyncService syncService;

    @Captor
    private ArgumentCaptor<TestEntity> captor;

    @Test
    public void read() {
        // given
        TestEntity entity1 = readTestEntity();
        entity1.setId(2L);
        TestEntity entity2 = readTestEntity();
        entity1.setId(3L);
        Deleted deleted = Deleted.of(TestEntity.class.getSimpleName(), 4L);
        UUID transaction1 = UUID.randomUUID();
        UUID transaction2 = UUID.randomUUID();

        AuditLog<TestEntity> auditLog1 = new AuditLog<>();
        auditLog1.setNumber(5L);
        auditLog1.setTransactionId(transaction1);
        auditLog1.setEntity(entity1);
        auditLog1.setAction(DataAction.CREATE);
        AuditLog<TestEntity> auditLog2 = new AuditLog<>();
        auditLog2.setNumber(6L);
        auditLog2.setEntity(entity2);
        auditLog2.setTransactionId(transaction2);
        auditLog2.setAction(DataAction.UPDATE);
        AuditLog<TestEntity> auditLog3 = new AuditLog<>();
        auditLog3.setNumber(7L);
        auditLog3.setTransactionId(null);
        auditLog3.setEntityId(4L);
        auditLog3.setEntityType(TestEntity.class.getName());
        auditLog3.setAction(DataAction.DELETE);

        List<String> types = Arrays.asList("Type1", "Type2");
        String ignoreDevice = "test client";
        Long offset = 4L;
        Pageable pageable = PageRequest.of(0, 3, Sort.by(Sort.Direction.DESC, "number"));
        Page<AuditLog<?>> page = new PageImpl(Arrays.asList(auditLog1, auditLog2, auditLog3), pageable, 100L);
        when(auditLogRepository.findByEntityTypeInAndDeviceIsNotAndNumberGreaterThanAndCurrentTrue(
                Arrays.asList(TestEntity.class.getName(), InnerEntity.class.getName()),
                ignoreDevice,
                offset,
                pageable))
                .thenReturn(page);
        when(deviceContext.currentDevice()).thenReturn(ignoreDevice);
        when(entityService.findClass("Type1")).thenReturn((Class) TestEntity.class);
        when(entityService.findClass("Type2")).thenReturn((Class) InnerEntity.class);

        // when
        List<Sync<? extends ApiObject>> entities = syncService.read(
                types, true, offset, 3, Sort.Direction.DESC);

        // then
        assertThat(entities).containsExactly(
                Sync.of(entity1, SyncType.CREATE, 5L, transaction1),
                Sync.of(entity2, SyncType.UPDATE, 6L, transaction2),
                Sync.of(deleted, SyncType.DELETE, 7L, null));
    }
    
    @Test(expected = SyncException.class)
    public void readIncorrectEntity() {
        when(entityService.findClass("Type1")).thenReturn((Class) NoAnnotationsEntity.class);
        syncService.read(Collections.singletonList("Type1"), true, 4L, 3, Sort.Direction.DESC);
    }

    @Test
    public void write() {
        // given
        TestEntity entity1 = readTestEntity();
        entity1.setId(2L);
        TestEntity entity1Result = readTestEntity();
        entity1Result.setId(20L);
        TestEntity entity2 = readTestEntity();
        entity2.setId(3L);
        TestEntity entity2Result = readTestEntity();
        entity2Result.setId(30L);
        when(dataService.create(entity1)).thenReturn(entity1Result);
        when(dataService.create(entity2)).thenReturn(entity2Result);

        // when
        List entities = syncService.write(Arrays.asList(toSyncItem(entity1), toSyncItem(entity2)));

        // then
        verify(dataService, times(2)).create(captor.capture());
        assertThat(captor.getAllValues()).containsExactly(entity1, entity2);
        assertThat(entities).containsExactly(entity1Result, entity2Result);
    }
    
    @Test(expected = SyncException.class)
    public void writeIncorrectEntity() {
        NoAnnotationsEntity entity1 = readNoAnnotationsEntity();
        syncService.write(Collections.singletonList(toSyncItem(entity1)));
    }

    @SuppressWarnings("PMD.UnusedPrivateMethod")
    private <T extends ApiObject> Sync<T> toSyncItem(T object) {
        Sync<T> sync = new Sync<>();
        sync.setObject(object);
        sync.setType(SyncType.CREATE);
        return sync;
    }

    private TestEntity readTestEntity() {
        return JsonUtils.fromJson("/json/entity.json", TestEntity.class);
    }
    
    private NoAnnotationsEntity readNoAnnotationsEntity() {
        return JsonUtils.fromJson("/json/no-annotations-entity.json", NoAnnotationsEntity.class);
    }
}