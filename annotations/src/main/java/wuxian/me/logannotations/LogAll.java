package wuxian.me.logannotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by wuxian on 24/11/2016.
 * 具有自动继承性质 就是说一个父类具有@LogAll 那么继承这个类的子类也自然拥有这个属性
 */

@Retention(RetentionPolicy.CLASS) //编译期
@Target({ElementType.TYPE})
public @interface LogAll {
    int level() default 0;

    boolean inheritated() default true;
}
