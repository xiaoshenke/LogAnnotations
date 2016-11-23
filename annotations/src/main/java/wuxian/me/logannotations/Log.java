package wuxian.me.logannotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by wuxian on 22/11/2016.
 * <p>
 * level: -1-->no log,0-->info,1-->debug,2-->error
 *
 * 具有继承属性 父类被@LOG 注解的函数,子类中不要再注解一次@LOG,但是会忽略具有@NoLog注解的类里的所有Log函数
 */

@Retention(RetentionPolicy.CLASS) //编译期
@Target(ElementType.METHOD)
public @interface LOG {
    int level() default -1;
}
