package com.fool.maven.plugin;

import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Context {


    private static volatile boolean initialized = false;

    private static URLClassLoader mavenProjectClassLoader;

    private static Method findMergedAnnotationMethod;

    private static Map<String, String> classNameJavaFilePathMap;

    private static Set<String> entityClassNames;

    private static boolean unwindClassField;

    public static boolean isUnwindClassField() {
        return unwindClassField;
    }

    public static void setUnwindClassField(boolean unwindClassField) {
        Context.unwindClassField = unwindClassField;
    }

    public static Set<String> getEntityClassNames() {
        return entityClassNames;
    }

    public static Method getFindMergedAnnotationMethod() {
        return findMergedAnnotationMethod;
    }

    public static URLClassLoader getMavenProjectClassLoader() {
        return mavenProjectClassLoader;
    }

    public static Map<String, String> getClassNameJavaFilePathMap() {
        return classNameJavaFilePathMap;
    }

    public static void setClassNameJavaFilePathMap(Map<String, String> classNameJavaFilePathMap) {
        Context.classNameJavaFilePathMap = classNameJavaFilePathMap;
    }

    public static void initialize(MavenProject project) {
        if (initialized) {
            return;
        }
        initialized = true;


        List<?> compileClasspathElements;
        try {
            compileClasspathElements = project.getCompileClasspathElements();
        } catch (DependencyResolutionRequiredException e) {
            throw new RuntimeException("获取编译路径失败", e);
        }
        URL[] urls = new URL[compileClasspathElements.size()];
        for (int i = 0; i < compileClasspathElements.size(); i++) {
            try {
                urls[i] = new File((String) compileClasspathElements.get(i)).toURI().toURL();
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
        }

        mavenProjectClassLoader = new URLClassLoader(urls);

        try {
            Class<?> annotatedElementUtilsClass = mavenProjectClassLoader.loadClass("org.springframework.core.annotation.AnnotatedElementUtils");
            findMergedAnnotationMethod = annotatedElementUtilsClass.getDeclaredMethod("findMergedAnnotation", AnnotatedElement.class, Class.class);
        } catch (ClassNotFoundException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }

        entityClassNames = new HashSet<>();
    }

    public static Class<?> loadClass(String className) {
        try {
            return mavenProjectClassLoader.loadClass(className);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public static Class<?> loadClassNonException(String className) {
        try {
            return mavenProjectClassLoader.loadClass(className);
        } catch (ClassNotFoundException ignore) {
            return null;
        }
    }

}
