package wuxian.me.logannotations.compiler.util;

import org.checkerframework.checker.nullness.qual.NonNull;

import javax.annotation.processing.Messager;

import wuxian.me.logannotations.antlr.java.JavaBaseListener;
import wuxian.me.logannotations.antlr.java.JavaParser;
import wuxian.me.logannotations.compiler.LogAnnotationsProcessor;

/**
 * Created by wuxian on 30/11/2016.
 */

public class ClassListener extends JavaBaseListener {
    public static void initMessager(@NonNull Messager msg) {
        if (messager == null) {
            messager = msg;
        }
    }

    private static Messager messager;

    @Override
    public void enterPackageDeclaration(JavaParser.PackageDeclarationContext ctx) {
        //ctx.getText() --> packagewuxian.me.logannotationsdemo;
        LogAnnotationsProcessor.info(messager, null, String.format("package declare: %s", ctx.getText()));
    }

    @Override
    public void enterImportDeclaration(JavaParser.ImportDeclarationContext ctx) {
        LogAnnotationsProcessor.info(messager, null, String.format("import declare: %s", ctx.getText()));
    }
}
