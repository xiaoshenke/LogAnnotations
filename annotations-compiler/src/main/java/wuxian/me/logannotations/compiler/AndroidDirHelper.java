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
import java.util.List;
import java.util.NoSuchElementException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.processing.Messager;

/**
 * Created by wuxian on 23/11/2016.
 *
 * A helper class.
 */
public class AndroidDirHelper {
    private static final String SETTINGS_GRADLE = "settings.gradle";
    private static final String BUILD_GRADLE = "build.gradle";
    private static final String DOT = ".";

    private static Messager messager = null;

    //一个project只有一个root dir
    private static File rootDir = null;

    //但是一个project可能有多个module 也就是有多个module dir
    private static List<String> modules = null;

    private AndroidDirHelper() {
        throw new NoSuchElementException("no instance");
    }

    public static void initMessager(@NonNull Messager msg) {
        if (messager != null) {
            return;
        }
        messager = msg;
    }

    /**
     * 这里不做class名字校验 并且这里的classname长这样 your.package.name.classname
     */
    public static File getFileByClassName(String classNameString) throws ProcessingException {
        int dot = classNameString.lastIndexOf(DOT);
        if (dot == -1) {
            return null;
        }

        String packageName = classNameString.substring(0, dot);
        String className = classNameString.substring(dot + 1, classNameString.length());
        String packageDir = findJavaDir(packageName).getAbsolutePath();
        File file = new File(packageDir + "/" + transformNametoPath(packageName) + "/" + className + ".java");
        if (file.exists()) {
            return file;
        }
        return null;
    }

    public static File getJavaRoot(String classNameString) throws ProcessingException {
        int dot = classNameString.lastIndexOf(DOT);
        if (dot == -1) {
            return null;
        }

        String packageName = classNameString.substring(0, dot);
        String className = classNameString.substring(dot + 1, classNameString.length());
        return findJavaDir(packageName);
    }

    private static boolean initRootDir() throws ProcessingException {
        if (null != rootDir) {
            return true;
        }
        rootDir = findRootDirectory();
        return true;
    }

    private static File findJavaDir(String packageName) throws ProcessingException {
        if (packageName == null || packageName.length() == 0) {
            throw new ProcessingException(null, "package name is null");
        }
        if (rootDir == null) {
            initRootDir();
        }
        if (null == modules) {
            initModules();
        }

        Pattern p = Pattern.compile("\\.");
        Matcher m = p.matcher(packageName);
        String packagePath = m.replaceAll("/");

        for (String module : modules) {
            if (new File((rootDir + "/" + module + "/src/main/java/" + packagePath)).exists()) {
                return new File(rootDir + "/" + module + "/src/main/java/");
            }
        }

        throw new ProcessingException(null, String.format("no module find for package:%s ", packageName));
    }

    private static String transformNametoPath(@NonNull String packageName) {
        return packageName.replace(".", "/");
    }

    /**
     * 读取android gradle工程的settings.gradle文件来获取所有android module
     * @return
     */
    private static boolean initModules() {
        modules = new ArrayList<>();

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
        Pattern r = Pattern.compile(pattern);
        Matcher m = r.matcher(content);
        while (m.find()) {
            modules.add(m.group());
        }
        return true;
    }

    @Nullable
    private static File findRootDirectory() throws ProcessingException {
        File root = new File(new File(".").getAbsolutePath()).getParentFile();

        if (root != null && root.isDirectory() && isRootDirectory(root)) {
            return root;
        }
        throw new ProcessingException(null, "can't find root dir");
    }

    /**
     * Currently only support Gradle project! not maven!
     */
    private static boolean isRootDirectory(File dir) {
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
}
