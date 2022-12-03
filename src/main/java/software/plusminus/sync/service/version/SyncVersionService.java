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
    public Optional<Field> findVersionField(Object object) {
        return FieldUtils.findFirst(object.getClass(), f -> Stream.of(f.getAnnotations())
                .anyMatch(annotation -> annotation.annotationType().getSimpleName().equals("Version")));
    }
    
    public Field findVersionFieldOrException(Object object) {
        Optional<Field> versionField = findVersionField(object);
        if (!versionField.isPresent()) {
            throw new SyncException("@Version field is missed in " + object.getClass() + " class");
        }
        return versionField.get();
    }

}
