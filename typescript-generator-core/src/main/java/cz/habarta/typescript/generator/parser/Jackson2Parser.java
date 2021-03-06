
package cz.habarta.typescript.generator.parser;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.ser.*;
import cz.habarta.typescript.generator.*;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.*;


public class Jackson2Parser extends ModelParser {

    private final ObjectMapper objectMapper;

    public Jackson2Parser(Settings settings, TypeProcessor typeProcessor) {
        this(settings, typeProcessor, new ObjectMapper());
    }

    public Jackson2Parser(Settings settings, TypeProcessor typeProcessor, ObjectMapper objectMapper) {
        super(settings, typeProcessor);
        this.objectMapper = objectMapper;
    }

    @Override
    protected BeanModel parseBean(SourceType<Class<?>> sourceClass) {
        final List<PropertyModel> properties = new ArrayList<>();

        final JsonTypeInfo jsonTypeInfo = sourceClass.type.getAnnotation(JsonTypeInfo.class);
        if (jsonTypeInfo != null && jsonTypeInfo.include() == JsonTypeInfo.As.PROPERTY) {
            properties.add(new PropertyModel(jsonTypeInfo.property(), String.class, true, null));
        }

        final BeanHelper beanHelper = getBeanHelper(sourceClass.type);
        if (beanHelper != null) {
            for (BeanPropertyWriter beanPropertyWriter : beanHelper.getProperties()) {
                if (!isParentProperty(beanPropertyWriter.getName(), sourceClass.type)) {
                    Type propertyType = beanPropertyWriter.getGenericPropertyType();
                    if (propertyType == JsonNode.class) {
                        propertyType = Object.class;
                    }
                    boolean optional = false;
                    for (Class<? extends Annotation> optionalAnnotation : settings.optionalAnnotations) {
                        if (beanPropertyWriter.getAnnotation(optionalAnnotation) != null) {
                            optional = true;
                            break;
                        }
                    }
                    properties.add(processTypeAndCreateProperty(beanPropertyWriter.getName(), propertyType, optional, sourceClass.type));
                }
            }
        }

        final JsonSubTypes jsonSubTypes = sourceClass.type.getAnnotation(JsonSubTypes.class);
        if (jsonSubTypes != null) {
            for (JsonSubTypes.Type type : jsonSubTypes.value()) {
                addBeanToQueue(new SourceType<>(type.value(), sourceClass.type, "<subClass>"));
            }
        }
        final Class<?> superclass = sourceClass.type.getSuperclass() == Object.class ? null : sourceClass.type.getSuperclass();
        if (superclass != null) {
            addBeanToQueue(new SourceType<>(superclass, sourceClass.type, "<superClass>"));
        }
        return new BeanModel(sourceClass.type, superclass, properties);
    }

    private boolean isParentProperty(String property, Class<?> cls) {
        if (cls.getSuperclass() == Object.class) {
            return false;
        } else {
            final BeanHelper beanHelper = getBeanHelper(cls.getSuperclass());
            if (beanHelper != null) {
                for (BeanPropertyWriter beanPropertyWriter : beanHelper.getProperties()) {
                    if (beanPropertyWriter.getName().equals(property)) {
                        return true;
                    }
                }
            }
            return false;
        }
    }

    private BeanHelper getBeanHelper(Class<?> beanClass) {
        if (beanClass == null) {
            return null;
        }
        try {
            final DefaultSerializerProvider.Impl serializerProvider1 = (DefaultSerializerProvider.Impl) objectMapper.getSerializerProvider();
            final DefaultSerializerProvider.Impl serializerProvider2 = serializerProvider1.createInstance(objectMapper.getSerializationConfig(), objectMapper.getSerializerFactory());
            final JavaType simpleType = objectMapper.constructType(beanClass);
            final JsonSerializer<?> jsonSerializer = BeanSerializerFactory.instance.createSerializer(serializerProvider2, simpleType);
            if (jsonSerializer == null) {
                return null;
            }
            if (jsonSerializer instanceof BeanSerializer) {
                return new BeanHelper((BeanSerializer) jsonSerializer);
            } else {
                final String jsonSerializerName = jsonSerializer.getClass().getName();
                System.out.println(String.format("Warning: Unknown serializer '%s' for class '%s'", jsonSerializerName, beanClass));
                return null;
            }
        } catch (JsonMappingException e) {
            throw new RuntimeException(e);
        }
    }

    private static class BeanHelper extends BeanSerializer {
        private static final long serialVersionUID = 1;

        public BeanHelper(BeanSerializer src) {
            super(src);
        }

        public BeanPropertyWriter[] getProperties() {
            return _props;
        }

    }

}
