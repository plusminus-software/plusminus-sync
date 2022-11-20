package software.plusminus.sync.service.fetcher;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import software.plusminus.data.repository.DataRepository;
import software.plusminus.sync.EntityWithUuid;
import software.plusminus.sync.dto.Sync;
import software.plusminus.sync.dto.SyncType;

import java.util.Optional;
import java.util.UUID;

import static software.plusminus.check.Checks.check;

@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("test")
public class ByUuidFetcherTest {
    
    @Autowired
    private ByUuidFetcher fetcher;
    @Autowired
    private DataRepository repository;
    
    private UUID uuid;
    private EntityWithUuid entity;
    
    @Before
    public void before() {
        uuid = UUID.randomUUID();
        entity = new EntityWithUuid();
        entity.setUuid(uuid);
        entity.setMyField("test value");
        
        repository.save(entity);
    }
    
    @After
    public void after() {
        repository.delete(entity);
    }
    
    @Test
    public void fetchEntityByUuid() {
        Sync<EntityWithUuid> sync = Sync.of(entity, SyncType.CREATE, null);
        Optional<EntityWithUuid> fetched = fetcher.fetch(sync);
        check(fetched).is(entity);
    }
    
    @Test
    public void fetchMissedUuid() {
        entity.setUuid(UUID.randomUUID());
        Sync<EntityWithUuid> sync = Sync.of(entity, SyncType.CREATE, null);
        Optional<EntityWithUuid> fetched = fetcher.fetch(sync);
        check(fetched).isEmpty();
    }

}