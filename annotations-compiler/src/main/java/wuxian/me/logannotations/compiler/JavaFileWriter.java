package wuxian.me.logannotations.compiler;

import org.checkerframework.checker.nullness.qual.NonNull;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.processing.Messager;

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

    private String origin;
    private String lastNormal;
    private String current;

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

        StringBuilder builder = new StringBuilder("");
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(
                    new FileInputStream(file.getAbsolutePath())));
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
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
            current = new String(lastNormal);
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

        int dot = classNameString.lastIndexOf(".");
        String className = classNameString.substring(dot + 1, classNameString.length());

        if (!matcher.find()) { //没有import log,手动导入
            Pattern pattern1 = Pattern.compile(String.format("package.*\\.%s;", className));
            Matcher matcher1 = pattern1.matcher(lastNormal);

            if (!matcher1.find()) {
                state = STATE_ERROR;
                return this;
            }

            int end = matcher1.end();//插入...
            String before = lastNormal.substring(end);
            String after = lastNormal.substring(end + 1, lastNormal.length());

            lastNormal = before + "\nimport android.util.Log;" + after;
            current = new String(lastNormal); //...
        }

        return this;
    }

    //TODO: to be finished
    @Override
    public IWriter writeLogToMethod(AnnotatedMethod method) {
        if (state == STATE_ERROR) {
            return this;
        }
        return this;
    }

    @Override
    public boolean save() {
        if (state == STATE_ERROR) {
            return false;
        }

        return true;
    }
}
