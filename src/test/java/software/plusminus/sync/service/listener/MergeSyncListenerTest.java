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
import software.plusminus.sync.service.fetcher.ByUuidFetcher;
import software.plusminus.sync.service.merger.EqualsMerger;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
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
    private ByUuidFetcher byUuidFetcher;
    @SpyBean
    private EqualsMerger equalsMerger;
    
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
    public void equalsEntityOnCreate() {
        Sync<EntityWithUuid> sync = Sync.of(entity, SyncType.CREATE, null);
        
        mergeSyncListener.onWrite(sync);
        
        check(sync.getType()).is(SyncType.TURN_BACK);
        check(sync.getObject()).is(entity);
        verify(byUuidFetcher).fetch(sync);
        verify(equalsMerger).process(any(), same(sync));
    }
    
    @Test
    public void equalsEntityOnCreateWithNullIdAndVersion() {
        EntityWithUuid toSync = createEntity();
        toSync.setUuid(entity.getUuid());
        toSync.setId(null);
        toSync.setVersion(null);
        Sync<EntityWithUuid> sync = Sync.of(toSync, SyncType.CREATE, null);
        
        mergeSyncListener.onWrite(sync);
        
        check(sync.getType()).is(SyncType.TURN_BACK);
        check(sync.getObject()).is(entity);
        verify(byUuidFetcher).fetch(sync);
        verify(equalsMerger).process(any(), same(sync));
    }

    @Test
    public void equalsEntityOnUpdate() {
        Sync<EntityWithUuid> sync = Sync.of(entity, SyncType.UPDATE, null);
        
        mergeSyncListener.onWrite(sync);
        
        check(sync.getType()).is(SyncType.TURN_BACK);
        check(sync.getObject()).is(entity);
        verify(byUuidFetcher, never()).fetch(sync);
        verify(equalsMerger).process(any(), same(sync));
    }
    
    private EntityWithUuid createEntity() {
        EntityWithUuid e = new EntityWithUuid();
        e.setUuid(UUID.randomUUID());
        e.setMyField("one");
        return e;
    }

}