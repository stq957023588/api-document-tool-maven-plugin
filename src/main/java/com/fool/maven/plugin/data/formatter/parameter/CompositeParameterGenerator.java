package com.fool.maven.plugin.data.formatter.parameter;

import com.fool.maven.plugin.data.formatter.ParameterGenerator;
import com.fool.maven.plugin.util.Composite;

import java.util.Map;

public class CompositeParameterGenerator extends ParameterGenerator implements Composite {

    private final ParameterGenerator[] parameterGenerators;

    public CompositeParameterGenerator() {
        parameterGenerators = this.instances(this.getClass().getPackageName(), ParameterGenerator.class);
    }

    @Override
    public Map<String, String> generateFieldsDescription(Class<?> clazz) {
        for (ParameterGenerator parameterGenerator : parameterGenerators) {
            Map<String, String> fieldsDescription = parameterGenerator.generateFieldsDescription(clazz);
            if (fieldsDescription != null) {
                return fieldsDescription;
            }
        }

        return null;
    }

    @Override
    public int order() {
        return 0;
    }
}
