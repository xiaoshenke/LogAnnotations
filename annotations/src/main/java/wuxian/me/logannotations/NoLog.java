package wuxian.me.logannotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by wuxian on 22/11/2016.
 * <p>
 * 用于注解class 被@NoLog注解的类下面的@Log无效
 */

@Retention(RetentionPolicy.CLASS) //编译期
@Target({ElementType.TYPE})
public @interface NoLog {
    boolean inheritated() default true;
}
