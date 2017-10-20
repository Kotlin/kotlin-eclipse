# Kotlin Eclipse Plugin Developer Documentation

## Kotlin Eclipse plugins overview

Kotlin plugin consists of several plugins, here is a short description for each of them:

- `kotlin-bundled-compiler`: This plugin is used as a dependency for each other plugin, it exports main jars to work with Kotlin (`kotlin-compiler.jar`, `kotlin-ide-common.jar`...).
Kotlin compiler will be used as a bundled compiler in built plugin and as a library during development.

    Also, `kotlin-bundled-compiler` plugin contains several helper classes for IDE-features (such as formatter) that are coming from Intellij IDEA.

- `kotlin-eclipse-aspects`: This plugin provides several aspects to weave into Eclipse and JDT internals.

- `kotlin-eclipse-core`: This plugin is used to interact with the Kotlin compiler to configure it and provide such features as analysis, compilation and interoperability with Java.

- `kotlin-eclipse-maven`: This plugin depends on `m2e` plugin and provides functionality to configure maven project with Kotlin.

- `kotlin-eclipse-ui`: This plugin provides IDE features through the standard Eclipse and JDT extension points.
    
- `kotlin-eclipse-test-framework`: This plugin contains useful utils and mock classes to write tests
    
- `kotlin-eclipse-ui-test`: This plugin contains functional tests for IDE features
    
#### Interoperability with JDT

Existing Java code can be called from Kotlin in a natural way, and Kotlin code can be used from Java.
Java code in Eclipse should understand Kotlin. Such features as navigation, refactorings, find usages, and others should work together with Kotlin and Java.

##### Light classes

