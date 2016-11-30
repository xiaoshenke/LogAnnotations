package wuxian.me.logannotations.compiler;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import wuxian.me.logannotations.LOG;
import wuxian.me.logannotations.LogAll;
import wuxian.me.logannotations.NoLog;
import wuxian.me.logannotations.compiler.util.AndroidDirHelper;
import wuxian.me.logannotations.compiler.util.ClassInheritanceHelper;
import wuxian.me.logannotations.compiler.util.JavaFileHelper;
import wuxian.me.logannotations.compiler.writer.LogWriter;

import java.io.File;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.Elements;
import javax.tools.Diagnostic;

/**
 * Created by wuxian on 22/11/2016.
 * <p>
 */

@SupportedAnnotationTypes(value = {"wuxian.me.logannotations.LOG"})
public class LogAnnotationsProcessor extends AbstractProcessor {
    @NonNull
    private Elements elementUtils;
    @NonNull
    private Filer filer;
    @NonNull
    private Messager messager;

    ClassInheritanceHelper helper;

    //存储所有class的methods,包括被NoLog注解的class
    private final Map<String, List<AnnotatedMethod>> mAllMethodsMap = new LinkedHashMap<>();

    //不含被NoLog注解的class
    private final @NonNull Map<String, List<AnnotatedMethod>> mGroupedMethodsMap = new LinkedHashMap<>();

    /**
     * 用于merge super class的method 由于存在这种情况 A-->B-->C B被@NoLog注解
     * 这种情况B中的method不会被记录到mGroupedMethodsMap,但会被记录到mAllMethodsMap
     */
    private final Set<String> mMergedSet = new HashSet<>();

    //被@NoLog注解的类
    private final List<TypeElement> mNoLogList = new ArrayList<>();

    //NoLog具有继承属性 因此清除log的时候 应对它的子类也进行log清除动作
    private final Set<TypeElement> mClearLogList = new HashSet<>();

    //被@LogAll注解的类
    private final List<TypeElement> mLogAllList = new ArrayList<>();

    @Override
    public synchronized void init(@NonNull ProcessingEnvironment env) {
        super.init(env);
        messager = processingEnv.getMessager();
        filer = processingEnv.getFiler();
        elementUtils = processingEnv.getElementUtils();
    }

    @Override
    public boolean process(@NonNull Set<? extends TypeElement> set,
                           @NonNull RoundEnvironment roundEnv) {
        JavaFileHelper.setMessager(messager);
        ClassInheritanceHelper.initMessager(messager);
        AndroidDirHelper.initMessager(messager);
        try {
            info(messager, null, "init helper");
            helper = ClassInheritanceHelper.getInstance(elementUtils);

            info(messager, null, "before process nolog");
            collectNoLogAnnotations(roundEnv);  //collect NoLog class

            info(messager, null, "before process logall");
            collectLogAllAnnotations(roundEnv); //collect LogAll class

            info(messager, null, "before collect annotations");
            collectLOGAnnotations(roundEnv);

            info(messager, null, "before dump class");
            if (!dumpClasses()) {
                info(messager, null, "no Annotations found!");
                return true;
            }

        } catch (ProcessingException e) {
            error(messager, e.getElement(), e.getMessage());
        }

        info(messager, null, "before get logall!");
        getAllLogAllClasses();

        info(messager, null, "before get nolog!");
        getAllNoLogClasses();

        info(messager, null, "before merge annotationclass");
        mergeLogAnnotations();

        info(messager, null, "before deal inheritance");
        mergeSuperClassAnnotations();

        info(messager, null, "before clear log");
        clearUselessLogs();

        info(messager, null, "before write log");
        writeLogs();

        mGroupedMethodsMap.clear();
        mLogAllList.clear();
        mNoLogList.clear();
        mClearLogList.clear();
        mAllMethodsMap.clear();
        mMergedSet.clear();

        return true;
    }

