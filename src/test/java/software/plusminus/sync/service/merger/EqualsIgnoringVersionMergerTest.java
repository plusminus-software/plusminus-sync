package software.plusminus.sync.service.merger;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import software.plusminus.check.util.JsonUtils;
import software.plusminus.sync.EntityWithoutVersion;
import software.plusminus.sync.TestEntity;
import software.plusminus.sync.dto.Sync;
import software.plusminus.sync.dto.SyncType;
import software.plusminus.sync.service.jsog.SyncJsonService;
import software.plusminus.sync.service.version.SyncVersionService;

import static software.plusminus.check.Checks.check;

@RunWith(MockitoJUnitRunner.class)
public class EqualsIgnoringVersionMergerTest {
    
    @Spy
    private SyncJsonService jsonService = new SyncJsonService();
    @Spy
    private SyncVersionService versionService = new SyncVersionService();
    @InjectMocks
    private EqualsIgnoringVersionMerger merger;
    
    @Test
    public void doesNotSupportEntitiesWithoutVersionField() {
        EntityWithoutVersion entity = new EntityWithoutVersion();
        entity.setId(10L);
        entity.setMyField("someText");
        Sync<EntityWithoutVersion> sync = Sync.of(entity, SyncType.UPDATE, null);

        boolean result = merger.supports(sync);

        check(result).isFalse();
    }
    
    @Test
    public void supportsEntitiesWithVersionField() {
        TestEntity inSync = readTestEntity();
        Sync<TestEntity> sync = Sync.of(inSync, SyncType.UPDATE, null);

        boolean result = merger.supports(sync);

        check(result).isTrue();
    }
    
    @Test
    public void mergeFullyEqualObjects() {
        TestEntity current = readTestEntity();
        TestEntity inSync = readTestEntity();
        Sync<TestEntity> sync = Sync.of(inSync, SyncType.UPDATE, null);
        
        merger.process(current, sync);
        
        check(sync.getObject()).isSame(inSync);
    }
    
    @Test
    public void mergeEqualObjectsIgnoringVersionField() {
        TestEntity current = readTestEntity();
        current.setVersion(10L);
        TestEntity inSync = readTestEntity();
        Sync<TestEntity> sync = Sync.of(inSync, SyncType.UPDATE, null);
        
        merger.process(current, sync);
        
        check(sync.getObject()).isSame(inSync);
        check(inSync.getVersion()).is(10L);
    }
    
    @Test
    public void notMergeDifferentObjectsOnCreate() {
        TestEntity current = readTestEntity();
        current.setVersion(10L);
        TestEntity inSync = readTestEntity();
        inSync.setMyField("different");
        Sync<TestEntity> sync = Sync.of(inSync, SyncType.UPDATE, null);
        
        merger.process(current, sync);
        
        check(sync.getType()).is(SyncType.UPDATE);
        check(sync.getObject()).isSame(inSync);
        check(current.getVersion()).is(10L);
        check(sync.getObject().getVersion()).is(0L);
    }

    private TestEntity readTestEntity() {
        return JsonUtils.fromJson("/json/entity.json", TestEntity.class);
    }

}