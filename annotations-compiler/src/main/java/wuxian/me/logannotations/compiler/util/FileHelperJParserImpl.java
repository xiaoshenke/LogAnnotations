package wuxian.me.logannotations.compiler.util;

import java.io.File;

/**
 * Created by wuxian on 1/12/2016.
 * <p>
 * using @japa.parser.JavaParser to implement IJavaHelper
 */

public class FileHelperJParserImpl implements IJavaHelper {
    @Override
    public String getLongClassName(String classInfo) {
        return null;
    }

    @Override
    public String getLongSuperClass(String classInfo) {
        return null;
    }

    @Override
    public boolean hasAllreadyImport(String className, String content) {
        return false;
    }

    @Override
    public String readClassInfo(File file) {
        return null;
    }
}
