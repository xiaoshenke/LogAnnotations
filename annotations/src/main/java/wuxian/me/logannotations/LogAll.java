package wuxian.me.logannotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by wuxian on 24/11/2016.
 */

@Retention(RetentionPolicy.CLASS) //编译期
@Target({ElementType.TYPE})
public @interface LogAll {
    int level() default 0;
}
