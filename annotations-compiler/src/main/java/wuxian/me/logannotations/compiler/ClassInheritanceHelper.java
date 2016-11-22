package wuxian.me.logannotations.compiler;

import org.checkerframework.checker.nullness.qual.NonNull;

import java.io.File;
import java.util.List;

import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;

/**
 * Created by wuxian on 22/11/2016.
 * A tool used to get class inheritance information.
 * TODO: 找出父类的方法? we can't get a @Element's super class
 * 所以必须通过读取java file的方式...
 */

public class ClassInheritanceHelper {

    private static ClassInheritanceHelper helper;

    public static ClassInheritanceHelper getInstance(Elements elements) {
        if (helper == null) {
            helper = new ClassInheritanceHelper(elements);
        }
        return helper;
    }

    private Elements elements;
    private String mPackageName;
    private File mRootDirectory;

    private ClassInheritanceHelper(@NonNull Elements elements) {
    }

    /**
     * TODO
     */
    private String getSuperClass(@NonNull String className) throws ProcessingException {

        //check it is an class element
        TypeElement classElement = elements.getTypeElement(className);
        if (null == classElement) {
            throw new ProcessingException(null, String.format("can't find class for name %s", className));
        }

        //读取project路径下的.java文件

        return className;
    }

    /**
     * TODO
     */
    private List<String> getSuperClasses(@NonNull String className) throws ProcessingException {
        return null;
    }
}
