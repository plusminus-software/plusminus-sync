package software.plusminus.sync.service.jsog;

import com.fasterxml.jackson.annotation.JsonFilter;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.FilterProvider;
import com.fasterxml.jackson.databind.ser.PropertyWriter;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import org.springframework.boot.json.JsonParseException;
import org.springframework.stereotype.Service;
import software.plusminus.util.ObjectUtils;

import java.util.function.BiPredicate;
import java.util.stream.Stream;

@Service
public class SyncJsonService {

    public String toJson(Object object, BiPredicate<Object, PropertyWriter> filter) {
        ObjectMapper mapper = new ObjectMapper();
        FilterProvider filterProvider = new SimpleFilterProvider()
                .addFilter("DynamicFilter", new DynamicFilter(filter));
        mapper.setFilterProvider(filterProvider);
        findReferences(object)
                .forEach(c -> mapper.addMixIn(c, DynamicFilterMixin.class));
        try {
            return mapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new JsonParseException(e);
        }
    }
    
    private Stream<Class<?>> findReferences(Object object) {
        return ObjectUtils.findReferences(object).stream()
                .map(Object::getClass);
    }

    private static final class DynamicFilter extends SimpleBeanPropertyFilter {

        private BiPredicate<Object, PropertyWriter> filter;

        private DynamicFilter(BiPredicate<Object, PropertyWriter> filter) {
            this.filter = filter;
        }

        @Override
        public void serializeAsField(Object pojo,
                                     JsonGenerator jgen,
                                     SerializerProvider provider,
                                     PropertyWriter writer) throws Exception {
            if (filter.test(pojo, writer)) {
                super.serializeAsField(pojo, jgen, provider, writer);
            }
        }
    }

    @JsonFilter("DynamicFilter")
    private interface DynamicFilterMixin {
    }

}
