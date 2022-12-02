package software.plusminus.sync.service.version;

import org.springframework.stereotype.Service;
import software.plusminus.sync.exception.SyncException;
import software.plusminus.util.FieldUtils;

import java.lang.reflect.Field;
import java.util.Optional;
import java.util.stream.Stream;

@Service
public class SyncVersionService {

    @SuppressWarnings("squid:S1872")
    public <T> Optional<Field> findVersionField(T source) {
        return FieldUtils.findFirst(source.getClass(), f -> Stream.of(f.getAnnotations())
                .anyMatch(annotation -> annotation.annotationType().getSimpleName().equals("Version")));
    }

    public <T> void populateVersion(T source, T target) {
        Optional<Field> versionField = findVersionField(source);
        if (!versionField.isPresent()) {
            throw new SyncException("@Version field is missed in " + source.getClass() + " class");
        }
        Object sourceVersion = FieldUtils.read(source, versionField.get());
        if (!(sourceVersion instanceof Number)) {
            return;
        }

        FieldUtils.write(target, sourceVersion, versionField.get());
    }

}
