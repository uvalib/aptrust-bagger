package edu.virginia.lib.aptrust.bags.metadata.annotations;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class AnnotationUtils {

    /**
     * Gets all values returned by appropriately annotated member variables
     * or methods on the given object.
     */
    public static List<String> getAnnotationValues(Class annotationClass, Object object) {
        try {
            for (Method m : object.getClass().getMethods()) {
                for (Annotation a : m.getAnnotations()) {
                    if (a.annotationType().equals(annotationClass)) {
                        if (m.getParameterTypes().length != 0) {
                            throw new RuntimeException("Methods annotated with " + annotationClass
                                    + " may not have parameters!");
                        }
                        if (m.getReturnType().equals(String[].class)) {
                            String[] values = (String[]) m.invoke(object);
                            if (values == null) {
                                return Collections.emptyList();
                            } else {
                                return Arrays.asList(values);
                            }
                        } else if (m.getReturnType().equals(String.class)) {
                            return Collections.singletonList((String) m.invoke(object));
                        } else {
                            throw new RuntimeException(m + " must not be annotated with " + annotationClass
                                    + " since it does not return a String or String[]");
                        }
                    }
                }
            }
            for (Field f : object.getClass().getFields()) {
                for (Annotation a : f.getAnnotations()) {
                    if (a.annotationType().equals(annotationClass)) {
                        if (f.getType().equals(String[].class)) {
                            String[] values = (String[]) f.get(object);
                            if (values == null) {
                                return Collections.emptyList();
                            } else {
                                return Arrays.asList(values);
                            }
                        } else if (f.getType().equals(String.class)) {
                            return Collections.singletonList((String) f.get(object));
                        } else {
                            throw new RuntimeException(f + " must not be annotated with " + annotationClass
                                    + " since it is not a String or String[]");
                        }
                    }
                }
            }
            throw new RuntimeException("No " + annotationClass.getName() + " annotation found for class " + object.getClass() + "!");
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Gets the first value returned by appropriately annotated member
     * variables on the given object.
     */
    public static String getAnnotationValue(Class annotationClass, Object o) {
        List<String> values = getAnnotationValues(annotationClass, o);
        if (values.isEmpty()) {
            return null;
        } else {
            return values.get(0);
        }
    }
}
