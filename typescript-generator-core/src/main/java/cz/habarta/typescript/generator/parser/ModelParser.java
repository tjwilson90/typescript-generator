
package cz.habarta.typescript.generator.parser;

import cz.habarta.typescript.generator.*;
import java.lang.reflect.Type;
import java.util.*;


public abstract class ModelParser {

    protected final Settings settings;
    protected final TypeProcessor typeProcessor;
    private final Javadoc javadoc;
    private final Queue<SourceType<? extends Type>> typeQueue = new LinkedList<>();

    public ModelParser(Settings settings, TypeProcessor typeProcessor) {
        this.settings = settings;
        this.typeProcessor = typeProcessor;
        this.javadoc = new Javadoc(settings.javadocXmlFiles);
    }

    public Model parseModel(Type type) {
        return parseModel(Arrays.asList(new SourceType<>(type)));
    }

    public Model parseModel(List<SourceType<Type>> types) {
        typeQueue.addAll(types);
        final Model model = parseQueue();
        final Model modelWithJavadoc = javadoc.enrichModel(model);
        return modelWithJavadoc;
    }

    private Model parseQueue() {
        final Set<Class<?>> parsedClasses = new LinkedHashSet<>();
        final List<BeanModel> beans = new ArrayList<>();
        final List<EnumModel> enums = new ArrayList<>();
        SourceType<?> sourceType;
        while ((sourceType = typeQueue.poll()) != null) {
            final TypeProcessor.Result result = processType(sourceType.type);
            if (result != null) {
                if (sourceType.type instanceof Class<?> && (
                        result.getTsType() instanceof TsType.StructuralType || result.getTsType() instanceof TsType.EnumType)) {
                    final Class<?> cls = (Class<?>) sourceType.type;
                    if (!parsedClasses.contains(cls)) {
                        System.out.println("Parsing '" + cls.getName() + "'" +
                                (sourceType.usedInClass != null ? " used in '" + sourceType.usedInClass.getSimpleName() + "." + sourceType.usedInMember + "'" : ""));
                        if (result.getTsType() instanceof TsType.StructuralType) {
                            final BeanModel bean = parseBean(sourceType.asSourceClass());
                            beans.add(bean);
                        }
                        if (result.getTsType() instanceof TsType.EnumType) {
                            final EnumModel enumModel = parseEnum(sourceType.asSourceClass());
                            enums.add(enumModel);
                        }
                        parsedClasses.add(cls);
                    }
                } else {
                    for (Class<?> cls : result.getDiscoveredClasses()) {
                        typeQueue.add(new SourceType<>(cls, sourceType.usedInClass, sourceType.usedInMember));
                    }
                }
            }
        }
        return new Model(beans, enums);
    }

    protected abstract BeanModel parseBean(SourceType<Class<?>> sourceClass);

    protected EnumModel parseEnum(SourceType<Class<?>> sourceClass) {
        final List<String> values = new ArrayList<>();
        if (sourceClass.type.isEnum()) {
            @SuppressWarnings("unchecked")
            final Class<? extends Enum<?>> enumClass = (Class<? extends Enum<?>>) sourceClass.type;
            for (Enum<?> enumConstant : enumClass.getEnumConstants()) {
                values.add(enumConstant.name());
            }
        }
        return new EnumModel(sourceClass.type, values, null);
    }

    protected void addBeanToQueue(SourceType<? extends Class<?>> sourceClass) {
        typeQueue.add(sourceClass);
    }

    protected PropertyModel processTypeAndCreateProperty(String name, Type type, boolean optional, Class<?> usedInClass) {
        List<Class<?>> classes = discoverClassesUsedInType(type);
        for (Class<?> cls : classes) {
            typeQueue.add(new SourceType<>(cls, usedInClass, name));
        }
        return new PropertyModel(name, type, optional, null);
    }

    private List<Class<?>> discoverClassesUsedInType(Type type) {
        final TypeProcessor.Result result = processType(type);
        return result != null ? result.getDiscoveredClasses() : Collections.<Class<?>>emptyList();
    }

    private TypeProcessor.Result processType(Type type) {
        return typeProcessor.processType(type, new TypeProcessor.Context() {
            @Override
            public String getMappedName(Class<?> cls) {
                return "NA";
            }
            @Override
            public TypeProcessor.Result processType(Type javaType) {
                return typeProcessor.processType(javaType, this);
            }
        });
    }

}