    /**
     * @NoLog 具有继承属性
     * 被@NoLog注解的类的子类里面的LogAll,LOG注解依然有效
     */
    private void getAllNoLogClasses() {
        int current = 0;
        int size = mNoLogList.size();
        while (current < size) {
            TypeElement element = mNoLogList.get(current);
            if (!element.getAnnotation(NoLog.class).inheritated()) {
                current++;
                continue;
            }

            List<String> sub = helper.getAllSubClasses(element.getQualifiedName().toString());
            for (String className : sub) {
                if (mNoLogList.contains(className)) {
                    continue;
                }
                TypeElement ele = elementUtils.getTypeElement(className);
                if (ele.getAnnotation(LogAll.class) != null) {  //被LogAll注解 跳过
                    continue;
                }

                mNoLogList.add(ele);
            }
            current++;
        }
    }

    /**
     * @LogAll 具有继承属性
     */
    private void getAllLogAllClasses() {
        int current = 0;
        int size = mLogAllList.size();
        while (current < size) {

            TypeElement element = mLogAllList.get(current);
            if (!element.getAnnotation(LogAll.class).inheritated()) { //LogAll不用继承
                current++;
                continue;
            }

            List<String> sub = helper.getAllSubClasses(element.getQualifiedName().toString());

            for (String className : sub) {
                if (mLogAllList.contains(className)) {
                    continue;
                }
                TypeElement ele = elementUtils.getTypeElement(className);
                if (ele.getAnnotation(NoLog.class) != null) {  //被NoLog注解 跳过
                    continue;
                }

                mLogAllList.add(ele);
            }
            current++;
        }
    }

    private boolean dumpClasses() throws ProcessingException {
        String className = null;

        if (mGroupedMethodsMap.size() != 0) {
            Iterator<String> iterator = mGroupedMethodsMap.keySet().iterator();
            className = iterator.next();

        } else if (mNoLogList.size() != 0) {
            Iterator<TypeElement> iterator = mNoLogList.iterator();
            className = iterator.next().getQualifiedName().toString();

        } else if (mLogAllList.size() != 0) {
            Iterator<TypeElement> iterator = mNoLogList.iterator();
            className = iterator.next().getQualifiedName().toString();
        } else {
            return false;
        }

        File javaRoot = AndroidDirHelper.getJavaModuleRoot(className);
        helper.dumpAllClasses(javaRoot);

        return true;
    }

    private int getLogAllLevel(TypeElement element) {
        if (element.getAnnotation(LogAll.class) != null) {
            return element.getAnnotation(LogAll.class).level();
        }

        String superClass = helper.getSuperClass(element.getQualifiedName().toString());
        if (superClass == null) {
            return LOG.LEVEL_NO_LOG;
        }

        TypeElement ele = elementUtils.getTypeElement(superClass);
        if (ele == null) {
            return LOG.LEVEL_NO_LOG;
        }

        return getLogAllLevel(ele);
    }

    /**
     * Merge @mGroupMethodsMap,@mLogAllList
     * 每个LogAll注解类的函数都相当于被LOG(LogAll.level)注解;原先的LOG注解依然有效
     * 因此将LogAll生成的注解merge到mGroupMethodsMap中
     */
    private void mergeLogAnnotations() {
        for (TypeElement element : mLogAllList) {
            String className = element.getQualifiedName().toString();

            List<? extends Element> elements = element.getEnclosedElements();

            List<AnnotatedMethod> annotatedMethods = new ArrayList<>();
            int level = getLogAllLevel(element); //如果是继承的 那么这里没有@LogAll注解

            for (Element ele : elements) { //收集element下的所有method
                if (ele.getKind() != ElementKind.METHOD) {
                    continue;
                }
                AnnotatedMethod annotatedMethod = new AnnotatedMethod((ExecutableElement) ele, level);
                if (annotatedMethods.contains(annotatedMethod)) {
                    continue;
                }
                annotatedMethods.add(annotatedMethod);
            }
            if (mGroupedMethodsMap.keySet() != null && mGroupedMethodsMap.keySet().contains(className)) {  //有则合并 否则加入
                mergeMethod(mGroupedMethodsMap.get(className), annotatedMethods);
            } else {
                mGroupedMethodsMap.put(className, annotatedMethods);
            }
        }
    }

