package com.fool.maven.plugin.data.formatter.tag;

import com.fool.maven.plugin.data.formatter.TagGetter;
import com.fool.maven.plugin.util.Composite;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class CompositeTagGetter implements TagGetter, Composite {

    private final TagGetter[] tagGetters;

    public CompositeTagGetter() {
        tagGetters = this.instances(this.getClass().getPackageName(),TagGetter.class);
    }

    @Override
    public List<String> classTags(Class<?> clazz, String javaFilePath) {
        for (TagGetter tagGetter : this.tagGetters) {
            List<String> result = tagGetter.classTags(clazz, javaFilePath);
            if (result != null) {
                return result;
            }
        }
        return new ArrayList<>();
    }

    @Override
    public List<String> methodTags(Class<?> clazz, String javaFilePath, Method method) {
        for (TagGetter tagGetter : this.tagGetters) {
            List<String> result = tagGetter.methodTags(clazz, javaFilePath, method);
            if (result != null) {
                return result;
            }
        }
        return new ArrayList<>();
    }

    @Override
    public String methodDescription(Class<?> clazz, String javaFilePath, Method method) {
        for (TagGetter tagGetter : this.tagGetters) {
            String result = tagGetter.methodDescription(clazz, javaFilePath, method);
            if (result != null) {
                return result;
            }
        }
        return "";
    }


    @Override
    public int order() {
        return 0;
    }
}
