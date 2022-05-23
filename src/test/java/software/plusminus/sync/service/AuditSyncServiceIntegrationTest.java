package software.plusminus.sync.service;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import software.plusminus.data.repository.DataRepository;
import software.plusminus.json.model.ApiObject;
import software.plusminus.sync.InnerEntity;
import software.plusminus.sync.TestEntity;
import software.plusminus.sync.dto.Sync;
import software.plusminus.sync.dto.SyncType;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("test")
public class AuditSyncServiceIntegrationTest {

    @Autowired
    private AuditSyncService syncService;
    @Autowired
    private DataRepository dataRepository;
    
    @Test
    public void writeWithSameInnerEntity() {
        InnerEntity innerEntity = new InnerEntity();
        innerEntity = dataRepository.save(innerEntity);
        TestEntity entityOne = new TestEntity();
        entityOne.setInnerEntity(innerEntity);
        entityOne = dataRepository.save(entityOne);
        TestEntity entityTwo = new TestEntity();
        entityTwo.setInnerEntity(innerEntity);
        entityTwo = dataRepository.save(entityTwo);

        List<TestEntity> result = (List<TestEntity>) syncService.write(Arrays.asList(
                toSyncItem(entityOne), toSyncItem(entityTwo)));

        assertThat(result.get(0).getInnerEntity()).isSameAs(result.get(1).getInnerEntity());
    }

    @SuppressWarnings("PMD.UnusedPrivateMethod")
    private <T extends ApiObject> Sync<T> toSyncItem(T object) {
        Sync<T> sync = new Sync<>();
        sync.setObject(object);
        sync.setType(SyncType.UPDATE);
        return sync;
    }
}