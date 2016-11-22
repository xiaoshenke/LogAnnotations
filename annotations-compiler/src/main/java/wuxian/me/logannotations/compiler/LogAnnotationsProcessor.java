package wuxian.me.logannotations.compiler;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
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
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.tools.Diagnostic;

import wuxian.me.logannotations.Log;

/**
 * Created by wuxian on 22/11/2016.
 * <p>
 * TODO: to be finished
 */

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

    private final List<List<String>> mClassInheritanceMap = new ArrayList<>();

    @Override
    public synchronized void init(@NonNull ProcessingEnvironment env) {
        super.init(env);
        messager = processingEnv.getMessager();
        filer = processingEnv.getFiler();
        elementUtils = processingEnv.getElementUtils();
        info(messager, null, "init annotation processor");
    }

    @Override
    public boolean process(@NonNull Set<? extends TypeElement> set,
                           @NonNull RoundEnvironment roundEnv) {
        info(messager, null, "begin to process annotations");
        try {
            collectAnnotations(Log.class, roundEnv);
        } catch (ProcessingException e) {
            error(messager, e.getElement(), e.getMessage());
        }
        dealClassInheritation();
        writeLogsToJavaFile();
        return true;
    }


    private void collectAnnotations(Class<? extends Annotation> annotationClass, @NonNull RoundEnvironment roundEnv) throws ProcessingException {
        Set<? extends Element> elements = roundEnv.getElementsAnnotatedWith(annotationClass);
        info(messager, null, "Processing %d elements annotated with @%s", elements.size(), elements);

        for (Element element : elements) {
            if (element.getKind() != ElementKind.METHOD) {
                throw new ProcessingException(element,
                        String.format("Only methods can be annotated with @%s",
                                annotationClass.getSimpleName()));
            } else {
                ExecutableElement executableElement = (ExecutableElement) element;
                try {
                    processMethod(executableElement, annotationClass);
                } catch (IllegalArgumentException e) {
                    throw new ProcessingException(executableElement, e.getMessage());
                }
            }
        }
    }


    private void processMethod(ExecutableElement executableElement, Class<? extends Annotation> annotationClass) throws ProcessingException {
        AnnotatedMethod annotatedMethod = new AnnotatedMethod(executableElement, annotationClass);
        checkMethodValidity(annotatedMethod);
        TypeElement enclosingClass = findEnclosingClass(annotatedMethod);
        if (enclosingClass == null) {
            throw new ProcessingException(null,
                    String.format("Can not find enclosing class for method %s",
                            executableElement.getSimpleName().toString()));
        } else {
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
    private TypeElement findEnclosingClass(@NonNull AnnotatedMethod annotatedMethod) {
        TypeElement enclosingClass;

        ExecutableElement methodElement = annotatedMethod.getExecutableElement();
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
     * TODO
     * 子类自动继承所有父类的@Log annotation
     */
    private void dealClassInheritation() {
        Set<String> classNames = mGroupedMethodsMap.keySet();
        Iterator<String> iterator = classNames.iterator();
        if (!iterator.hasNext()) {
            return;
        }
        TypeElement classTypeElement = elementUtils.getTypeElement(iterator.next());
        PackageElement packageElement = elementUtils.getPackageOf(classTypeElement);

        //ClassInheritanceHelper helper = ClassInheritanceHelper.getInstance(elementUtils, packageElement);

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
