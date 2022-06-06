//
// (c) 2022 teddyxlandlee
// This buildscript is licensed under LGPL v3 or later.
// see <https://www.gnu.org/licenses/lgpl-3.0.html> for more info.
//

buildscript {
    dependencies {
        classpath(group = "org.ow2.asm", name = "asm", version = "9.2")
    }
}

fun String.toSlashed() = this.replace('.', '/')

class ModuleInfo(val moduleName: String, val moduleVersion: String? = null, val open: Boolean = false) {
    private val exports: MutableMap<String, Array<out String>?> = mutableMapOf()
    private val requires: MutableMap<String, String?> = mutableMapOf()
    private val requiresTransitive: MutableMap<String, String?> = mutableMapOf()
    private val opens: MutableMap<String, Array<out String>?> = mutableMapOf()
    private val provides: MutableMap<String, Array<out String>?> = mutableMapOf()

    private val requiresList = mutableSetOf<String>()

    fun toByteArray() : ByteArray {
        val cw = ClassWriter(0)
        cw.visit(ops.V9, ops.ACC_MODULE, "module-info",
                null, null, null)
        cw.visitSource("asm-gen:teddyxlandlee", null)
        cw.visitModule(moduleName,
                if (open) ops.ACC_OPEN else 0,
                moduleVersion).run {
            requires.forEach { (module, version) ->
                visitRequire(module, 0, version)
            }
            requiresTransitive.forEach { (module, version) ->
                visitRequire(module, ops.ACC_TRANSITIVE, version)
            }
            if ("java.base" !in requiresList)
                visitRequire("java.base", ops.ACC_MANDATED, null)

            exports.forEach { (pkg, to) ->
                visitExport(pkg, 0, *(to ?: arrayOf()))
            }
            opens.forEach { (pkg, to) ->
                visitOpen(pkg, 0, *(to ?: arrayOf()))
            }
            provides.forEach { (spi, impl) ->
                visitProvide(spi, *(impl ?: arrayOf()))
            }
        }
        cw.visitEnd()
        return cw.toByteArray()
    }

    fun exports(pkg: String, vararg to: String) {
        exports[pkg.toSlashed()] = to
    }

    fun exports(pkg: String) {
        exports[pkg.toSlashed()] = null
    }

    private fun checkReqExists(module: String) {
        if (!requiresList.add(module)) {    // already exists
            requires.remove(module)
            requiresTransitive.remove(module)
        }
    }

    fun requires(module: String, version: String?) {
        checkReqExists(module)
        requires[module] = version
    }

    fun requires(vararg modules: String) {
        modules.forEach(::requires)
    }

    fun requires(module: String) {
        checkReqExists(module)
        requires[module] = null
    }

    fun requiresTransitive(module: String, version: String?) {
        checkReqExists(module)
        requiresTransitive[module] = version
    }

    fun requiresTransitive(vararg modules: String) {
        modules.forEach(::requiresTransitive)
    }

    fun requiresTransitive(module: String) {
        checkReqExists(module)
        requiresTransitive[module] = null
    }

    fun opens(pkg: String, vararg to: String) {
        if (open)
            throw kotlin.RuntimeException("module is open")
        opens[pkg.toSlashed()] = to
    }

    fun opens(pkg: String) {
        if (open)
            throw RuntimeException("module is open")
        opens[pkg.toSlashed()] = null
    }

    fun opens(vararg packages: String) {
        packages.forEach(::opens)
    }

    fun provides(spi: String, vararg impl: String) {
        if (impl.isEmpty()) throw kotlin.RuntimeException("Implementation of $spi should not be empty")
        val impl0 = Array(impl.size) { impl[it].toSlashed() }
        provides[spi.toSlashed()] = impl0
    }
}


