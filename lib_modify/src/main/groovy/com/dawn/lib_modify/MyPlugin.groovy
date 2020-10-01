package com.dawn.lib_modify

import org.gradle.api.Plugin
import org.gradle.api.Project

class MyPlugin implements Plugin<Project>{

    @Override
    void apply(Project project) {
        println "==dawn=====---------------3333------------------------>"
        project.android.registerTransform(new MyTransform(project));

    }
}