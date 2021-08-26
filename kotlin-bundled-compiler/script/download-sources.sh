#!/bin/bash

# Directories
lib_directory="../lib"
download_directory="$lib_directory/download"
dependencies_directory="$lib_directory/dependencies"

mkdir -p "$lib_directory"
mkdir -p "$download_directory"
mkdir -p "$dependencies_directory"


# Download zip sources
intellij_dependencies_repository="https://www.jetbrains.com/intellij-repository/releases"
intellij_dependencies_group="com/jetbrains/intellij/idea"
intellij_dependencies_version="203.8084.24"
intellij_dependencies=(
  "ideaIC"
  "intellij-core"
)
for dependency in "${intellij_dependencies[@]}"; do
  url="$intellij_dependencies_repository/$intellij_dependencies_group/$dependency/$intellij_dependencies_version/"
  url+="$dependency-$intellij_dependencies_version.zip"
  curl "$url" -L -o "$download_directory/$dependency.zip"
done


# Unzip all archives
for file in "$download_directory"/*; do
    if [[ -f $file ]]; then
      unzip "$file" -d "$dependencies_directory"
    fi
done


# Copy needed dependencies
dependencies=(
  "asm-all-9.0"
  "guava-29.0-jre"
  "idea"
  "intellij-core-analysis-deprecated"
  "intellij-core"
  "intellij-deps-fastutil-8.4.1-4"
  "platform-api"
  "platform-ide-util-io"
  "platform-impl"
  "platform-util-ui"
  "trove4j"
  "util"
)
for dependency in "${dependencies[@]}"; do
  filename="$dependency.jar"
  find "$dependencies_directory" -name "$filename" -type f -exec cp {} "$lib_directory" \;
done


# Download kotlin-ide-plugin dependencies
kotlin_ide_plugin_dependencies_repository="https://maven.pkg.jetbrains.space/kotlin/p/kotlin/kotlin-ide-plugin-dependencies"
kotlin_ide_plugin_dependencies_group="org/jetbrains/kotlin"
kotlin_ide_plugin_dependencies_version="1.5.10-release-894"
kotlin_ide_plugin_dependencies=(
  "kotlin-compiler-cli-for-ide"
  "kotlin-compiler-for-ide"
  "kotlin-reflect"
  "kotlin-script-runtime"
  "kotlin-scripting-common"
  "kotlin-scripting-compiler"
  "kotlin-scripting-compiler-impl"
  "kotlin-scripting-jvm"
  "kotlin-stdlib"
)
for dependency in "${kotlin_ide_plugin_dependencies[@]}"; do
  filename="$dependency-$kotlin_ide_plugin_dependencies_version.jar"
  filename_without_version="$dependency.jar"
  url="$kotlin_ide_plugin_dependencies_repository/$kotlin_ide_plugin_dependencies_group/$dependency/$kotlin_ide_plugin_dependencies_version/$filename"
  curl "$url" -L -o "$lib_directory/$filename_without_version"
done


# Download kotlin-ide dependencies
kotlin_ide_repository="https://maven.pkg.jetbrains.space/kotlin/p/kotlin/kotlin-ide"
kotlin_ide_dependency_group="org/jetbrains/kotlin"
kotlin_ide_dependencies_version="203-1.5.10-release-891-IJ7717.8"
kotlin_ide_dependencies=(
  "common"
  "formatter"
  "j2k-old"
)
for dependency in "${kotlin_ide_dependencies[@]}"; do
  filename="$dependency-$kotlin_ide_dependencies_version.jar"
  filename_without_version="$dependency.jar"
  url="$kotlin_ide_repository/$kotlin_ide_dependency_group/$dependency/$kotlin_ide_dependencies_version/$filename"
  curl "$url" -L -o "$lib_directory/$filename_without_version"
done


# Download jna dependency
jna_filename="jna-3.0.9.jar"
jna_dependency_url="https://repo1.maven.org/maven2/com/sun/jna/jna/3.0.9/$jna_filename"
curl "$jna_dependency_url" -L -o "$lib_directory/$jna_filename"

javaslang_filename="javaslang-2.0.6.jar"
javaslang_dependency_url="https://repo1.maven.org/maven2/io/javaslang/javaslang/2.0.6/$javaslang_filename"
curl "$javaslang_dependency_url" -L -o "$lib_directory/$javaslang_filename"

inject_filename="javax.inject-1.jar"
inject_dependency_url="https://repo1.maven.org/maven2/javax/inject/javax.inject/1/$inject_filename"
curl "$inject_dependency_url" -L -o "$lib_directory/$inject_filename"

annotations_filename="annotations-13.0.jar"
annotations_dependency_url="https://repo1.maven.org/maven2/org/jetbrains/annotations/13.0/$annotations_filename"
curl "$annotations_dependency_url" -L -o "$lib_directory/$annotations_filename"

kotlin_stdlib_sources_filename="kotlin-stdlib-sources.jar"
kotlin_stdlib_sources_dependency_url="https://maven.pkg.jetbrains.space/kotlin/p/kotlin/kotlin-ide-plugin-dependencies/org/jetbrains/kotlin/kotlin-stdlib/1.5.10-release-894/kotlin-stdlib-1.5.10-release-894-sources.jar"
curl "$kotlin_stdlib_sources_dependency_url" -L -o "$lib_directory/$kotlin_stdlib_sources_filename"

# Download kotlinc
kotlin_compiler_name="kotlin-compiler"
kotlin_compiler_version="1.5.10"
kotlinc_url="https://github.com/JetBrains/kotlin/releases/download/v$kotlin_compiler_version/$kotlin_compiler_name-$kotlin_compiler_version.zip"
curl "$kotlinc_url" -L -o "$download_directory/$kotlin_compiler_name"
unzip "$download_directory/$kotlin_compiler_name" -d "$lib_directory"

rm -r "$dependencies_directory"
rm -r "$download_directory"
