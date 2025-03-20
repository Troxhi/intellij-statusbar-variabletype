package com.timobaumberger.displaytype

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.event.CaretEvent
import com.intellij.openapi.editor.event.CaretListener
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerEvent
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.StatusBar
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.openapi.wm.impl.status.EditorBasedWidget
import com.intellij.psi.PsiManager
import com.jetbrains.python.psi.PyExpression
import com.jetbrains.python.psi.PyFile
import com.jetbrains.python.psi.PyTypedElement
import com.jetbrains.python.psi.types.TypeEvalContext
import java.awt.Component

const val WIDGET_ID = "VarTypeStatusBarWidget"

class VarTypeStatusBarWidget(project: Project) : EditorBasedWidget(project), StatusBarWidget.TextPresentation {
    private val disposable = Disposer.newDisposable()

    private var text: String = ""

    override fun ID(): String = WIDGET_ID

    override fun install(statusBar: StatusBar) {
        super.install(statusBar)

        myConnection.subscribe<FileEditorManagerListener>(
            FileEditorManagerListener.FILE_EDITOR_MANAGER, object : FileEditorManagerListener {
                override fun selectionChanged(event: FileEditorManagerEvent) = requestUpdate()
                override fun fileOpened(source: FileEditorManager, file: VirtualFile) = requestUpdate()
            })

        val multicaster = EditorFactory.getInstance().eventMulticaster
        multicaster.addCaretListener(object : CaretListener {
            override fun caretPositionChanged(event: CaretEvent) = requestUpdate()
            override fun caretAdded(event: CaretEvent) = requestUpdate()
            override fun caretRemoved(event: CaretEvent) = requestUpdate()
        }, disposable)

        requestUpdate()
    }

    override fun getAlignment(): Float = Component.CENTER_ALIGNMENT

    override fun getText(): String = text

    override fun getTooltipText(): String = "Variable Type"

    override fun getPresentation(): StatusBarWidget.WidgetPresentation = this

    fun requestUpdate() {
        ApplicationManager.getApplication().executeOnPooledThread {
            update()
        }
    }

    private fun update() {
        if (isDisposed) return

        val editor = getEditor() ?: return hideAndUpdate()
        if (editor.caretModel.caretCount != 1) return hideAndUpdate()

        val virtualFile = editor.virtualFile ?: return hideAndUpdate()
        val psiFile = runReadAction { PsiManager.getInstance(project).findFile(virtualFile) as? PyFile } ?: return hideAndUpdate()

        val psiElement = runReadAction { psiFile.findElementAt(editor.caretModel.offset) } ?: return hideAndUpdate()
        val psiParent = runReadAction { psiElement.parent as? PyTypedElement } ?: return hideAndUpdate()

        if (psiParent !is PyExpression || psiParent is PyFile) return hideAndUpdate()

        val type = runReadAction {
            TypeEvalContext.codeAnalysis(project, psiFile).getType(psiParent)?.name
        } ?: return hideAndUpdate()

        text = type
        statusBar?.updateWidget(ID())
    }

    private fun hideAndUpdate() {
        text = ""
        statusBar?.updateWidget(ID())
    }

    override fun dispose() {
        super.dispose()
        Disposer.dispose(disposable)
    }
}