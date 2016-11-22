package wuxian.me.logannotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by wuxian on 22/11/2016.
 * <p>
 * level: -1-->no log,0-->info,1-->debug,2-->error
 */

@Retention(RetentionPolicy.CLASS) //编译期
@Target(ElementType.METHOD)
public @interface Log {
    int level() default -1;
}
