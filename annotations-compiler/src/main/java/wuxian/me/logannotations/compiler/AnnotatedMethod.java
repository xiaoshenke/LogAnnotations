package wuxian.me.logannotations.compiler;

import org.checkerframework.checker.nullness.qual.NonNull;

import java.lang.annotation.Annotation;

import javax.lang.model.element.ExecutableElement;

import wuxian.me.logannotations.LOG;

/**
 * Created by wuxian on 22/11/2016.
 */

public class AnnotatedMethod {
    private ExecutableElement element;
    private int level = -1;

    public AnnotatedMethod(@NonNull ExecutableElement methodElement,
                           @NonNull Class<? extends Annotation> annotationClass) throws IllegalArgumentException {
        Annotation annotation = methodElement.getAnnotation(annotationClass);
        this.element = methodElement;
        this.level = ((LOG) annotation).level();
    }

    @NonNull
    public ExecutableElement getExecutableElement() {
        return element;
    }

    @NonNull
    public int getLevel() {
        return level;
    }
}
