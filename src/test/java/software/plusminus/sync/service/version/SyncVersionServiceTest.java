package software.plusminus.sync.service.version;

import lombok.Data;
import org.junit.Test;
import software.plusminus.json.model.ApiObject;
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
    public void findVersionFieldWithPrimitiveType() {
        EntityWithPrimitiveVersion entity = new EntityWithPrimitiveVersion();
        Optional<Field> versionField = versionService.findVersionField(entity);
        check(versionField).isPresent();
        check(versionField.get().getName()).is("version");
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