package com.fool.maven.plugin.data.formatter.tag;

import com.fool.maven.plugin.data.formatter.TagGetter;
import com.fool.maven.plugin.util.CommonUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SwaggerClassTagGetter implements TagGetter {

    private final String apiAnnotationClassName = "io.swagger.annotations.Api";

    private final String apiOperationAnnotationClassName = "io.swagger.annotations.ApiOperation";


    SwaggerClassTagGetter() {

    }

    @Override
    public List<String> classTags(Class<?> clazz, String javaFilePath) {
        ClassLoader classLoader = clazz.getClassLoader();
        try {
            Class<? extends Annotation> apiAnnotationClass = (Class<? extends Annotation>) classLoader.loadClass(apiAnnotationClassName);
            Annotation annotation = clazz.getAnnotation(apiAnnotationClass);
            String[] tags = CommonUtils.annotationMethodInvoke(annotation, "tags", String[].class);
            if (tags == null || tags.length == 0) {
                return null;
            }
            ArrayList<String> result = new ArrayList<>();
            Collections.addAll(result, tags);
            return result;
        } catch (ClassNotFoundException e) {
            return null;
        }
    }


    @Override
    public List<String> methodTags(Class<?> clazz, String javaFilePath, Method method) {
        ClassLoader classLoader = clazz.getClassLoader();
        try {
            Class<? extends Annotation> apiOperationAnnotationClass = (Class<? extends Annotation>) classLoader.loadClass(apiOperationAnnotationClassName);
            Annotation annotation = method.getAnnotation(apiOperationAnnotationClass);
            if (annotation == null) {
                return null;
            }
            String[] tags = CommonUtils.annotationMethodInvoke(annotation, "tags", String[].class);
            if (tags.length == 0 || (tags.length == 1 && tags[0].isBlank())) {
                String value = CommonUtils.annotationMethodInvoke(annotation, "value", String.class);
                ArrayList<String> result = new ArrayList<>();
                result.add(value);
                return result;
            }
            ArrayList<String> result = new ArrayList<>();
            Collections.addAll(result, tags);
            return result;
        } catch (ClassNotFoundException e) {
            return null;
        }
    }


    @Override
    public String methodDescription(Class<?> clazz, String javaFilePath, Method method) {
        ClassLoader classLoader = clazz.getClassLoader();
        try {
            Class<? extends Annotation> apiOperationAnnotationClass = (Class<? extends Annotation>) classLoader.loadClass(apiOperationAnnotationClassName);
            Annotation annotation = method.getAnnotation(apiOperationAnnotationClass);
            return CommonUtils.annotationMethodInvoke(annotation, "value", String.class);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }


    @Override
    public int order() {
        return 0;
    }
}
