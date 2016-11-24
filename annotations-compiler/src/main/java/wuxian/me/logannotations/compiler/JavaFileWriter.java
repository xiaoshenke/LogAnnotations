package wuxian.me.logannotations.compiler;

import org.checkerframework.checker.nullness.qual.NonNull;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.processing.Messager;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;

import wuxian.me.logannotations.LOG;

/**
 * Created by wuxian on 23/11/2016.
 * 打开前和save后都为NO_STATE
 * 正常写为WRITING_NORMAL
 * 写某个方法出错时为WRITTING_ERROR WRITTING_ERROR的那个方法不写入 进行回滚
 * 其它错误为ERROR ERROR时最后写文件不写 而写入原来的文件
 */

public class JavaFileWriter implements IWriter {

    private static final int STATE_NO_STATE = 0;
    private static final int STATE_WRITING_NORMAL = 1;
    private static final int STATE_WRITING_ERROR = 2;
    private static final int STATE_ERROR = -1;

    private File file;
    private String classNameString;
    private String className;

    private String origin;
    private String lastNormal;

    private int state = STATE_NO_STATE;
    private static Messager messager;

    public static void initMessager(@NonNull Messager msg) {
        if (messager != null) {
            return;
        }
        messager = msg;
    }

    public JavaFileWriter() {
        ;
    }

    @Override
    public IWriter open(@NonNull String classNameString) {
        File file;
        try {
            file = AndroidDirHelper.getFileByClassName(classNameString);
        } catch (ProcessingException e) {
            state = STATE_ERROR;
            return this;
        }
        this.file = file;
        this.classNameString = classNameString;
        int dot = classNameString.lastIndexOf(".");
        this.className = classNameString.substring(dot + 1, classNameString.length());

        StringBuilder builder = new StringBuilder("");
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(
                    new FileInputStream(file.getAbsolutePath())));
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line + "\n"); //手动加上\n
            }
            state = STATE_WRITING_NORMAL;
        } catch (FileNotFoundException e) {
            state = STATE_ERROR;
            LogAnnotationsProcessor.error(messager, null, String.format("settings.gradle not find"));
        } catch (IOException e) {
            state = STATE_ERROR;
            LogAnnotationsProcessor.error(messager, null, String.format("reading settings.gradle IOException"));
        } catch (Exception e) {
            state = STATE_ERROR;
            LogAnnotationsProcessor.error(messager, null, String.format("exception"));
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (Exception e) {
                    LogAnnotationsProcessor.error(messager, null, String.format("close settings.gradle IOException"));
                }
            }
        }

        if (state == STATE_WRITING_NORMAL) {
            origin = builder.toString();
            lastNormal = new String(origin);
        }

        return this;
    }

    @Override
    public IWriter addImportIfneed() {
        if (state == STATE_ERROR) {
            return this;
        }
        Pattern pattern = Pattern.compile("import\\s+android\\.util\\.Log;");
        Matcher matcher = pattern.matcher(origin);

        if (!matcher.find()) { //没有import log,手动导入
            String regex = String.format("package[\\.\\s\\w]*;");
            Pattern pattern1 = Pattern.compile(regex);
            Matcher matcher1 = pattern1.matcher(lastNormal);

            if (!matcher1.find()) {
                state = STATE_ERROR;
                return this;
            }

            int end = matcher1.end();//插入到package的后面
            String before = lastNormal.substring(0, end);
            String after = lastNormal.substring(end, lastNormal.length());

            lastNormal = before + "\n\nimport android.util.Log;" + after;
        }

        return this;
    }

    private String getSimpleName(@NonNull String origin) {
        int dot = origin.lastIndexOf(".");
        if (dot == -1) {
            return origin;
        }

        return origin.substring(dot + 1, origin.length());
    }

    //void main(A a,B b)
    @Override
    public IWriter writeLogToMethod(AnnotatedMethod method) {
        if (state == STATE_ERROR) {
            return this;
        }

        if (method.getLevel() == LOG.LEVEL_NO_LOG) {
            return this;
        }

        ExecutableElement element = method.getExecutableElement();

        String name = element.getSimpleName().toString();
        String returnname = element.getReturnType().toString();

        String regex = returnname + "\\s+" + name + "\\s*\\(\\s*";

        List<? extends VariableElement> parameters = element.getParameters();
        for (int i = 0; i < parameters.size(); i++) {
            if (i == parameters.size() - 1) {
                regex = regex + getSimpleName(parameters.get(i).asType().toString()) + "\\s+[\\w]+\\s*";
            } else {
                regex = regex + getSimpleName(parameters.get(i).asType().toString()) + "\\s+[\\w]+\\s*,\\s*";
            }
        }
        regex = regex + "\\)\\s*\\{"; //regex --> void main(){

        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(lastNormal);

        if (matcher.find()) {
            String before = lastNormal.substring(0, matcher.end());
            String after = lastNormal.substring(matcher.end(), lastNormal.length());

            //regex --> super.call(abcd);
            Pattern pattern1 = Pattern.compile("\\s*super\\.[,_\\w\\(\\)\\s]+;"); //存在调用super函数的 插入到super函数的后面
            Matcher matcher1 = pattern1.matcher(after);

            boolean hasSuper = false;
            if (matcher1.find() && matcher1.start() == 0) { //存在调用super 且不是其它函数的super
                hasSuper = true;
            }

            Pattern pattern2 = Pattern.compile("\\s*(?=[\\w\\s;}])"); //regex --> 找出第一行的回车键 tab建
            Matcher matcher2 = pattern2.matcher(after);
            if (!matcher2.find() || matcher2.start() != 0) {
                state = STATE_WRITING_ERROR;
                return this;
            }

            String add = matcher2.group();
            if (method.getLevel() == LOG.LEVEL_INFO) {
                add = add + "Log.i(";
            } else if (method.getLevel() == LOG.LEVEL_DEBUG) {
                add = add + "Log.d(";
            } else {
                add = add + "Log.e(";
            }

            add = add + className + "," + "\"in func " + name + "\");";//add --> Log.e(classname,func name);

            if (hasSuper) {
                before = before + after.substring(0, matcher1.end());  //重置before after
                after = after.substring(matcher1.end(), after.length());
            }
            lastNormal = before + add + after;
        } else {
            state = STATE_WRITING_ERROR; //有一个函数没有被匹配
        }

        return this;
    }

    /**
     * TODO
     */
    @Override
    public boolean save() {
        if (state == STATE_ERROR) {
            return false;
        }

        return true;
    }
}
