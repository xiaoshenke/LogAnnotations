package wuxian.me.logannotations.compiler;

import org.checkerframework.checker.nullness.qual.NonNull;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
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
 *
 * TODO:NoLog,LogAll annotation的处理
 *
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

    private static final String POST_FIX = " ,powed by LOG Annotation";

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

    /**
     * 被@NoLog注解 清空该文件下所有log
     */
    @Override
    public IWriter clearAllLog() {
        Pattern pattern = Pattern.compile(String.format("\\s*Log[.,\\s\"\\w\\(]+%s\"\\);", POST_FIX));
        Matcher matcher = pattern.matcher(lastNormal);
        lastNormal = matcher.replaceAll("");
        return this;
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

    private String getFindMethodRegex(@NonNull ExecutableElement element) {
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
        return regex;
    }

    private boolean hasLogAllready(@NonNull String content) {
        int superPos = getSuperFuncPosition(content);
        if (superPos != -1) {
            content = content.substring(superPos, content.length());
        }

        Pattern pattern = Pattern.compile(String.format("\\s*Log[.,\\s\"\\w\\(]+%s", POST_FIX)); //powed by annotation作为标记
        Matcher matcher = pattern.matcher(content);
        if (matcher.find() && matcher.start() == 0) {
            return true;
        }

        return false;
    }

    private String getLogSentence(@NonNull AnnotatedMethod method) {
        String log = "";
        if (method.getLevel() == LOG.LEVEL_INFO) {
            log = log + "Log.i(";
        } else if (method.getLevel() == LOG.LEVEL_DEBUG) {
            log = log + "Log.d(";
        } else {
            log = log + "Log.e(";
        }

        //add --> Log.e(classname,func name);
        log = log + "\"" + className + "\"" + "," + "\"in func " + method.getExecutableElement().getSimpleName().toString() + POST_FIX + "\");";
        return log;
    }

    /**
     * 有些函数 比如说onCreate() 必须调用一下super.onCreate();然后才能添加log 否则报错
     *
     * @return -1：没有super 否则返回super.xxx();的;的+1位置
     */
    private int getSuperFuncPosition(String content) {
        Pattern pattern = Pattern.compile("\\s*super\\.[,_\\w\\(\\)\\s]+;"); //存在调用super函数的 插入到super函数的后面
        Matcher matcher = pattern.matcher(content);

        if (matcher.find() && matcher.start() == 0) { //存在调用super 且不是其它函数的super
            return matcher.end();
        }
        return -1;
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
        Pattern pattern = Pattern.compile(getFindMethodRegex(element));
        Matcher matcher = pattern.matcher(lastNormal);

        if (matcher.find()) { //找到element对应的method
            String before = lastNormal.substring(0, matcher.end());
            String after = lastNormal.substring(matcher.end(), lastNormal.length());

            if (hasLogAllready(after)) {
                return this; //don't need to change state
            }

            Pattern pattern2 = Pattern.compile("\\s*(?=[\\w\\s;}])"); //regex --> 找出第一行的回车键 tab建
            Matcher matcher2 = pattern2.matcher(after);
            if (!matcher2.find() || matcher2.start() != 0) {
                state = STATE_WRITING_ERROR;
                return this;
            }

            String add = matcher2.group();
            add = add + getLogSentence(method);//add --> Log.e(classname,func name);

            int end = getSuperFuncPosition(after);
            if (end != -1) {
                before = before + after.substring(0, end);  //重置before after
                after = after.substring(end, after.length());
            }
            lastNormal = before + add + after;
        } else {
            state = STATE_WRITING_ERROR; //有一个函数没有被匹配
        }
        return this;
    }


    private boolean saveFile(String content) {
        FileOutputStream fop = null;
        try {
            fop = new FileOutputStream(file);
            byte[] bytes = lastNormal.getBytes();
            fop.write(bytes);
            fop.flush();

        } catch (FileNotFoundException e) {
            return false;
        } catch (IOException e) {
            return false;
        } finally {
            try {
                fop.close();
            } catch (IOException e) {
                return false;
            }

        }
        return true;
    }

    @Override
    public boolean save() {
        if (state == STATE_ERROR) {
            return false;
        }

        if (!saveFile(lastNormal)) {
            return saveFile(origin);
        }

        return true;
    }
}
