package wuxian.me.logannotations.compiler;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import wuxian.me.logannotations.Log;
import wuxian.me.logannotations.NoLog;

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
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.tools.Diagnostic;

/**
 * Created by wuxian on 22/11/2016.
 * <p>
 */

@SupportedAnnotationTypes(value = {"wuxian.me.logannotations.Log", "wuxian.me.logannotations.NoLog"})
public class LogAnnotationsProcessor extends AbstractProcessor {
    @NonNull
    private Elements elementUtils;
    @NonNull
    private Filer filer;
    @NonNull
    private Messager messager;

    //Map<classname,list<method>>
    private final @NonNull Map<String, List<AnnotatedMethod>> mGroupedMethodsMap =
            new LinkedHashMap<>();

    //merge结果map class没有superclass的时候自动为true,否则为false,只有merge过才记为true
    private final Map<String, Boolean> mMergedClassMap = new HashMap<>();

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
            collectAnnotations(Log.class, roundEnv);
        } catch (ProcessingException e) {
            error(messager, e.getElement(), e.getMessage());
        }
        info(messager, null, "begin to deal class inheritance");
        dealClassInheritance();

        info(messager, null, "begin to write log");
        writeLogsToJavaFile();
        return true;
    }

    private void collectAnnotations(Class<? extends Annotation> annotationClass, @NonNull RoundEnvironment roundEnv) throws ProcessingException {
        Set<? extends Element> elements = roundEnv.getElementsAnnotatedWith(annotationClass);
        //info(messager, null, "Processing %d elements annotated with @%s", elements.size(), elements);

        for (Element element : elements) {
            if (element.getKind() != ElementKind.METHOD) {
                throw new ProcessingException(element,
                        String.format("Only methods can be annotated with @%s",
                                annotationClass.getSimpleName()));
            } else {
                ExecutableElement executableElement = (ExecutableElement) element;
                try {
                    info(messager, null, String.format("process method %s", executableElement.getSimpleName().toString()));
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
                info(messager, null, "annotated by NoLog");
                return;
            }

            AnnotatedMethod annotatedMethod = new AnnotatedMethod(executableElement, annotationClass);
            String className = enclosingClass.getQualifiedName().toString();  //将该element存入一个class的map中
            List<AnnotatedMethod> groupedMethods = mGroupedMethodsMap.get(className);
            if (groupedMethods == null) {
                groupedMethods = new ArrayList<>();
                info(messager, null, String.format("add class:%s to mGroupedMethodMap", className));
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
     * 子类自动继承所有父类的@Log annotation
     */
    private void dealClassInheritance() {
        Set<String> classNames = mGroupedMethodsMap.keySet();
        Iterator<String> iterator = classNames.iterator();
        if (!iterator.hasNext()) {
            return;
        }
        TypeElement classTypeElement = elementUtils.getTypeElement(iterator.next());
        PackageElement packageElement = elementUtils.getPackageOf(classTypeElement);

        info(messager, null, "begin init helper");
        ClassInheritanceHelper helper;
        try {
            helper = ClassInheritanceHelper.getInstance(messager, elementUtils, packageElement);
        } catch (ProcessingException e) {
            return;
        }

        info(messager, null, "begin deal inheritance after init helper");
        iterator = classNames.iterator();  //重新赋值
        while (iterator.hasNext()) {
            String className = iterator.next();
            if (mMergedClassMap.containsKey(className)) { //该结点已经被处理
                continue;
            }
            recursiveDealClass(helper, className);
        }
    }

    private void recursiveDealClass(ClassInheritanceHelper helper, String className) {
        String superClass = helper.getSuperClass(className);
        if (superClass == null) {                           //没有superclass 不需要merge过程
            mMergedClassMap.put(className, true);
            return;
        }

        if (!mGroupedMethodsMap.containsKey(superClass)) { //尽管该class有superclass 但该superclass没有被注解的函数 因此不需要merge过程
            mMergedClassMap.put(className, true);
            return;
        }

        if (!mMergedClassMap.containsKey(superClass)) { //父结点没有被处理 先处理父结点
            recursiveDealClass(helper, superClass);
        }

        mergeMethod(mGroupedMethodsMap.get(superClass), mGroupedMethodsMap.get(className));
        mMergedClassMap.put(className, true);

    }

    /**
     * 去重？子annotated的标记比如public protectd,参数的不同会被认为是不同的method么？
     * TODO: debug and test
     */
    private void mergeMethod(List<AnnotatedMethod> to, List<AnnotatedMethod> from) {
        to.removeAll(from); //先去重
        to.addAll(from);    //再合并
        return;
    }

    /**
     * TODO
     * 写文件
     */
    private void writeLogsToJavaFile() {
        ;
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
