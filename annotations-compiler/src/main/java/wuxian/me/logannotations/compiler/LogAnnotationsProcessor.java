package wuxian.me.logannotations.compiler;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import wuxian.me.logannotations.LOG;
import wuxian.me.logannotations.NoLog;

import java.io.File;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.HashMap;
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
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.Elements;
import javax.tools.Diagnostic;

/**
 * Created by wuxian on 22/11/2016.
 * <p>
 */

@SupportedAnnotationTypes(value = {"wuxian.me.logannotations.LOG", "wuxian.me.logannotations.NoLog"})
public class LogAnnotationsProcessor extends AbstractProcessor {
    @NonNull
    private Elements elementUtils;
    @NonNull
    private Filer filer;
    @NonNull
    private Messager messager;

    //Map<classname,list<method>>
    //不含被NoLog注解的class
    private final @NonNull Map<String, List<AnnotatedMethod>> mGroupedMethodsMap =
            new LinkedHashMap<>();

    //merge结果map class没有superclass的时候自动为true,否则为false,只有merge过才记为true,
    //包括被NoLog注解的class
    private final Map<String, Boolean> mMergedClassMap = new HashMap<>();

    //存储所有class的methods,
    //包括被NoLog注解的class
    private final Map<String, List<AnnotatedMethod>> mAllMethodsMap = new LinkedHashMap<>();

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
        info(messager, null, "begin to collect annotations");
        try {
            collectAnnotations(LOG.class, roundEnv);
        } catch (ProcessingException e) {
            error(messager, e.getElement(), e.getMessage());
        }
        info(messager, null, "begin to deal class inheritance");
        dealClassInheritance();

        info(messager, null, "begin to save log");
        writeLogsToJavaFile();
        return true;
    }

    private void collectAnnotations(Class<? extends Annotation> annotationClass, @NonNull RoundEnvironment roundEnv) throws ProcessingException {
        Set<? extends Element> elements = roundEnv.getElementsAnnotatedWith(annotationClass);

        for (Element element : elements) {
            if (element.getKind() != ElementKind.METHOD) {
                throw new ProcessingException(element,
                        String.format("Only methods can be annotated with @%s",
                                annotationClass.getSimpleName()));
            } else {
                ExecutableElement executableElement = (ExecutableElement) element;
                try {
                    processMethod(executableElement, annotationClass, roundEnv);
                } catch (IllegalArgumentException e) {
                    throw new ProcessingException(executableElement, e.getMessage());
                }
            }
        }
    }

    private void processMethod(ExecutableElement executableElement, Class<? extends Annotation> annotationClass, @NonNull RoundEnvironment roundEnv) throws ProcessingException {
        //checkMethodValidity(annotatedMethod);
        TypeElement enclosingClass = findEnclosingClass(executableElement);

        if (enclosingClass == null) {
            throw new ProcessingException(null,
                    String.format("Can not find enclosing class for method %s",
                            executableElement.getSimpleName().toString()));
        } else {
            if (enclosingClass.getAnnotation(NoLog.class) != null) { //类被NoLog注解 不处理这个类里面的log
                return;
            }

            AnnotatedMethod annotatedMethod = new AnnotatedMethod(executableElement, annotationClass);
            String className = enclosingClass.getQualifiedName().toString();  //将该element存入一个class的map中
            List<AnnotatedMethod> groupedMethods = mGroupedMethodsMap.get(className);
            if (groupedMethods == null) {
                groupedMethods = new ArrayList<>();
                mGroupedMethodsMap.put(className, groupedMethods);
            }
            groupedMethods.add(annotatedMethod);
        }
    }

    /**
     * 合法性校验 被@Log注解的函数不能是PRIVATE,PROTECTED,ABSTRACT
     * ??????
     */
    private void checkMethodValidity(@NonNull AnnotatedMethod item) throws ProcessingException {
        ExecutableElement methodElement = item.getExecutableElement();
        Set<Modifier> modifiers = methodElement.getModifiers();

        // The annotated method needs to be accessible by the generated class which will have
        // the same package. Public or "package private" (default) methods are required.
        if (modifiers.contains(Modifier.PRIVATE) || modifiers.contains(Modifier.PROTECTED)) {
            throw new ProcessingException(methodElement,
                    String.format("The method %s can not be private or protected.",
                            methodElement.getSimpleName().toString()));
        }

        // We cannot annotate abstract methods, we need to annotate the actual implementation of
        // the method on the implementing class.
        if (modifiers.contains(Modifier.ABSTRACT)) {
            throw new ProcessingException(methodElement, String.format(
                    "The method %s is abstract. You can't annotate abstract methods with @%s",
                    methodElement.getSimpleName().toString(), AnnotatedMethod.class.getSimpleName()));
        }
    }

    /**
     * 找到method从属的class
     */
    @Nullable
    private TypeElement findEnclosingClass(@NonNull ExecutableElement element) {
        TypeElement enclosingClass;

        ExecutableElement methodElement = element;
        while (true) {
            Element enclosingElement = methodElement.getEnclosingElement();
            if (enclosingElement.getKind() == ElementKind.CLASS) {
                enclosingClass = (TypeElement) enclosingElement;
                break;
            }
        }
        return enclosingClass;
    }

    /**
     * 子类自动继承所有父类的@LOG annotation
     */
    private void dealClassInheritance() {
        Set<String> classNames = mGroupedMethodsMap.keySet();
        Iterator<String> iterator;
        ClassInheritanceHelper helper;
        try {
            helper = ClassInheritanceHelper.getInstance(messager, elementUtils);
        } catch (ProcessingException e) {
            return;
        }

        iterator = classNames.iterator();  //重新赋值
        while (iterator.hasNext()) {
            String className = iterator.next();
            if (!mMergedClassMap.containsKey(className)) { //该结点未处理 先去处理
                recursiveDealClass(helper, className);
            }

            mGroupedMethodsMap.put(className, mAllMethodsMap.get(className));
        }
    }

    private void recursiveDealClass(ClassInheritanceHelper helper, String className) {

        if (mGroupedMethodsMap.containsKey(className)) {
            mAllMethodsMap.put(className, mGroupedMethodsMap.get(className));
        } else {
            mAllMethodsMap.put(className, new ArrayList<AnnotatedMethod>());
        }

        String superClass = helper.getSuperClass(className);
        if (superClass == null) {                           //没有superclass 不需要merge过程
            mMergedClassMap.put(className, true);
            return;
        }

        if (!mMergedClassMap.containsKey(superClass)) { //父结点没有被处理 去处理父结点
            recursiveDealClass(helper, superClass);
        }

        mergeMethod(mAllMethodsMap.get(className), mAllMethodsMap.get(superClass));
        mMergedClassMap.put(className, true);
    }

    /**
     * 去重
     */
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
     * 写文件
     */
    private void writeLogsToJavaFile() {
        JavaFileWriter.initMessager(messager);
        for (String classNameString : mGroupedMethodsMap.keySet()) {
            JavaFileWriter writer = new JavaFileWriter();
            writer.open(classNameString).addImportIfneed();

            for (AnnotatedMethod method : mGroupedMethodsMap.get(classNameString)) {
                writer.writeLogToMethod(method);
            }

            writer.save();
        }
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
