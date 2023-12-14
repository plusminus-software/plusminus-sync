package software.plusminus.sync.service.fetcher;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import software.plusminus.data.repository.DataRepository;
import software.plusminus.sync.EntityWithUuid;
import software.plusminus.test.IntegrationTest;

import java.util.Optional;
import java.util.UUID;

import static software.plusminus.check.Checks.check;

public class ByUuidFinderTest extends IntegrationTest {
    
    @Autowired
    private ByUuidFinder finder;
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
        Optional<EntityWithUuid> fetched = finder.find(entity);
        check(fetched).is(entity);
    }
    
    @Test
    public void fetchMissedUuid() {
        entity.setUuid(UUID.randomUUID());
        Optional<EntityWithUuid> fetched = finder.find(entity);
        check(fetched).isEmpty();
    }

}