    private void collectLogAllAnnotations(@NonNull RoundEnvironment roundEnv) throws ProcessingException {
        Set<? extends Element> elements = roundEnv.getElementsAnnotatedWith(LogAll.class);
        for (Element element : elements) {
            if (element.getKind() != ElementKind.CLASS) {
                throw new ProcessingException(element,
                        String.format("Only class can be annotated with @%s",
                                LogAll.class.getSimpleName()));
            } else {
                if (element.getAnnotation(NoLog.class) != null) {
                    throw new ProcessingException(element,
                            String.format("class can't be annotated with both @%s and @%s",
                                    NoLog.class.getSimpleName(), LogAll.class.getSimpleName()));
                }

                mLogAllList.add((TypeElement) element);
            }
        }
    }

    private void collectNoLogAnnotations(@NonNull RoundEnvironment roundEnv) throws ProcessingException {
        Set<? extends Element> elements = roundEnv.getElementsAnnotatedWith(NoLog.class);
        for (Element element : elements) {
            if (element.getKind() != ElementKind.CLASS) {
                throw new ProcessingException(element,
                        String.format("Only class can be annotated with @%s",
                                NoLog.class.getSimpleName()));
            } else {
                if (element.getAnnotation(LogAll.class) != null) {
                    throw new ProcessingException(element,
                            String.format("class can't be annotated with both @%s and @%s",
                                    NoLog.class.getSimpleName(), LogAll.class.getSimpleName()));
                }
                mNoLogList.add((TypeElement) element);
            }
        }

    }

    private void collectLOGAnnotations(@NonNull RoundEnvironment roundEnv) throws ProcessingException {
        Class<? extends Annotation> annotationClass = LOG.class;
        Set<? extends Element> elements = roundEnv.getElementsAnnotatedWith(annotationClass);

        for (Element element : elements) {
            if (element.getKind() != ElementKind.METHOD) {
                throw new ProcessingException(element,
                        String.format("Only methods can be annotated with @%s",
                                annotationClass.getSimpleName()));
            } else {
                ExecutableElement executableElement = (ExecutableElement) element;
                try {
                    processLOGMethod(executableElement);
                } catch (IllegalArgumentException e) {
                    throw new ProcessingException(executableElement, e.getMessage());
                }
            }
        }
    }

    /**
     * 子类自动继承所有父类的@LOG annotation
     */
    private void mergeSuperClassAnnotations() {
        Set<String> classNames = mGroupedMethodsMap.keySet();
        Iterator<String> iterator = classNames.iterator();

        while (iterator.hasNext()) {
            String className = iterator.next();
            if (!mMergedSet.contains(className)) { //该结点未处理 先去处理
                recursiveDealClass(className);
            }
            mGroupedMethodsMap.put(className, mAllMethodsMap.get(className));
        }
    }

    /**
     * 因为LogAll具有继承属性 所以NoLog也应该具有继承属性 必须清理子类中由于LogAll产生的log
     * must be called before @writeLogs
     */
    private void clearUselessLogs() {
        LogWriter.initMessager(messager);

        for (TypeElement element : mNoLogList) {
            String classNameString = element.getQualifiedName().toString();
            LogWriter writer = new LogWriter();
            writer.open(classNameString).clearAllLog().save();
        }
    }

    /**
     * 写文件
     */
    private void writeLogs() {
        for (String classNameString : mGroupedMethodsMap.keySet()) {
            LogWriter writer = new LogWriter();
            writer.open(classNameString).addImportIfneed();

            for (AnnotatedMethod method : mGroupedMethodsMap.get(classNameString)) {
                info(messager, null, String.format("write log element: %s", method.getExecutableElement().toString()));

                writer.writeLogToMethod(method);
            }
            writer.save();
        }
    }

