package com.fool.maven.plugin.util;

import com.fool.maven.plugin.Context;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class CommonUtils {

    public static <T> int search(T[] arr, T value) {
        if (arr == null) {
            return -1;
        }
        for (int i = 0; i < arr.length; i++) {
            if (Objects.equals(arr[i], value)) {
                return i;
            }
        }
        return -1;
    }

    public static boolean isSubFromJdkClass(Class<?> clazz) {
        while (clazz != null && !clazz.equals(Object.class)) {
            if (clazz.getClassLoader() == null) {
                return true;
            }
            clazz = clazz.getSuperclass();
        }
        return false;
    }


    public static String[] filedGenericTypes(Field field) {
        Type genericType = field.getGenericType();
        return filedGenericTypes(genericType);
    }

    public static String[] filedGenericTypes(Type genericType) {
        String[] genericTypeNames = null;
        if (genericType instanceof ParameterizedType) {
            ParameterizedType type = (ParameterizedType) genericType;
            Type[] actualTypeArguments = type.getActualTypeArguments();
            genericTypeNames = new String[actualTypeArguments.length];
            for (int i = 0; i < actualTypeArguments.length; i++) {
                Context.getEntityClassNames().add(actualTypeArguments[i].getTypeName());
                genericTypeNames[i] = actualTypeArguments[i].getTypeName();
            }
        }
        return genericTypeNames;
    }

    public static List<Field> getAllFields(Class<?> clazz) {
        List<Field> fields = new ArrayList<>();
        while (clazz != null) {
            Field[] declaredFields = clazz.getDeclaredFields();
            fields.addAll(Arrays.asList(declaredFields));
            clazz = clazz.getSuperclass();
        }
        return fields;
    }


    public static String join(String[] arr) {
        if (arr == null || arr.length == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder(arr[0]);
        for (int i = 1; i < arr.length; i++) {
            sb.append(",").append(arr[i]);
        }
        return sb.toString();
    }


    public static String formatPath(String path) {
        path = path.startsWith("/") ? path : "/" + path;
        path = path.endsWith("/") ? path.substring(0, path.length() - 1) : path;
        return path;
    }


    public static <T> T annotationMethodInvoke(Annotation o, String methodName, Class<T> returnType) {
        if (o == null) {
            return null;
        }
        try {
            Method declaredMethod = o.getClass().getDeclaredMethod(methodName);
            Object invoke = declaredMethod.invoke(o);
            return returnType.cast(invoke);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

}
