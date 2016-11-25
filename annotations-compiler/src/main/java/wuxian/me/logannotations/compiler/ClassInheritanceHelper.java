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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.processing.Messager;
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

    private boolean dumpOnce = false;

    //key:class value:super-class
    private Map<String, String> superClassMap = new HashMap<>();

    private ClassInheritanceHelper(@NonNull Messager messager, @NonNull Elements elements) throws ProcessingException {
        this.elements = elements;
        this.messager = messager;

        AndroidDirHelper.initMessager(messager);
    }

    private List<File> paths = new ArrayList<>();

    /**
     * 读取java root下的所有文件 记录继承关系到superClassMap中 --> ???效率?
     */
    public void dumpAllClassesOnce(File javaRoot) throws ProcessingException {
        if (dumpOnce) {
            return;
        }

        dumpOnce = true;

        if (!javaRoot.isDirectory()) {
            throw new ProcessingException(null, "java root is not directory!");
        }

        paths.clear();
        paths.add(javaRoot);

        int current = 0;
        while (current < paths.size()) {
            recursiveDealFile(paths.get(current));
            current++;
        }

        return;
    }

    private boolean checkDir(@NonNull File dir) {
        if (!dir.isDirectory()) {
            return false;
        }

        if (!dir.canRead()) {
            return false;
        }
        return true;
    }

    private boolean checkJavaFile(@NonNull File file) {
        if (file.isDirectory()) {
            return false;
        }
        if (!file.canRead()) {
            return false;
        }
        if (file.length() == 0) {
            return false;
        }

        return file.getName().endsWith(".java");
    }

    private void recursiveDealFile(@NonNull File dir) {
        if (!checkDir(dir)) {
            return;
        }

        File[] files;
        files = dir.listFiles();
        for (int i = 0; i < files.length; i++) {
            File file = files[i];
            if (file.isDirectory() && checkDir(file)) {
                paths.add(file);
                continue;
            }

            if (checkJavaFile(file)) {
                tryGetClassHeritance(file);
            }
        }
    }

    /**
     * 读取java文件 拿到继承关系
     *
     * @param file
     */
    private void tryGetClassHeritance(File file) {
        String classInfo = getClassByInfoString(file.getAbsolutePath());

        if (null == classInfo) {
            return;
        }

        String className = getClassByInfoString(classInfo);
        String superClass = getSuperClassByInfoString(classInfo);
        if (className == null || superClass == null) {
            return;
        }

        String wholeClass = getWholeClass(classInfo, className);  //xxx.xxx.xxx.class
        String wholeSuperClass = getWholeSuperClass(classInfo, superClass); //xxx.xxx.xxx.superclass

        if (wholeClass == null || wholeSuperClass == null) {
            return;
        }

        superClassMap.put(wholeClass, wholeSuperClass);
    }

    private String getWholeClass(@NonNull String classInfo, @NonNull String className) {
        String regex = String.format("(?<=package)\\s+[\\.\\w]+\\s*(?=;)");  //找到package name
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(classInfo);

        if (!matcher.find()) {
            return null;
        }

        String packageName = matcher.group().trim();
        return packageName + "." + className;
    }

    private String getWholeSuperClass(String classInfo, String className) {
        String regex = String.format("(?<=package)\\s+[\\.\\w]+\\s*(?=;)");  //找到package name
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(classInfo);
        if (!matcher.find()) {
            return null;
        }

        String packageName = matcher.group().trim();
        String superPackage = packageName; //默认在该包下

        if (hasImportClass(className, classInfo)) { //存在import 说明在其它package下
            Pattern pattern3 = Pattern.compile(String.format("(?<=import)\\s+[\\s.\\w]+(?=.%s\\s*;)", classInfo));
            Matcher matcher2 = pattern3.matcher(classInfo);
            if (matcher2.find()) {
                superPackage = matcher2.group().trim();
            }
        }
        return superPackage + "." + className;
    }

    /**
     * 只返回上一层的sub-class A-->B-->C A不是C的sub-class
     * 1 先加入superClassMap里的类
     * 2 遍历java root下的所有file 跳过已经存入到list中的对应路径class
     */
    public List<String> getSubClasses(@NonNull String classNameString) {
        List<String> subClasses = new ArrayList<>();

        Set<String> set = superClassMap.keySet();
        Iterator<String> iterator = set.iterator();
        while (iterator.hasNext()) {
            if (superClassMap.get(iterator.next()).equals(classNameString)) {
                subClasses.add(iterator.next());
            }
        }

        return subClasses;
    }

    @Nullable
    private String getClassByInfoString(@NonNull String info) {
        Pattern pattern = Pattern.compile("(?<=class)\\s+[_\\w]+(?=[\\{\\s])");
        Matcher matcher = pattern.matcher(info);
        if (matcher.find()) {
            return matcher.group().trim();
        }
        return null;
    }

    @Nullable
    private String getSuperClassByInfoString(@NonNull String info) {
        Pattern pattern = Pattern.compile(String.format("(?<=extends)\\s+[_\\w]+(?=[\\{\\s])"));
        Matcher matcher = pattern.matcher(info);
        if (matcher.find()) {
            return matcher.group().trim();
        }
        return null;
    }

    /**
     * Currently NOT SUPPORT inner class
     *
     * @return package xxxx; import xxxxx;class A extends B implements C{
     * 因为在获取package时需要前面的package,import信息 因此保留
     */
    private String getClassInfoFromFile(@NonNull File file) {

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

    /**
     * 返回所有的sub-class A-->B-->C A是C的sub-class
     */
    public List<String> getAllLevelSubClasses(@NonNull String classNameString) {
        List<String> allLevel = getSubClasses(classNameString);
        int current = 0;
        while (current < allLevel.size()) {
            List<String> single = getSubClasses(allLevel.get(current));
            current++;
            if (single.size() == 0) {
                continue;
            }

            allLevel.removeAll(single);
            allLevel.addAll(single);
        }

        return allLevel;
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

        return superClassMap.get(classNameString);

        /*
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
            superClassMap.put(classNameString, superClassName);
        }

        return superClassName;
        */
    }

    /**
     * 是否导入了class
     *
     * @param className
     * @param content
     * @return
     */
    private boolean hasImportClass(String className, String content) {
        //import your-super-class-package.superclass;
        Pattern pattern = Pattern.compile(String.format("import.+%s\\s*;", className));
        Matcher matcher = pattern.matcher(content);
        if (matcher.find()) {
            return true;
        }
        return false;
    }

    @Nullable
    private String getSuperClassFrom(@NonNull File file, @NonNull String classNameString) {
        int dot = classNameString.lastIndexOf(DOT);
        if (dot == -1) {
            return null;
        }
        String packageName = classNameString.substring(0, dot);
        String className = classNameString.substring(dot + 1, classNameString.length());

        String classInfo = getClassInfoFromFile(file);
        if (classInfo == null) {
            return null;
        }

        String name = getClassByInfoString(classInfo);
        if (name == null || !name.equals(className)) { //比较两者是否相等
            return null;
        }

        String superClass = getSuperClassByInfoString(classInfo);
        if (superClass == null) {
            return null;
        }

        String superPackage = packageName; //默认在该包下
        if (hasImportClass(superClass, classInfo)) { //存在import 说明在其它package下
            Pattern pattern3 = Pattern.compile(String.format("(?<=import)\\s+[\\s.\\w]+(?=.%s\\s*;)", superClass));
            Matcher matcher2 = pattern3.matcher(classInfo);
            if (matcher2.find()) {
                superPackage = matcher2.group().trim();
            }
        }

        return superPackage + DOT + superClass;  //your-super-class-package.superclass;
    }
}
