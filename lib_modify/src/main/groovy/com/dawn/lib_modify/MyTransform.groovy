package com.dawn.lib_modify

import com.android.SdkConstants
import com.android.annotations.NonNull
import com.android.build.api.transform.Format
import com.android.build.api.transform.QualifiedContent
import com.android.build.api.transform.Transform
import com.android.build.api.transform.TransformException
import com.android.build.api.transform.TransformInvocation
import com.android.build.gradle.internal.pipeline.TransformManager
import javassist.ClassPool
import javassist.CtClass
import javassist.CtMethod
import org.apache.commons.io.FileUtils
import org.gradle.api.Project


class MyTransform extends Transform {
    private Project project;
    private ClassPool pool = ClassPool.default;

    public MyTransform(Project project) {
        this.project = project;
    }

    @Override
    String getName() {
        return "dawn";
    }

    @Override
    Set<QualifiedContent.ContentType> getInputTypes() {
        return TransformManager.CONTENT_CLASS;

    }

    @Override
    Set<? super QualifiedContent.Scope> getScopes() {
        return TransformManager.SCOPE_FULL_PROJECT;
    }

    @Override
    boolean isIncremental() {
        return false;
    }

    @Override
    public void transform(@NonNull TransformInvocation transformInvocation) throws TransformException, InterruptedException, IOException {
        super.transform(transformInvocation);
        project.android.bootClasspath.each{
            pool.appendClassPath(it.absolutePath);
        };
        transformInvocation.inputs.each {
            it.jarInputs.each {//不处理 直接扔到下一个环节
                //把类加载到PC内存
                pool.insertClassPath(it.file.absolutePath);
                def dest = transformInvocation.outputProvider.getContentLocation(it.name, it.contentTypes, it.scopes, Format.JAR);
                FileUtils.copyFile(it.file, dest);//复制到下一个环节

            };

            //class 存放的位置
            it.directoryInputs.each {
                //处理
                def rootFileName = it.file.absolutePath;
//app/build/intermediates/javac/debug/classes。。下的目录
                //把类加载到PC内存
                pool.insertClassPath(rootFileName);
                findTarget(it.file, rootFileName);


                def dest = transformInvocation.outputProvider.getContentLocation(it.name, it.contentTypes, it.scopes, Format.DIRECTORY);
                FileUtils.copyDirectory(it.file, dest);
            };
        }

    }

    private void findTarget(File dir, String fileName) {
        //dir dawn的绝对路径，递归寻找class 然后修改
        if (dir.isDirectory()) {
            dir.listFiles().each {
                findTarget(it, fileName);
            }
        } else {
            //不是文件 是.class
            modify(dir, fileName);
        }

    }
    //修改class fileName 最开始的目录///app/build/intermediates/javac/debug/classes。。下的目录
    //dir .class 文件
    private void modify(File dir, String fileName) {
        def filePath = dir.absolutePath;
        if (!filePath.endsWith(".class")) {
            return;
        }
        if (filePath.contains('R$') || filePath.contains('R.class') || filePath.contains("BuildConfig.class")) {
            return;
        }

        //拿到全类名
        def className = filePath.replace(fileName, "").replace(File.separator, ".");
        def name=className.replace(SdkConstants.DOT_CLASS, "").substring(1);//所有的类名了
//        if(name.contains()){
//
//        }
        CtClass ctClass = pool.get(name);
        def body = "android.widget.Toast.makeText(this,\"111\",android.widget.Toast.LENGTH_LONG).show();";
        addCode(ctClass, body, fileName);

    }

    private void addCode(CtClass ctClass, String body, String fileName) {
        if (ctClass.getName().contains("11111")) {
            return;
        }
        CtMethod[] methods = ctClass.getDeclaredMethods();
        for (method in methods) {
            if(method.getName().contains("isSupport")){
                continue;
            }
            method.insertAfter(body);
        }
        ctClass.writeFile(fileName);
        ctClass.detach();

    }


}