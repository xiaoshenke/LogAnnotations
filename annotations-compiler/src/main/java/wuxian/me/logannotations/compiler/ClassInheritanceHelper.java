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

    private Map<String, String> classHeritanceMap = new HashMap<>();

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
    private String getSuperClass(@NonNull String classNameString) throws ProcessingException {

        //check it is an class element
        TypeElement classElement = elements.getTypeElement(classNameString);
        if (null == classElement) {
            throw new ProcessingException(null, String.format("can't find class for name %s", classNameString));
        }

        String superClass = classHeritanceMap.get(classNameString);
        if (superClass != null) {
            return superClass;
        }

        //读取project路径下的.java文件
        String packageName = getPackageFromString(classNameString);
        String className = getClassFromString(classNameString);

        if (packageName.length() == 0 || className.length() == 0) {
            throw new ProcessingException(null, String.format("invalid class string", classNameString));
        }

        File file = getValidFile(packageName, className);
        if (null == file) {  //we can't find a valid file
            return null;
        }

        //TODO read file to get super class... 同个包下是无需import的 否则会有一个import 包的过程。

        return classNameString;
    }
}
