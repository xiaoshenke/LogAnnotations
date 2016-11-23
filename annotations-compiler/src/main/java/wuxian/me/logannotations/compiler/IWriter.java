package wuxian.me.logannotations.compiler;

import java.io.File;

import javax.lang.model.element.ExecutableElement;

/**
 * Created by wuxian on 23/11/2016.
 */

public interface IWriter {

    IWriter open(String classNameString);

    IWriter addImportIfneed();

    IWriter writeLogToMethod(AnnotatedMethod method);

    boolean save();
}