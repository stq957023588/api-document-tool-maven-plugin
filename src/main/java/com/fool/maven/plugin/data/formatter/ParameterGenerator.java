package com.fool.maven.plugin.data.formatter;

import com.fool.maven.plugin.ApiParameter;
import com.fool.maven.plugin.Context;
import com.fool.maven.plugin.util.CommonUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public abstract class ParameterGenerator {

    protected Class<? extends Annotation> pathVariableAnnClass = null;

    protected Class<? extends Annotation> requestBodyAnnClass = null;

    protected Class<?>[] nonparametricClass = null;

    protected Class<?>[] sameAsJdkClass = null;

    public List<ApiParameter> generate(Method method) {
        try {
            pathVariableAnnClass = (Class<? extends Annotation>) Context.getMavenProjectClassLoader().loadClass("org.springframework.web.bind.annotation.PathVariable");
            requestBodyAnnClass = (Class<? extends Annotation>) Context.getMavenProjectClassLoader().loadClass("org.springframework.web.bind.annotation.RequestBody");
            nonparametricClass = new Class<?>[]{
                    Context.getMavenProjectClassLoader().loadClass("javax.servlet.http.HttpServletResponse"),
                    Context.getMavenProjectClassLoader().loadClass("javax.servlet.http.HttpServletRequest"),
            };
            sameAsJdkClass = new Class<?>[]{
                    Context.getMavenProjectClassLoader().loadClass("org.springframework.web.multipart.MultipartFile"),
            };

        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }


        Parameter[] parameters = method.getParameters();
        Class<?>[] parameterTypes = method.getParameterTypes();
        int parameterCount = method.getParameterCount();

        List<ApiParameter> apiParameters = new ArrayList<>();

        for (int i = 0; i < parameterCount; i++) {
            if (isNonparametricClass(parameterTypes[i])) {
                continue;
            }

            if (isSameAsJdkClass(parameterTypes[i])) {
                apiParameters.add(new ApiParameter(parameters[i].getName(), parameterTypes[i], null, "FORM"));
                continue;
            }

            if (parameters[i].getAnnotation(pathVariableAnnClass) != null) {
                apiParameters.add(new ApiParameter(parameters[i].getName(), parameterTypes[i], null, "PATH"));
                continue;
            }
            String position = parameters[i].getAnnotation(requestBodyAnnClass) == null ? "QUERY" : "BODY";
            if (parameterTypes[i].getClassLoader() == null) {
                apiParameters.add(new ApiParameter(parameters[i].getName(), parameterTypes[i], null, position));
                continue;
            }
            List<ApiParameter> unwindClassParameter = generateParameterFromClass(parameterTypes[i], position);
            apiParameters.addAll(unwindClassParameter);
        }

        return apiParameters;
    }

    protected boolean isNonparametricClass(Class<?> clazz) {
        for (Class<?> c : nonparametricClass) {
            if (c.isAssignableFrom(clazz)) {
                return true;
            }
        }
        return false;
    }

    protected boolean isSameAsJdkClass(Class<?> clazz) {
        return CommonUtils.search(sameAsJdkClass, clazz) != -1;
    }


    public List<ApiParameter> generateParameterFromClass(Class<?> clazz, String position) {
        return Context.isUnwindClassField() ? generateParameterFromClassDeep(clazz, position, null) : generateParameterFromClassShallow(clazz, position);
    }

    public List<ApiParameter> generateParameterFromClassShallow(Class<?> clazz, String position) {
        List<ApiParameter> parameters = new ArrayList<>();
        Map<String, String> fieldsDescription = generateFieldsDescription(clazz);

        List<Field> allFields = CommonUtils.getAllFields(clazz);
        for (Field field : allFields) {
            boolean isStatic = Modifier.isStatic(field.getModifiers());
            if (isStatic) {
                continue;
            }
            if (field.getType().getClassLoader() != null) {
                Context.getEntityClassNames().add(field.getType().getName());
            }

            String[] genericTypeNames = CommonUtils.filedGenericTypes(field);
            parameters.add(new ApiParameter(field.getName(), field.getType(), genericTypeNames, fieldsDescription == null ? null : fieldsDescription.get(field.getName()), position));
        }
        return parameters;

    }


    public List<ApiParameter> generateParameterFromClassDeep(Class<?> clazz, String position, String prefix) {
        if (prefix == null) {
            prefix = "";
        }

        List<ApiParameter> parameters = new ArrayList<>();

        Map<String, String> fieldsDescription = generateFieldsDescription(clazz);


        List<Field> allFields = CommonUtils.getAllFields(clazz);
        for (Field field : allFields) {
            boolean isStatic = Modifier.isStatic(field.getModifiers());
            if (isStatic) {
                continue;
            }
            ClassLoader classLoader = field.getType().getClassLoader();
            String key = prefix + field.getName();
            if (classLoader == null) {
                String[] genericTypeNames = CommonUtils.filedGenericTypes(field);
                parameters.add(new ApiParameter(key, field.getType(), genericTypeNames, fieldsDescription == null ? null : fieldsDescription.get(field.getName()), position));
            } else {
                List<ApiParameter> subParameters = generateParameterFromClassDeep(field.getType(), position, key + ".");
                parameters.addAll(subParameters);
            }
        }
        return parameters;
    }


    public abstract Map<String, String> generateFieldsDescription(Class<?> clazz);

    public abstract int order();
}
