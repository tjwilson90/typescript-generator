
package cz.habarta.typescript.generator;

import java.lang.reflect.Type;
import java.util.*;


public class ExcludingTypeProcessor implements TypeProcessor {

    private final Set<String> excludedClassNames = new LinkedHashSet<>();

    public ExcludingTypeProcessor(List<String> excludedClassNames) {
        if (excludedClassNames != null) {
            this.excludedClassNames.addAll(excludedClassNames);
        }
    }

    @Override
    public Result processType(Type javaType, Context context) {
        if (javaType instanceof Class<?>) {
            final Class<?> classType = (Class<?>) javaType;
            if (excludedClassNames.contains(classType.getName())) {
                return new Result(TsType.Any);
            }
        }
        return null;
    }
    
}