Note that Kotlin does not have presentation compiler as Java or Scala, instead of this, 
Kotlin plugin generates so called "light class files": translated Kotlin source code to the bytecode declarations without bodies.
Each project with Kotlin in Eclipse depends on `KOTLIN_CONTAINER` (see [`KotlinClasspathContainer`](https://github.com/JetBrains/kotlin-eclipse/blob/master/kotlin-eclipse-core/src/org/jetbrains/kotlin/core/KotlinClasspathContainer.kt)) 
which contains `kotlin-stdlib.jar`, `kotlin-reflect.jar` and folder with light classfiles (`kotlin_bin`).
Light classes are stored only in virtual memory and managed by special file system (see [`KotlinFileSystem`](https://github.com/JetBrains/kotlin-eclipse/blob/master/kotlin-eclipse-core/src/org/jetbrains/kotlin/core/filesystem/KotlinFileSystem.java)
, [`KotlinFileStore`](https://github.com/JetBrains/kotlin-eclipse/blob/master/kotlin-eclipse-core/src/org/jetbrains/kotlin/core/filesystem/KotlinFileStore.kt)), 
so they don't add any value on runtime.
  
Let us describe what is happening on each file save.
On each file save Eclipse triggers Kotlin builder, then method [`KotlinLightClassGeneration.updateLightClasses()`](https://github.com/JetBrains/kotlin-eclipse/blob/master/kotlin-eclipse-core/src/org/jetbrains/kotlin/core/asJava/KotlinLightClassGeneration.kt) 
is called which takes affected files and computes names of class files that can be created from the affected source files. 
If we don't found light class in our cache, we create new empty class file  in our file system. If file exists, we touch that file. 
After this, Eclipse determines that some class files on the classpath were added or changed which triggers reindex for those files 
by calling method [`KotlinFileStore.openInputStream`](https://github.com/JetBrains/kotlin-eclipse/blob/master/kotlin-eclipse-core/src/org/jetbrains/kotlin/core/filesystem/KotlinFileStore.kt#L46). 
This method generates bytecode for the light class by calling Kotlin compiler in special mode ([`KotlinLightClassGeneration.buildLightClasses`](https://github.com/JetBrains/kotlin-eclipse/blob/master/kotlin-eclipse-core/src/org/jetbrains/kotlin/core/asJava/KotlinLightClassGeneration.kt#L43)).
Basically, now Java can see Kotlin sources as special binary dependency.

##### Light classes to Kotlin source code

Existence of light classes allows to call Kotlin code from Java in Eclipse, but to navigate from Java to Kotlin source code we have to map light classes to the source code.
Otherwise we'll navigate to the binary code. Unfortunately, Eclipse JDT does not provide extension point to handle such case and to do so, we use aspects to weave
into Java navigation mechanism. We provide simple aspect ([`KotlinOpenEditorAspect.aj`](https://github.com/JetBrains/kotlin-eclipse/blob/master/kotlin-eclipse-aspects/src/org/jetbrains/kotlin/aspects/navigation/KotlinOpenEditorAspect.aj)), 
which weaves into `org.eclipse.jdt.internal.ui.javaeditor.EditorUtility.openInEditor` method and checks input element. 
If this element belongs to our special file system, then we are trying to find corresponding source element in Kotlin and navigate to it.

### Editor Actions

Kotlin plugin provides editors for usual Kotlin files (`.kt`), Kotlin script files (`.kts`) and Kotlin binary files (`.class` files).
Each editor implements common interface [`KotlinEditor`](https://github.com/JetBrains/kotlin-eclipse/blob/master/kotlin-eclipse-ui/src/org/jetbrains/kotlin/ui/editors/KotlinEditor.kt).
Editors for Kotlin files and script files also implement [`KotlinCommonEditor`](https://github.com/JetBrains/kotlin-eclipse/blob/master/kotlin-eclipse-ui/src/org/jetbrains/kotlin/ui/editors/KotlinCommonEditor.kt).
[`KotlinCommonEditor`](https://github.com/JetBrains/kotlin-eclipse/blob/master/kotlin-eclipse-ui/src/org/jetbrains/kotlin/ui/editors/KotlinCommonEditor.kt) 
extends Java editor (`CompilationUnitEditor`) and provides own editor actions (see [`createActions`](https://github.com/JetBrains/kotlin-eclipse/blob/master/kotlin-eclipse-ui/src/org/jetbrains/kotlin/ui/editors/KotlinCommonEditor.kt#L108))

#### Organize imports action example

As an example of editor action let's consider how organize imports works. Organize imports action is registered in [`createActions`](https://github.com/JetBrains/kotlin-eclipse/blob/master/kotlin-eclipse-ui/src/org/jetbrains/kotlin/ui/editors/KotlinCommonEditor.kt#L145) 
method with the corresponding action ID. As we reuse Java editor, we don't have to set up shortcuts, they will be the same as for Java editor.
The main method for this action is [`KotlinOrganizeImportsAction.run`](https://github.com/JetBrains/kotlin-eclipse/blob/master/kotlin-eclipse-ui/src/org/jetbrains/kotlin/ui/editors/organizeImports/KotlinOrganizeImportsAction.kt#L59). 

First of all, it collects missing imports, adds them to the existing imports and then runs [`KotlinOrganizeImportsAction.optimizeImports`](https://github.com/JetBrains/kotlin-eclipse/blob/master/kotlin-eclipse-ui/src/org/jetbrains/kotlin/ui/editors/organizeImports/KotlinOrganizeImportsAction.kt#L86).
This method removes duplicates and unused imports, reorganize imports and replace some explicit imports with the start import. Important part here is that we
reuse code to optimize imports from Kotlin plugin for the Intellij IDEA. The original method that is called from the Eclipse plugin is [`buildOptimizedImports`](https://github.com/JetBrains/kotlin/blob/master/idea/ide-common/src/org/jetbrains/kotlin/idea/util/OptimizedImportsBuilder.kt#L87), 
which is used in the plugin for IDEA.

#### Code reuse from the IDEA plugin

Eclipse plugin depends on `kotlin-ide-common.jar` artifact, which provides common functionality for IDEA and Eclipse plugin. 
Basically, this is a module ([`ide-common`](https://github.com/JetBrains/kotlin/tree/master/idea/ide-common)) in Kotlin project with minimum dependencies, 
so it can be used in Eclipse or Netbeans plugin. This module provides several utils that are used across the Eclipse plugin. 
For example, such feature as completion in the Eclipse plugin is mostly reuse parts of completion from the IDEA plugin 
([`KotlinCompletionUtils.getReferenceVariants`](https://github.com/JetBrains/kotlin-eclipse/blob/master/kotlin-eclipse-ui/src/org/jetbrains/kotlin/ui/editors/completion/KotlinCompletionUtils.kt#L84) 
uses [`ReferenceVarianceHelper`](https://github.com/JetBrains/kotlin/blob/master/idea/ide-common/src/org/jetbrains/kotlin/idea/codeInsight/ReferenceVariantsHelper.kt)).
Generally, it's a preferable way to implement features in the Eclipse plugin, i.e. to reuse parts from the IDEA plugin. Unfortunately, there is no
common way to do this because of different models of IDEs.

### Kotlin compilation and launch

Kotlin Eclipse plugin does not support incremental compilation or presentation compiler. Kotlin files are compiled using the 
Kotlin compiler in [`KotlinCompiler.compileKotlinFiles`](https://github.com/JetBrains/kotlin-eclipse/blob/master/kotlin-eclipse-core/src/org/jetbrains/kotlin/core/compiler/KotlinCompiler.java#L48).
 
#### Kotlin Builder

Kotlin plugin uses concept of Eclipse builder to track changes and compile Kotlin files if needed. For a usual change in project, 
Kotlin builder only updates light classes. Then, if project is building to launch the application ([`KotlinBuilder.isBuildingForLaunch`](https://github.com/JetBrains/kotlin-eclipse/blob/master/kotlin-eclipse-ui/src/org/jetbrains/kotlin/ui/builder/KotlinBuilder.kt#L180)), 
Kotlin builder compiles Kotlin files (in [`KotlinBuilder.build`](https://github.com/JetBrains/kotlin-eclipse/blob/master/kotlin-eclipse-ui/src/org/jetbrains/kotlin/ui/builder/KotlinBuilder.kt#L63)). 
Therefore, Kotlin builder should always precede Java builder.

#### Kotlin Nature

There is a Kotlin [nature](https://github.com/JetBrains/kotlin-eclipse/blob/master/kotlin-eclipse-core/src/org/jetbrains/kotlin/core/model/KotlinNature.kt) 
to mark Kotlin projects.

#### Kotlin Debugger

Kotlin Eclipse plugin is using the standard debugger for Java in Eclipse. 
For example, there are [`KotlinToggleBreakpointAdapter`](https://github.com/JetBrains/kotlin-eclipse/blob/master/kotlin-eclipse-ui/src/org/jetbrains/kotlin/ui/debug/KotlinToggleBreakpointAdapter.kt) 
and [`KotlinRunToLineAdapter`](https://github.com/JetBrains/kotlin-eclipse/blob/master/kotlin-eclipse-ui/src/org/jetbrains/kotlin/ui/debug/KotlinRunToLineAdapter.kt) 
adapters to add breakpoint to a specific line and to support action "run to cursor".     

### Kotlin environment and project analysis

In order to analyse files and use compiler API, [`KotlinEnvironment`](https://github.com/JetBrains/kotlin-eclipse/blob/master/kotlin-eclipse-core/src/org/jetbrains/kotlin/core/model/KotlinEnvironment.kt#L207) 
have to be configured. Basically, `KotlinEnvironment` is created for each project in Eclipse and maps external (from Eclipse) project model to the internal one. 
Also, it registers various services and configures dependencies, see [`KotlinEnvironment.configureClasspath`](https://github.com/JetBrains/kotlin-eclipse/blob/master/kotlin-eclipse-core/src/org/jetbrains/kotlin/core/model/KotlinEnvironment.kt#L231). 
Important note: if classpath was changed, corresponding Kotlin environment should be recreated.  

#### Kotlin parsing

There are PSI and Kt elements that are basically represent concrete syntax tree of Kotlin program. To get parsed version of some source file
[`KotlinPsiManager`](https://github.com/JetBrains/kotlin-eclipse/blob/master/kotlin-eclipse-core/src/org/jetbrains/kotlin/core/builder/KotlinPsiManager.kt) 
should be used. Note that it caches last version of `KtFile`, so to get actual `KtFile`, source code of file can be
explicitly passed to the [`getKotlinFileIfExist`](https://github.com/JetBrains/kotlin-eclipse/blob/master/kotlin-eclipse-core/src/org/jetbrains/kotlin/core/builder/KotlinPsiManager.kt#L368) 
method, or you can use [`commitFile`](https://github.com/JetBrains/kotlin-eclipse/blob/master/kotlin-eclipse-core/src/org/jetbrains/kotlin/core/builder/KotlinPsiManager.kt#L389) 
to reparse and cache changed file.

#### "Remove explicit type" quick assist example

Let's consider how "Remove explicit type" quick assist works. This quick assist removes explicitly written type reference for property, 
function and loop parameter, i.e. it converts `val s: String = "value"` to `val s = "value"`.
  
First of all, when user invokes quick assist on some element (`ctrl+1`), method
[`KotlinQuickAssist.isApplicable`](https://github.com/JetBrains/kotlin-eclipse/blob/master/kotlin-eclipse-ui/src/org/jetbrains/kotlin/ui/editors/quickassist/KotlinQuickAssist.kt#L32) 
is called. `isApplicable` method obtains current PSI element ([`getActiveElement`](https://github.com/JetBrains/kotlin-eclipse/blob/master/kotlin-eclipse-ui/src/org/jetbrains/kotlin/ui/editors/quickassist/KotlinQuickAssist.kt#L37)),
it gets PSI file and then calls `findElementAt` to get concrete element at specific offset.

Once we get active PSI element, we pass it to our quick assists and check for theirs applicability. 
[`KotlinRemoveExplicitTypeAssistProposal.isApplicable`](https://github.com/JetBrains/kotlin-eclipse/blob/master/kotlin-eclipse-ui/src/org/jetbrains/kotlin/ui/editors/quickassist/KotlinRemoveExplicitTypeAssistProposal.kt#L35)
checks that active PSI element is actually property, function or loop parameter with the type reference. 
In method [`KotlinRemoveExplicitTypeAssistProposal.apply`](https://github.com/JetBrains/kotlin-eclipse/blob/master/kotlin-eclipse-ui/src/org/jetbrains/kotlin/ui/editors/quickassist/KotlinRemoveExplicitTypeAssistProposal.kt#L62) 
quick assist executes and removes corresponding type reference.

This and other quick assists and actions are dramatically use knowledge of CST for Kotlin. In order to make it easier, there is an action
"View Psi Structure for Current File" in the context menu for Kotlin file, which can be used to examine structure of the Kotlin CST.

#### Kotlin analyzer

Many features in IDE requires more deep knowledge of the Kotlin program. For example, [`KotlinLineAnnotationsReconciler`](https://github.com/JetBrains/kotlin-eclipse/blob/master/kotlin-eclipse-ui/src/org/jetbrains/kotlin/ui/editors/annotations/AnnotationManager.kt#L146)
is used  to show diagnostics from the compiler. It uses concept of `ReconcilingStrategy` and runs after each change in active file,
when analysis results for the file will be ready and cached. The main line there is `KotlinAnalyzer.analyzeFile(file)...`, which returns
analysis results and can be used to get diagnostics from the compiler.

Kotlin compiler uses map called `BindingContext` to contain all types and internal representations (descriptors) for expressions in program. 
To get binding context one can use [`KotlinAnalyzer.analyzeFile(file).bindingContext`](https://github.com/JetBrains/kotlin-eclipse/blob/master/kotlin-eclipse-core/src/org/jetbrains/kotlin/core/resolve/KotlinAnalyzer.kt#L26). 
See [`KotlinSemanticHighlighter`](https://github.com/JetBrains/kotlin-eclipse/blob/master/kotlin-eclipse-ui/src/org/jetbrains/kotlin/ui/editors/highlighting/KotlinSemanticHighlighting.kt) 
for an example of the compiler analysis use. There are several methods in [`KotlinSemanticHighlightingVisitor`](https://github.com/JetBrains/kotlin-eclipse/blob/master/kotlin-eclipse-ui/src/org/jetbrains/kotlin/ui/editors/highlighting/KotlinSemanticHighlightingVisitor.kt), 
which uses binding context to obtain information either from a declaration ([`visitProperty`](https://github.com/JetBrains/kotlin-eclipse/blob/master/kotlin-eclipse-ui/src/org/jetbrains/kotlin/ui/editors/highlighting/KotlinSemanticHighlightingVisitor.kt#L123)), 
or from a reference ([`visitSimpleNameExpression`](https://github.com/JetBrains/kotlin-eclipse/blob/master/kotlin-eclipse-ui/src/org/jetbrains/kotlin/ui/editors/highlighting/KotlinSemanticHighlightingVisitor.kt#L82)).     

