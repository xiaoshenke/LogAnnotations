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
    private static final String SETTINGS_GRADLE = "settings.gradle";
    private static final String BUILD_GRADLE = "build.gradle";
    private static ClassInheritanceHelper helper;

    public static ClassInheritanceHelper getInstance(@NonNull Messager messager
            , @NonNull Elements elements, @NonNull PackageElement packageElement) throws ProcessingException {
        if (helper == null) {
            helper = new ClassInheritanceHelper(messager, elements, packageElement);
        }
        return helper;
    }

    private Elements elements;
    private PackageElement packageElement;
    private String mPackageName;
    private File mProjectRoot;
    private File mJavaRootDirectory;
    private Messager messager;
    private Map<String, String> classHeritanceMap = new HashMap<>();

    private ClassInheritanceHelper(@NonNull Messager messager, @NonNull Elements elements, @NonNull PackageElement packageElement) throws ProcessingException {
        this.elements = elements;
        this.packageElement = packageElement;
        this.messager = messager;

        File cur = new File(new File(".").getAbsolutePath());
        File root = findRootDirectory(cur.getParentFile());
        mProjectRoot = root;
        mJavaRootDirectory = getJavaRootFile(root, packageElement.toString());

        if (mProjectRoot == null || mJavaRootDirectory == null) {
            throw new ProcessingException(null, String.format("we can't find a valid root directory!"));
        }
    }

    @Nullable
    private File getJavaRootFile(File rootDir, String packageName) {
        if (rootDir == null) {
            return null;
        }
        StringBuilder builder = new StringBuilder("");

        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(
                    new FileInputStream(rootDir.getAbsolutePath() + "/" + SETTINGS_GRADLE)));
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
        } catch (FileNotFoundException e) {
            LogAnnotationsProcessor.error(messager, null, String.format("settings.gradle not find"));
        } catch (IOException e) {
            LogAnnotationsProcessor.error(messager, null, String.format("reading settings.gradle IOException"));
        } catch (Exception e) {
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

        String content = builder.toString();
        String pattern = "(?<=['\"]:)[-\\w]+(?=['\"])";
        List<String> modules = new ArrayList<>();
        Pattern r = Pattern.compile(pattern);
        Matcher m = r.matcher(content);
        while (m.find()) {
            modules.add(m.group());
        }

        return findValidJavaRoot(modules, packageName);
    }

    @Nullable
    private File findValidJavaRoot(List<String> modules, String packageName) {
        if (mProjectRoot == null || modules == null || modules.size() == 0 || packageName == null || packageName.length() == 0) {
            return null;
        }
        Pattern p = Pattern.compile("\\.");
        Matcher m = p.matcher(packageName);
        String packagePath = m.replaceAll("/");

        for (String module : modules) {
            if (new File((mProjectRoot + "/" + module + "/src/main/java/" + packagePath)).exists()) {
                return new File(mProjectRoot + "/" + module + "/src/main/java/");
            }
        }

        return null;
    }

    @Nullable
    private File findRootDirectory(File fromFile) {
        if (fromFile != null && fromFile.isDirectory() && isRootDirectory(fromFile)) {
            return fromFile;
        }
        return null;
    }

    /**
     * Currently only support Gradle project! not maven!
     */
    private boolean isRootDirectory(File dir) {
        boolean settingFileExsit = false;
        boolean buildFileExsit = false;
        File[] files;
        files = dir.listFiles();
        for (int i = 0; i < files.length; i++) {
            File file = files[i];
            if (file.getName().equals(SETTINGS_GRADLE)) {
                settingFileExsit = true;
            } else if (file.getName().equals(BUILD_GRADLE)) {
                buildFileExsit = true;
            }
            if (settingFileExsit && buildFileExsit) {
                break;
            }
        }
        return (settingFileExsit && buildFileExsit);
    }

    @NonNull
    private String getPackageFromString(@NonNull String classNameString) {
        int dot = classNameString.lastIndexOf(DOT);
        if (dot == -1) {
            return "";
        }
        return classNameString.substring(0, dot);
    }

    @NonNull
    private String getClassFromString(@NonNull String classNameString) {
        int dot = classNameString.lastIndexOf(DOT);
        if (dot == -1) {
            return "";
        }
        return classNameString.substring(dot + 1, classNameString.length());
    }

    private String getPackageNameFromPath(@NonNull String packagePath) {
        return packagePath.replace("/", ".");
    }

    private String getPackagePathFromName(@NonNull String packageName) {
        return packageName.replace(".", "/");
    }

    @Nullable
    private File getValidFile(@NonNull String packageName, @NonNull String className) {
        File file = new File(mJavaRootDirectory + "/" + getPackagePathFromName(packageName) + "/" + className + ".java");
        if (file.exists()) {
            return file;
        }
        return null;
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

        //读取project路径下的.java文件
        String packageName = getPackageFromString(classNameString);
        String className = getClassFromString(classNameString);

        if (packageName.length() == 0 || className.length() == 0) {
            //throw new ProcessingException(null, String.format("invalid class string", classNameString));
            return null;
        }

        File file = getValidFile(packageName, className);
        if (null == file) {  //we can't find a valid file
            return null;
        }

        String superClassName = getSuperClassFrom(file, packageName, className);
        if (null != superClassName) {
            classHeritanceMap.put(classNameString, superClassName);
        }

        return superClassName;
    }

    @Nullable
    private String getSuperClassFrom(@NonNull File file, @NonNull String packageName, @NonNull String className) {
        //class A extends B implements C {
        Pattern pattern = Pattern.compile(String.format("class\\s+%s\\s+extends\\s+[\\w]+.*{", className));

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
        Pattern pattern1 = Pattern.compile(String.format("(?<=extends[\\s]+)%s(?=[{\\s])", className));
        Matcher matcher = pattern1.matcher(builder.toString());
        if (!matcher.find()) {
            return null;
        }

        String superClass = matcher.group();

        //import your-super-class-package.superclass;
        Pattern pattern2 = Pattern.compile(String.format("import.+%s\\s*;", superClass));
        Matcher matcher1 = pattern2.matcher(builder.toString());

        String superPackage = packageName; //默认在该包下
        if (matcher1.find()) { //存在import 说明在其它package下
            Pattern pattern3 = Pattern.compile(String.format("(?<=import\\s+).+(?=\\.%s\\s*;)", superClass));
            Matcher matcher2 = pattern3.matcher(matcher1.group());
            if (matcher2.find()) {
                superPackage = matcher2.group();
            }
        }

        return superPackage + DOT + superClass;  //your-super-class-package.superclass;
    }
}
