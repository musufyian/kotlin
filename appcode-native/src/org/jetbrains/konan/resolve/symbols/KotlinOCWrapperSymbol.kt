/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.konan.resolve.symbols

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Comparing
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.jetbrains.cidr.lang.symbols.*
import org.jetbrains.kotlin.backend.konan.objcexport.Stub
import org.jetbrains.kotlin.idea.util.application.runReadAction
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater.newUpdater

abstract class KotlinOCWrapperSymbol<State : KotlinOCWrapperSymbol.StubState, Stb : Stub<*>>(
    stub: Stb,
    project: Project,
    @Transient private val file: VirtualFile
) : OCSymbol, OCForeignSymbol {

    private val name: String = stub.name

    @Volatile
    private var _state: State? = null

    @Transient
    @Volatile
    private var stubAndProject: Pair<Stb, Project>? = Pair(stub, project)

    protected val state: State
        get() {
            _state?.let { return it }
            stubAndProject?.let { (stub, project) ->
                //todo check project.isDisposed
                val newState = runReadAction { computeState(stub, project) }
                if (valueUpdater.compareAndSet(this, null, newState)) {
                    stubAndProject = null
                    return newState
                }
            }
            return _state!!
        }

    private fun psi(project: Project): PsiElement? {
        stubAndProject?.let { return it.first.psi }
        return OCSymbolBase.doLocateDefinition(this, project, KtNamedDeclaration::class.java)
    }

    override fun getName(): String = name

    override fun getComplexOffset(): Long =
        _state?.offset
        ?: stubAndProject?.first?.offset
        ?: _state!!.offset

    protected abstract fun computeState(stub: Stb, project: Project): State

    override fun getContainingFile(): VirtualFile = file

    override fun deepEqualStep(c: DeepEqual.Comparator, first: Any, second: Any): Boolean {
        val f = first as KotlinOCWrapperSymbol<*, *>
        val s = second as KotlinOCWrapperSymbol<*, *>

        if (!Comparing.equal(f.file, s.file)) return false
        if (!Comparing.equal(f.state, s.state)) return false

        return true
    }

    override fun hashCodeExcludingOffset(): Int = name.hashCode() * 31 + file.hashCode()

    override fun locateDefinition(project: Project): PsiElement? = psi(project)?.let { KotlinOCPsiWrapper(it, this) }

    override fun isSameSymbol(symbol: OCSymbol?, project: Project): Boolean {
        return super.isSameSymbol(symbol, project)
               || symbol is KotlinLightSymbol && psi(project) == symbol.locateDefinition(project)
    }

    override fun updateOffset(start: Int, lengthShift: Int) {
        if (_state == null) return
        super.updateOffset(start, lengthShift)
    }

    override fun setComplexOffset(complexOffset: Long) {
        _state!!.offset = complexOffset
    }

    abstract class StubState(stub: Stub<*>) {
        var offset: Long = stub.offset
            internal set
    }

    companion object {
        private val valueUpdater = newUpdater(
            KotlinOCWrapperSymbol::class.java,
            KotlinOCWrapperSymbol.StubState::class.java,
            "myState"
        )
    }
}

val Stub<*>.offset: Long
    get() = psi?.let { OCSymbolOffsetUtil.getComplexOffset(it.textOffset, 0) } ?: 0
