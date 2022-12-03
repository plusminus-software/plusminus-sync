package software.plusminus.sync.service.listener;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import software.plusminus.data.repository.DataRepository;
import software.plusminus.sync.EntityWithUuid;
import software.plusminus.sync.dto.Sync;
import software.plusminus.sync.dto.SyncType;
import software.plusminus.sync.service.fetcher.ByUuidFinder;
import software.plusminus.sync.service.merger.VersionMerger;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static software.plusminus.check.Checks.check;

@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("test")
public class MergeSyncListenerTest {
    
    @Autowired
    private MergeSyncListener mergeSyncListener;
    @Autowired
    private DataRepository repository;
    @SpyBean
    private ByUuidFinder finder;
    @SpyBean
    private VersionMerger merger;
    
    private EntityWithUuid entity;

    @Before
    public void before() {
        entity = createEntity();
        repository.save(entity);
    }
    
    @After
    public void after() {
        repository.delete(entity);
    }

    @Test
    public void doNotProcessIfNoSupportedMergers() {
        doReturn(false).when(merger).supports(any());
        Sync<EntityWithUuid> sync = Sync.of(entity, SyncType.UPDATE, null);
        
        mergeSyncListener.onWrite(sync);
        
        verify(merger, never()).process(any(), any());
    }
    
    @Test
    public void doNotProcessIfNoFoundEntityOnCreate() {
        entity.setUuid(UUID.randomUUID());
        Sync<EntityWithUuid> sync = Sync.of(entity, SyncType.CREATE, null);
        
        mergeSyncListener.onWrite(sync);
        
        verify(merger, never()).process(any(), any());
    }
    
    @Test
    public void doNotProcessIfNoFoundEntityOnUpdate() {
        entity.setId(321L);
        Sync<EntityWithUuid> sync = Sync.of(entity, SyncType.UPDATE, null);
        
        mergeSyncListener.onWrite(sync);
        
        verify(merger, never()).process(any(), any());
    }
    
    @Test
    public void processIfThereAreSupportedMergers() {
        Sync<EntityWithUuid> sync = Sync.of(entity, SyncType.UPDATE, null);
        mergeSyncListener.onWrite(sync);
        verify(merger).process(eq(entity), same(sync));
    }
    
    @Test
    public void createIsChangedToUpdateIfFound() {
        Long id = entity.getId();
        entity.setId(null);
        Sync<EntityWithUuid> sync = Sync.of(entity, SyncType.CREATE, null);
        
        mergeSyncListener.onWrite(sync);
        
        check(sync.getType()).is(SyncType.UPDATE);
        check(sync.getObject().getId()).is(id);
    }
    
    @Test
    public void versionIsSetToZeroIfCreateIsChangedToUpdate() {
        entity.setId(null);
        entity.setVersion(null);
        Sync<EntityWithUuid> sync = Sync.of(entity, SyncType.CREATE, null);
        
        mergeSyncListener.onWrite(sync);
        
        check(sync.getType()).is(SyncType.UPDATE);
        check(sync.getObject().getVersion()).is(0L);
    }
    
    @Test
    public void finderIsUsedOnCreateSync() {
        Sync<EntityWithUuid> sync = Sync.of(entity, SyncType.CREATE, null);
        mergeSyncListener.onWrite(sync);
        verify(finder).find(entity);
    }
    
    @Test
    public void finderIsNotUsedOnUpdateSync() {
        Sync<EntityWithUuid> sync = Sync.of(entity, SyncType.UPDATE, null);
        mergeSyncListener.onWrite(sync);
        verify(finder, never()).find(any());
    }
    
    private EntityWithUuid createEntity() {
        EntityWithUuid e = new EntityWithUuid();
        e.setUuid(UUID.randomUUID());
        e.setMyField("one");
        return e;
    }

}