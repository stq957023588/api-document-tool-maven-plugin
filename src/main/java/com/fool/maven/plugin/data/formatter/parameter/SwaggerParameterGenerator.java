package com.fool.maven.plugin.data.formatter.parameter;

import com.fool.maven.plugin.data.formatter.ParameterGenerator;
import com.fool.maven.plugin.util.CommonUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

public class SwaggerParameterGenerator extends ParameterGenerator {



    @Override
    public Map<String, String> generateFieldsDescription(Class<?> clazz) {
        ClassLoader classLoader = clazz.getClassLoader();
        try {
            Class<? extends Annotation> apiModelPropertyAnnotationClass = (Class<? extends Annotation>) classLoader.loadClass("io.swagger.annotations.ApiModelProperty");
            HashMap<String, String> result = new HashMap<>();
            Field[] declaredFields = clazz.getDeclaredFields();
            for (Field field : declaredFields) {
                Annotation annotation = field.getAnnotation(apiModelPropertyAnnotationClass);
                if (annotation == null) {
                    continue;
                }
                String value = CommonUtils.annotationMethodInvoke(annotation, "value", String.class);
                result.put(field.getName(), value);
            }
            return result;
        } catch (ClassNotFoundException ignore) {

        }
        return null;
    }

    @Override
    public int order() {
        return 1;
    }
}
