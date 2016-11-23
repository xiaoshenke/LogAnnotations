package wuxian.me.logannotations.compiler;

import java.io.File;

import javax.lang.model.element.ExecutableElement;

/**
 * Created by wuxian on 23/11/2016.
 */

public class JavaFileWriter implements IWriter {

    public JavaFileWriter() {
        ;
    }

    @Override
    public IWriter open(File file) {
        return null;
    }

    @Override
    public IWriter addImportIfneed() {
        return null;
    }

    @Override
    public IWriter writeLogToMethod(ExecutableElement element) {
        return null;
    }

    @Override
    public boolean write() {
        return false;
    }
}
