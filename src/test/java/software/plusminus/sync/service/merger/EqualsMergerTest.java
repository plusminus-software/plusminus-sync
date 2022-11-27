package software.plusminus.sync.service.merger;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import software.plusminus.check.util.JsonUtils;
import software.plusminus.sync.TestEntity;
import software.plusminus.sync.dto.Sync;
import software.plusminus.sync.dto.SyncType;
import software.plusminus.sync.service.jsog.SyncJsonService;

import static software.plusminus.check.Checks.check;

@RunWith(MockitoJUnitRunner.class)
public class EqualsMergerTest {
    
    @Spy
    private SyncJsonService jsonService = new SyncJsonService();
    @InjectMocks
    private EqualsMerger merger;
    
    @Test
    public void mergeEqualObjectsOnCreate() {
        TestEntity current = readTestEntity();
        TestEntity inSync = readTestEntity();
        inSync.setId(null);
        inSync.setVersion(null);
        Sync<TestEntity> sync = Sync.of(inSync, SyncType.CREATE, null);
        
        merger.process(current, sync);
        
        check(sync.getType()).is(SyncType.TURN_BACK);
        check(sync.getObject()).isSame(current);
    }
    
    @Test
    public void mergeEqualObjectsOnUpdate() {
        TestEntity current = readTestEntity();
        TestEntity inSync = readTestEntity();
        inSync.setVersion(2L);
        Sync<TestEntity> sync = Sync.of(inSync, SyncType.UPDATE, null);
        
        merger.process(current, sync);
        
        check(sync.getType()).is(SyncType.TURN_BACK);
        check(sync.getObject()).isSame(current);
    }
    
    @Test
    public void notMergeDifferentObjectsOnCreate() {
        TestEntity current = readTestEntity();
        TestEntity inSync = readTestEntity();
        inSync.setMyField("different");
        Sync<TestEntity> sync = Sync.of(inSync, SyncType.CREATE, null);
        
        merger.process(current, sync);
        
        check(sync.getType()).is(SyncType.CREATE);
        check(sync.getObject()).isSame(inSync);
    }
    
    @Test
    public void notMergeDifferentObjectsOnUpdate() {
        TestEntity current = readTestEntity();
        TestEntity inSync = readTestEntity();
        inSync.setMyField("different version");
        Sync<TestEntity> sync = Sync.of(inSync, SyncType.UPDATE, null);
        
        merger.process(current, sync);
        
        check(sync.getType()).is(SyncType.UPDATE);
        check(sync.getObject()).isSame(inSync);
    }

    private TestEntity readTestEntity() {
        return JsonUtils.fromJson("/json/entity.json", TestEntity.class);
    }

}