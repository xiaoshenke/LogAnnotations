package wuxian.me.logannotations.compiler.util;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.processing.Messager;

import wuxian.me.littleparser.LittleParser;
import wuxian.me.littleparser.Visitor;
import wuxian.me.littleparser.astnode.ASTNode;
import wuxian.me.littleparser.astnode.ClassDeclareNode;
import wuxian.me.logannotations.compiler.LogAnnotationsProcessor;

/**
 * Created by wuxian on 25/11/2016.
 * <p>
 * 读文件 用于获取class A extends B implements C等关系
 */

public class JavaFileHelper implements IJavaHelper {

    private static Messager messager;

    public static void setMessager(@NonNull Messager msg) {
        messager = msg;
    }

    private static JavaFileHelper helper = null;

    public static JavaFileHelper getInstance() {
        if (helper == null) {
            helper = new JavaFileHelper();
        }

        return helper;
    }

    private JavaFileHelper() {
        ;
    }

    @Nullable
    private String getShortSuperClass(@NonNull String info) {
        //class A(<? (extends X & IY &IZ)?,N>)? extends B(<O,P&Q>)? (implement C,D)?{
        Pattern pattern;//= Pattern.compile(String.format("(?<=extends)\\s+[_\\w]+(?=[\\{\\s])"));
        pattern = Pattern.compile("class[,.<>_&\\w\\s]+\\{");
        Matcher matcher = pattern.matcher(info);
        if (matcher.find()) {
            LittleParser parser = new LittleParser();
            boolean success = parser.matchClassString(matcher.group());
            if (success) {
                Visitor visitor = new Visitor();
                ASTNode classNode = visitor.visitFirstNode(parser.getParsedASTNode(), ASTNode.NODE_CLASS_DECLARATION);
                if (classNode != null && classNode instanceof ClassDeclareNode) {
                    LogAnnotationsProcessor.info(messager, null, String.format("find super name: %s", ((ClassDeclareNode) classNode).getSuperClassName()));
                    return ((ClassDeclareNode) classNode).getSuperClassName();
                }
            }
        }
        return null;
    }

    @Nullable
    private String getShortClassName(@NonNull String info) {
        //LogAnnotationsProcessor.info(messager,null,String.format("getShortclass %s",info));
        Pattern pattern;//= Pattern.compile("(?<=class)\\s+[_\\w]+(?=[\\{\\s])");
        pattern = Pattern.compile("class[,.<>_&\\w\\s]+\\{");
        Matcher matcher = pattern.matcher(info);
        if (matcher.find()) {
            LittleParser parser = new LittleParser();
            boolean success = parser.matchClassString(matcher.group());
            if (success) {
                Visitor visitor = new Visitor();
                ASTNode classNode = visitor.visitFirstNode(parser.getParsedASTNode(), ASTNode.NODE_CLASS_DECLARATION);
                if (classNode != null && classNode instanceof ClassDeclareNode) {
                    LogAnnotationsProcessor.info(messager, null, String.format("find name: %s", ((ClassDeclareNode) classNode).getClassName()));
                    return ((ClassDeclareNode) classNode).getClassName();
                }

            }
        }
        return null;
    }

    @Override
    @Nullable
    public String getLongClassName(@NonNull String classInfo) {
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
    @Override
    public String getLongSuperClass(String classInfo) {
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
            Pattern pattern3 = Pattern.compile(String.format("(?<=import)\\s+[\\s.\\w]+(?=.%s\\s*;)", className));
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
    @Override
    public boolean hasAllreadyImport(String className, String content) {
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
     * @return string "package xxxx; import xxxxx;class A extends B implements C{"
     * 因为在获取package时需要前面的package,import信息 因此保留前面的内容
     */
    @Nullable
    @Override
    public String readClassInfo(@NonNull File file) {
        Pattern pattern;//= Pattern.compile(String.format("class\\s+[_\\w]+\\s+extends\\s+[_\\w\\s]+\\{"));

        pattern = Pattern.compile("class[,.<>_&\\w\\s]+\\{");
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

        return builder.toString();
    }

    /**
     * A helper func
     * usage: findMatchCharactor('{','}',your-string) it will find matched @right charactor
     * if it meets @left,it should find num(right+1) pos
     *
     * @first should not contain @left...
     */
    private int findMatchCharactor(char left, char right, @NonNull String content, int first) {
        if (content.length() == 0) {
            return -1;
        }

        int rightFind = 1;
        for (int i = first; i < content.length(); i++) {
            char c = content.charAt(i);
            if (c == left) {
                rightFind += 1;
            } else if (c == right) {
                rightFind--;
                if (rightFind == 0) {
                    return i;
                }
            }
        }
        return -1;
    }
}
