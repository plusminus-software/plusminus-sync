package software.plusminus.sync.config;

import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.BeanSerializer;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;
import com.fasterxml.jackson.databind.ser.impl.ObjectIdWriter;
import com.voodoodyne.jackson.jsog.JSOGRefSerializer;
import org.hibernate.proxy.HibernateProxy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import software.plusminus.util.AnnotationUtils;
import software.plusminus.util.FieldUtils;

import java.lang.reflect.Field;
import javax.persistence.Entity;

@Configuration
public class DehydrationConfig {

    @Autowired
    private DehydrationContext dehydrationContext;

    @Autowired
    public void jacksonConfiguration(ObjectMapper objectMapper) {
        objectMapper.registerModule(new SimpleModule() {
            @Override
            public void setupModule(SetupContext context) {
                super.setupModule(context);
                context.addBeanSerializerModifier(new BeanSerializerModifier() {
                    @Override
                    public JsonSerializer<?> modifySerializer(
                            SerializationConfig config, BeanDescription desc, JsonSerializer<?> serializer) {
                        Class<?> beanClass = desc.getBeanClass();
                        if (HibernateProxy.class.isAssignableFrom(beanClass)) {
                            beanClass = beanClass.getSuperclass();
                        }
                        if (AnnotationUtils.findAnnotation(Entity.class, beanClass) != null
                                && serializer instanceof BeanSerializer) {
                            BeanSerializer beanSerializer = (BeanSerializer) serializer;
                            fixBugWithJsog(beanSerializer);
                            return new DehydrationSerializer(dehydrationContext, beanSerializer);
                        }
                        return serializer;
                    }
                });
            }
        });
    }

    private void fixBugWithJsog(BeanSerializer serializer) {
        ObjectIdWriter objectIdWriter = FieldUtils.readFirstWithType(serializer, ObjectIdWriter.class);
        if (objectIdWriter != null && objectIdWriter.serializer == null) {
            Field serializerField = FieldUtils.findFirstWithType(ObjectIdWriter.class, JsonSerializer.class)
                    .orElseThrow(IllegalStateException::new);
            FieldUtils.write(objectIdWriter, new JSOGRefSerializer(), serializerField);
        }
    }
}