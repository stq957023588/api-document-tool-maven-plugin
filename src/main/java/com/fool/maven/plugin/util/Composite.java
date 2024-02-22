package com.fool.maven.plugin.util;

import org.reflections.Reflections;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Set;

public interface Composite {

    default <T> T[] instances(String packageName, Class<T> clazz) {
        Reflections reflections = new Reflections(packageName);
        //获取继承了ISuperClass的所有类
        Set<Class<? extends T>> classSet = reflections.getSubTypesOf(clazz);
        T[] arr = (T[]) Array.newInstance(clazz, classSet.size() - 1);
        int index = 0;
        for (Class<? extends T> c : classSet) {
            if (c.equals(this.getClass())) {
                continue;
            }
            try {
                Constructor<? extends T> constructor = c.getDeclaredConstructor();
                constructor.setAccessible(true);
                arr[index++] = constructor.newInstance();
            } catch (InvocationTargetException | NoSuchMethodException | InstantiationException |
                     IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
        return arr;
    }

}
