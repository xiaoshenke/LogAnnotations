package wuxian.me.logannotations.compiler.writer;


import wuxian.me.logannotations.compiler.AnnotatedMethod;

/**
 * Created by wuxian on 23/11/2016.
 */

public interface IWriter {

    IWriter clearAllLog();

    IWriter open(String classNameString);

    IWriter addImportIfneed();

    IWriter writeLogToMethod(AnnotatedMethod method);

    boolean save();
}
