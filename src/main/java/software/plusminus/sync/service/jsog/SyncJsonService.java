package software.plusminus.sync.service.jsog;

import com.fasterxml.jackson.annotation.JsonFilter;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.ser.FilterProvider;
import com.fasterxml.jackson.databind.ser.PropertyWriter;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import org.springframework.stereotype.Service;
import software.plusminus.check.exception.JsonException;
import software.plusminus.util.ObjectUtils;

import java.util.function.Predicate;
import java.util.stream.Stream;

@Service
public class SyncJsonService {

    public String toJson(Object object, Predicate<PropertyWriter> filter) {
        ObjectMapper mapper = new ObjectMapper();
        FilterProvider filterProvider = new SimpleFilterProvider()
                .addFilter("DynamicFilter", new DynamicFilter(filter));
        mapper.setFilterProvider(filterProvider);
        findReferences(object)
                .forEach(c -> mapper.addMixIn(c, DynamicFilterMixin.class));
        try {
            return mapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new JsonException(e);
        }
    }
    
    private Stream<Class<?>> findReferences(Object object) {
        return ObjectUtils.findReferences(object).stream()
                .map(Object::getClass);
    }

    private static final class DynamicFilter extends SimpleBeanPropertyFilter {

        private Predicate<PropertyWriter> filter;

        private DynamicFilter(Predicate<PropertyWriter> filter) {
            this.filter = filter;
        }

        @Override
        protected boolean include(BeanPropertyWriter writer) {
            return include((PropertyWriter) writer);
        }

        @Override
        protected boolean include(PropertyWriter writer) {
            return filter.test(writer);
        }
    }

    @JsonFilter("DynamicFilter")
    private interface DynamicFilterMixin {
    }

}
