package com.fool.maven.plugin.data.formatter;

import java.lang.reflect.Method;
import java.util.List;

public interface TagGetter {

    List<String> classTags(Class<?> clazz, String javaFilePath);

    List<String> methodTags(Class<?> clazz, String javaFilePath, Method  method );

    String methodDescription(Class<?> clazz, String javaFilePath, Method method );

    int order();

}
