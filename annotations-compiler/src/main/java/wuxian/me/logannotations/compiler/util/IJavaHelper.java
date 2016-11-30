package wuxian.me.logannotations.compiler.util;

import org.checkerframework.checker.nullness.qual.NonNull;

import java.io.File;

/**
 * Created by wuxian on 30/11/2016.
 */

public interface IJavaHelper {

    String getLongClassName(String classInfo);

    String getLongSuperClass(String classInfo);

    boolean hasAllreadyImport(String className, String content);

    String readClassInfo(File file);
}
