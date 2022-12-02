package software.plusminus.sync.service.version;

import lombok.Data;
import org.junit.Test;
import software.plusminus.json.model.ApiObject;
import software.plusminus.sync.EntityWithUuid;
import software.plusminus.sync.TestEntity;
import software.plusminus.sync.annotation.Syncable;
import software.plusminus.sync.annotation.Uuid;

import java.lang.reflect.Field;
import java.util.Optional;
import java.util.UUID;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Version;

import static software.plusminus.check.Checks.check;

public class SyncVersionServiceTest {
    
    private SyncVersionService versionService = new SyncVersionService();
    
    @Test
    public void findVersionField() {
        TestEntity entity = new TestEntity();
        Optional<Field> versionField = versionService.findVersionField(entity);
        check(versionField).isPresent();
        check(versionField.get().getName()).is("version");
    }

    @Test
    public void populateVersion() {
        EntityWithUuid e1 = new EntityWithUuid();
        e1.setVersion(10L);
        EntityWithUuid e2 = new EntityWithUuid();
        e2.setVersion(20L);

        versionService.populateVersion(e1, e2);
        
        check(e2.getVersion()).is(10L);
    }
    
    @Test
    public void populateVersionWithPrimitiveType() {
        EntityWithPrimitiveVersion e1 = new EntityWithPrimitiveVersion();
        e1.setVersion(10);
        EntityWithPrimitiveVersion e2 = new EntityWithPrimitiveVersion();
        e2.setVersion(20);

        versionService.populateVersion(e1, e2);
        
        check(e2.getVersion()).is(10);
    }

    @Data
    @Syncable
    @Entity
    private static class EntityWithPrimitiveVersion implements ApiObject {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;
        @Uuid
        private UUID uuid;
        @Version
        private long version;
        private String myField;

    }

}