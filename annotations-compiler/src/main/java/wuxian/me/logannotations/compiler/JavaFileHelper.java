package wuxian.me.logannotations.compiler;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.NoSuchElementException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by wuxian on 25/11/2016.
 * <p>
 * 读文件 用于获取class A extends B implements C等关系
 */

public class JavaFileHelper {
    private JavaFileHelper() {
        throw new NoSuchElementException("no instance");
    }

    @Nullable
    private static String getShortSuperClass(@NonNull String info) {
        Pattern pattern = Pattern.compile(String.format("(?<=extends)\\s+[_\\w]+(?=[\\{\\s])"));
        Matcher matcher = pattern.matcher(info);
        if (matcher.find()) {
            return matcher.group().trim();
        }
        return null;
    }

    @Nullable
    private static String getShortClassName(@NonNull String info) {
        Pattern pattern = Pattern.compile("(?<=class)\\s+[_\\w]+(?=[\\{\\s])");
        Matcher matcher = pattern.matcher(info);
        if (matcher.find()) {
            return matcher.group().trim();
        }
        return null;
    }

    @Nullable
    public static String getLongClassName(@NonNull String classInfo) {
        String className = getShortClassName(classInfo);
        if (className == null) {
            return null;
        }

        String regex = String.format("(?<=package)\\s+[\\.\\w]+\\s*(?=;)");  //找到package name
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(classInfo);

        if (!matcher.find()) {
            return null;
        }

        String packageName = matcher.group().trim();
        return packageName + "." + className;
    }

    @Nullable
    public static String getLongSuperClass(String classInfo) {
        String className = getShortSuperClass(classInfo);
        if (className == null) {
            return null;
        }
        String regex = String.format("(?<=package)\\s+[\\.\\w]+\\s*(?=;)");  //找到package name
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(classInfo);
        if (!matcher.find()) {
            return null;
        }

        String packageName = matcher.group().trim();
        String superPackage = packageName; //默认在该包下

        if (hasAllreadyImport(className, classInfo)) { //存在import 说明在其它package下
            Pattern pattern3 = Pattern.compile(String.format("(?<=import)\\s+[\\s.\\w]+(?=.%s\\s*;)", classInfo));
            Matcher matcher2 = pattern3.matcher(classInfo);
            if (matcher2.find()) {
                superPackage = matcher2.group().trim();
            }
        }
        return superPackage + "." + className;
    }

    /**
     * 是否导入了class
     */
    public static boolean hasAllreadyImport(String className, String content) {
        //import your-super-class-package.superclass;
        Pattern pattern = Pattern.compile(String.format("import.+%s\\s*;", className));
        Matcher matcher = pattern.matcher(content);
        if (matcher.find()) {
            return true;
        }
        return false;
    }

    /**
     * Currently NOT SUPPORT inner class
     *
     * @return string "package xxxx; import xxxxx;class A extends B implements C{"
     * 因为在获取package时需要前面的package,import信息 因此保留前面的内容
     */
    @Nullable
    public static String readClassInfo(@NonNull File file) {
        Pattern pattern = Pattern.compile(String.format("class\\s+[_\\w]\\s+extends\\s+[\\w]+.*\\{"));

        StringBuilder builder = new StringBuilder("");
        BufferedReader reader = null;
        int lines = 0;
        boolean find = false;
        try {
            reader = new BufferedReader(new InputStreamReader(
                    new FileInputStream(file.getAbsolutePath())));
            String line;
            while ((line = reader.readLine()) != null) {
                lines++;
                builder.append(line);
                if (lines % 3 == 1) {  //每3行试图匹配一下
                    Matcher m = pattern.matcher(builder.toString());
                    if (m.find()) {
                        find = true;
                        break;
                    }
                }
            }
            if (!find) {
                Matcher m = pattern.matcher(builder.toString());
                if (m.find()) {
                    find = true;
                }
            }
        } catch (FileNotFoundException e) {
            //LogAnnotationsProcessor.error(messager, null, String.format("settings.gradle not find"));
            return null;
        } catch (IOException e) {
            return null;
        } catch (Exception e) {
            return null;
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (Exception e) {
                    return null;
                }

            }
        }
        if (!find) {
            return null;  //fail to find super class
        }

        Matcher classInfoMatcher = pattern.matcher(builder.toString());

        int end = classInfoMatcher.end();
        String sub = builder.toString().substring(0, end);
        Pattern bracketPattern = Pattern.compile("\\{");
        Matcher bracketMatcher = bracketPattern.matcher(sub);
        if (bracketMatcher.find()) {
            return null;  //目前不支持inner class
        }

        return builder.toString();
    }
}
