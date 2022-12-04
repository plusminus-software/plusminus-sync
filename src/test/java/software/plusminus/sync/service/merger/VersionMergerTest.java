package software.plusminus.sync.service.merger;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import software.plusminus.check.util.JsonUtils;
import software.plusminus.sync.EmbeddedObject;
import software.plusminus.sync.EntityWithoutVersion;
import software.plusminus.sync.InnerEntity;
import software.plusminus.sync.TestEntity;
import software.plusminus.sync.dto.Sync;
import software.plusminus.sync.dto.SyncType;
import software.plusminus.sync.service.jsog.SyncJsonService;
import software.plusminus.sync.service.version.SyncVersionService;

import static software.plusminus.check.Checks.check;

@RunWith(MockitoJUnitRunner.class)
public class VersionMergerTest {
    
    @Spy
    private SyncJsonService jsonService = new SyncJsonService();
    @Spy
    private SyncVersionService versionService = new SyncVersionService();
    @InjectMocks
    private VersionMerger merger;
    
    @Test
    public void notSupportsEntitiesWithoutVersionField() {
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
    public void mergeEqualObjectsIgnoringVersionField() {
        TestEntity current = readTestEntity();
        current.setVersion(10L);
        current.setInnerEntity(new InnerEntity());
        TestEntity inSync = readTestEntity();
        inSync.setVersion(5L);
        inSync.setInnerEntity(new InnerEntity());
        Sync<TestEntity> sync = Sync.of(inSync, SyncType.UPDATE, null);
        
        merger.process(current, sync);
        
        check(sync.getObject()).isSame(inSync);
        check(inSync.getVersion()).is(10L);
    }
    
    
    @Test
    public void mergeEqualObjectsIgnoringInnerEntity() {
        InnerEntity currentInnerEntity = new InnerEntity();
        currentInnerEntity.setId(2L);
        currentInnerEntity.setMyField("current");
        TestEntity current = readTestEntity();
        current.setVersion(10L);
        current.setInnerEntity(currentInnerEntity);
        InnerEntity isSyncInnerEntity = new InnerEntity();
        isSyncInnerEntity.setId(2L);
        isSyncInnerEntity.setMyField("inSync");
        TestEntity inSync = readTestEntity();
        inSync.setVersion(5L);
        inSync.setInnerEntity(isSyncInnerEntity);
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
        inSync.setVersion(5L);
        inSync.setMyField("different");
        Sync<TestEntity> sync = Sync.of(inSync, SyncType.UPDATE, null);
        
        merger.process(current, sync);
        
        check(sync.getType()).is(SyncType.UPDATE);
        check(sync.getObject()).isSame(inSync);
        check(current.getVersion()).is(10L);
        check(sync.getObject().getVersion()).is(5L);
    }

    @Test
    public void notMergeObjectsWithEqualsVersions() {
        TestEntity current = readTestEntity();
        current.setMyField("current value");
        current.setVersion(3L);
        TestEntity inSync = readTestEntity();
        inSync.setMyField("value to update");
        inSync.setVersion(3L);
        Sync<TestEntity> sync = Sync.of(inSync, SyncType.UPDATE, null);

        merger.process(current, sync);

        check(sync.getObject()).isSame(inSync);
        check(sync.getObject().getVersion()).is(3L);
        check(sync.getObject().getMyField()).is("value to update");
    }
    
    
    @Test
    public void notMergeObjectsWithDifferentInnerNonEntityObject() {
        EmbeddedObject currentEmbedded = new EmbeddedObject();
        currentEmbedded.setEmbeddedString("current");
        TestEntity current = readTestEntity();
        current.setVersion(10L);
        current.setEmbeddedObject(currentEmbedded);
        EmbeddedObject isSyncEmbedded = new EmbeddedObject();
        isSyncEmbedded.setEmbeddedString("inSync");
        TestEntity inSync = readTestEntity();
        inSync.setVersion(5L);
        inSync.setEmbeddedObject(isSyncEmbedded);
        Sync<TestEntity> sync = Sync.of(inSync, SyncType.UPDATE, null);

        merger.process(current, sync);

        check(sync.getObject()).isSame(inSync);
        check(sync.getObject().getVersion()).is(5L);
        check(sync.getObject().getEmbeddedObject().getEmbeddedString()).is("inSync");
    }

    private TestEntity readTestEntity() {
        return JsonUtils.fromJson("/json/entity.json", TestEntity.class);
    }

}