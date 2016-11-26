package wuxian.me.logannotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by wuxian on 22/11/2016.
 * <p>
 * Current NOT SUPPORT Interface ,Inner-class,anonymous-class(anntation can't be recognized in anonymous-class).
 * If you annotate an Interface method,you will get a compile error!
 * If you annotate an Inner-class method,it just be ignored.
 * So as @NoLog and @LogAll
 */

@Retention(RetentionPolicy.CLASS) //编译期
@Target(ElementType.METHOD)
public @interface LOG {
    int level() default 0;

    int LEVEL_NO_LOG = -1;
    int LEVEL_INFO = 0;
    int LEVEL_DEBUG = 1;
    int LEVEL_ERROR = 2;
}
