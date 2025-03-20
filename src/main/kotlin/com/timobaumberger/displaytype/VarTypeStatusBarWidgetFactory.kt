package com.timobaumberger.displaytype

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.openapi.wm.impl.status.widget.StatusBarEditorBasedWidgetFactory

class VarTypeStatusBarWidgetFactory: StatusBarEditorBasedWidgetFactory() {
    override fun getDisplayName(): String = "Variable Type"

    override fun getId(): String = WIDGET_ID

    override fun createWidget(project: Project): StatusBarWidget {
        return VarTypeStatusBarWidget(project)
    }
}