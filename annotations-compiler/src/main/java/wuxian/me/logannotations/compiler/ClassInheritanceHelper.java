package wuxian.me.logannotations.compiler;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.processing.Messager;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;

/**
 * Created by wuxian on 22/11/2016.
 * A tool used to get class inheritance information.
 * we can't get a @Element's super class,so we have to read java file to deal it.
 */

public class ClassInheritanceHelper {
    private static final String DOT = ".";
    private static ClassInheritanceHelper helper;

    public static ClassInheritanceHelper getInstance(@NonNull Messager messager
            , @NonNull Elements elements) throws ProcessingException {
        if (helper == null) {
            helper = new ClassInheritanceHelper(messager, elements);
        }
        return helper;
    }

    private Elements elements;

    private Messager messager;
    private Map<String, String> classHeritanceMap = new HashMap<>();

    private ClassInheritanceHelper(@NonNull Messager messager, @NonNull Elements elements) throws ProcessingException {
        this.elements = elements;
        this.messager = messager;

        AndroidDirHelper.initMessager(messager);
    }

    /**
     * currently NOT SUPPORT inner class,anonymous class... and currently only support one application module,not support library
     * this function may return null,because
     * 1 not support class listed above
     * 2 the class has no super class
     *
     * @param classNameString your.package.name.classname
     */
    @Nullable
    public String getSuperClass(@NonNull String classNameString) {
        //check if is an class element
        TypeElement classElement = elements.getTypeElement(classNameString);
        if (null == classElement) {
            //throw new ProcessingException(null, String.format("can't find class for name %s", classNameString));
            return null;
        }

        String superClass = classHeritanceMap.get(classNameString);
        if (superClass != null) {
            return superClass;
        }

        File file = null;
        try {
            file = AndroidDirHelper.getFileByClassName(classNameString);
        } catch (ProcessingException e) {
            ;
        }

        if (null == file) {  //we can't find a valid file
            return null;
        }

        String superClassName = getSuperClassFrom(file, classNameString);
        if (null != superClassName) {
            classHeritanceMap.put(classNameString, superClassName);
        }

        return superClassName;
    }

    @Nullable
    private String getSuperClassFrom(@NonNull File file, @NonNull String classNameString) {
        int dot = classNameString.lastIndexOf(DOT);
        if (dot == -1) {
            return null;
        }
        String packageName = classNameString.substring(0, dot);
        String className = classNameString.substring(dot + 1, classNameString.length());

        //class A extends B implements C {
        Pattern pattern = Pattern.compile(String.format("class\\s+%s\\s+extends\\s+[\\w]+.*\\{", className));

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
            //LogAnnotationsProcessor.error(messager, null, String.format("reading settings.gradle IOException"));
            return null;
        } catch (Exception e) {
            //LogAnnotationsProcessor.error(messager, null, String.format("exception"));
            return null;
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (Exception e) {
                    //LogAnnotationsProcessor.error(messager, null, String.format("close settings.gradle IOException"));
                    return null;
                }

            }
        }
        if (!find) {
            return null;  //fail to find super class
        }

        //extends your-super-class{
        Pattern pattern1 = Pattern.compile(String.format("(?<=extends)[\\s][\\w]+(?=[{\\s])"));
        Matcher matcher = pattern1.matcher(builder.toString());
        if (!matcher.find()) {
            return null;
        }

        String superClass = matcher.group().trim();

        //import your-super-class-package.superclass;
        Pattern pattern2 = Pattern.compile(String.format("import.+%s\\s*;", superClass));
        Matcher matcher1 = pattern2.matcher(builder.toString());

        String superPackage = packageName; //默认在该包下
        if (matcher1.find()) { //存在import 说明在其它package下
            Pattern pattern3 = Pattern.compile(String.format("(?<=import)\\s+[\\s.\\w]+(?=.%s\\s*;)", superClass));
            Matcher matcher2 = pattern3.matcher(matcher1.group());
            if (matcher2.find()) {
                superPackage = matcher2.group().trim();
            }
        }

        return superPackage + DOT + superClass;  //your-super-class-package.superclass;
    }
}
