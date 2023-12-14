package software.plusminus.sync.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.ser.BeanSerializer;
import org.apache.commons.lang3.StringUtils;
import org.springframework.lang.Nullable;
import software.plusminus.json.model.Classable;
import software.plusminus.sync.annotation.Uuid;
import software.plusminus.util.FieldUtils;
import software.plusminus.util.MethodUtils;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Optional;
import java.util.stream.Stream;

public class DehydrationSerializer extends JsonSerializer<Object> {

    private DehydrationContext context;
    private BeanSerializer defaultSerializer;

    public DehydrationSerializer(DehydrationContext context, BeanSerializer serializer) {
        this.context = context;
        defaultSerializer = serializer;
    }

    @Override
    public void serialize(Object value, JsonGenerator jgen, SerializerProvider provider) throws IOException {
        if (context.shouldDehydrate()) {
            writeDehydratedObject(value, jgen, provider);
            return;
        }
        context.runWithDehydration(() -> {
            try {
                defaultSerializer.serialize(value, jgen, provider);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
    }

    @Override
    public void serializeWithType(Object value, JsonGenerator gen, SerializerProvider serializers,
                                  TypeSerializer typeSer) throws IOException {
        serialize(value, gen, serializers);
    }

    private void writeDehydratedObject(Object value, JsonGenerator jgen,
                                       SerializerProvider provider) throws IOException {
        jgen.writeStartObject();
        jgen.writeBooleanField(":dehydrated", true);
        if (value instanceof Classable) {
            Classable classable = Classable.class.cast(value);
            jgen.writeStringField("class", classable.getClazz());
        }
        Field idField = FieldUtils.findFirst(value.getClass(), field -> Stream.of(field.getDeclaredAnnotations())
                        .anyMatch(a -> a.annotationType().getSimpleName().equalsIgnoreCase("id")))
                .orElseThrow(IllegalArgumentException::new);
        writeField("id", read(value, idField), jgen, provider);
        Optional<Field> uuidField = FieldUtils.findFirstWithAnnotation(value.getClass(), Uuid.class);
        if (uuidField.isPresent()) {
            writeField("uuid", read(value, uuidField.get()), jgen, provider);
        }
        jgen.writeEndObject();
    }

    private void writeField(String fieldName, @Nullable Object fieldValue,
                            JsonGenerator jgen, SerializerProvider provider) throws IOException {
        if (fieldValue == null) {
            jgen.writeNullField(fieldName);
            return;
        }
        JsonSerializer<Object> valueSerializer = provider.findValueSerializer(fieldValue.getClass());
        jgen.writeFieldName(fieldName);
        valueSerializer.serialize(fieldValue, jgen, provider);
    }

    private Object read(Object object, Field field) {
        String getterName = "get" + StringUtils.capitalize(field.getName());
        Method getter = MethodUtils.getMethodsStream(object.getClass())
                .filter(method -> method.getName().equals(getterName))
                .findFirst()
                .orElseThrow(IllegalArgumentException::new);
        getter.setAccessible(true);
        try {
            return getter.invoke(object);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new IllegalStateException(e);
        }
    }
}