    private void processLOGMethod(ExecutableElement executableElement) throws ProcessingException {
        TypeElement enclosingClass = findEnclosingClass(executableElement);

        if (enclosingClass == null) {
            throw new ProcessingException(null,
                    String.format("Can not find enclosing class for method %s",
                            executableElement.getSimpleName().toString()));
        } else {
            if (enclosingClass.getAnnotation(NoLog.class) != null) { //类被NoLog注解 不处理这个类里面的log
                return;
            }

            AnnotatedMethod annotatedMethod = new AnnotatedMethod(executableElement, LOG.class);
            String className = enclosingClass.getQualifiedName().toString();  //将该element存入一个class的map中
            List<AnnotatedMethod> groupedMethods = mGroupedMethodsMap.get(className);
            if (groupedMethods == null) {
                groupedMethods = new ArrayList<>();
                mGroupedMethodsMap.put(className, groupedMethods);
            }
            groupedMethods.add(annotatedMethod);
        }
    }

    private void recursiveDealClass(String className) {
        if (mGroupedMethodsMap.containsKey(className)) {
            mAllMethodsMap.put(className, mGroupedMethodsMap.get(className));
        } else {
            mAllMethodsMap.put(className, new ArrayList<AnnotatedMethod>());
        }

        String superClass = helper.getSuperClass(className);
        if (superClass == null) {                           //没有superclass 不需要merge过程
            mMergedSet.add(className);
            return;
        }

        if (!mMergedSet.contains(superClass)) { //父结点没有被处理 去处理父结点
            recursiveDealClass(superClass);
        }

        mergeMethod(mAllMethodsMap.get(className), mAllMethodsMap.get(superClass));
        mMergedSet.add(className);
    }

    private void mergeMethod(List<AnnotatedMethod> toList, List<AnnotatedMethod> fromList) {
        for (AnnotatedMethod from : fromList) {
            boolean same = false;
            for (AnnotatedMethod to : toList) {
                ExecutableElement toEle = to.getExecutableElement();
                ExecutableElement fromEle = from.getExecutableElement();
                if (compare(toEle, fromEle)) {
                    same = true;
                    break;
                }
            }
            if (!same) {
                toList.add(from);
            }
        }
        return;
    }

    /**
     * true: the same,false: different
     * 这里我认为名字 参数个数和类型 返回值都一致时认为同一函数 不管private public等
     */
    private boolean compare(@NonNull ExecutableElement to, @NonNull ExecutableElement from) {
        if (!to.getSimpleName().equals(from.getSimpleName())) {
            return false;
        }
        if (!to.getReturnType().equals(from.getReturnType())) {
            return false;
        }
        if (to.getParameters().size() != from.getParameters().size()) {
            return false;
        } else {
            List<? extends VariableElement> toParams = to.getParameters();
            List<? extends VariableElement> fromParams = from.getParameters();

            for (int i = 0; i < toParams.size(); i++) {
                if (!toParams.get(i).asType().equals(fromParams.get(i).asType())) { //类型不同
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * 找到method从属的class
     */
    @Nullable
    private TypeElement findEnclosingClass(@NonNull ExecutableElement element) throws ProcessingException {
        TypeElement enclosingClass;
        ExecutableElement methodElement = element;
        while (true) {
            Element enclosingElement = methodElement.getEnclosingElement();
            if (enclosingElement.getKind() == ElementKind.CLASS) {
                enclosingClass = (TypeElement) enclosingElement;
                break;
            } else if (enclosingElement.getKind() == ElementKind.INTERFACE) {
                throw new ProcessingException(element, "not support Interface yet!"); //throw Error
            }
        }
        return enclosingClass;
    }

    public static void error(@NonNull Messager messager, @Nullable Element e, @NonNull String msg, @Nullable Object... args) {
        messager.printMessage(Diagnostic.Kind.ERROR, String.format(msg, args), e);
    }

    public static void warn(@NonNull Messager messager, @Nullable Element e, @NonNull String msg, @Nullable Object... args) {
        messager.printMessage(Diagnostic.Kind.WARNING, String.format(msg, args), e);
    }

    public static void info(@NonNull Messager messager, @Nullable Element e, @NonNull String msg, @Nullable Object... args) {
        messager.printMessage(Diagnostic.Kind.NOTE, String.format(msg, args), e);
    }
}
