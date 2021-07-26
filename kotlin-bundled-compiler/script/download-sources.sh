#!/bin/bash

lib_directory="../lib"
download_directory="$lib_directory/download"
dependencies_directory="$lib_directory/dependencies"
dependencies=(
  "annotations.jar"
  "idea.jar"
  "intellij-core.jar"
  "intellij-core-analysis-deprecated.jar"
  "platform-api.jar"
  "platform-impl.jar"
  "platform-util-ui.jar"
  "trove4j.jar"
  "util.jar"
)

# Download sources
while read -r url; do
  wget "$url" -P "$download_directory"
done < urls.txt

# Unzip all archives
for file in "$download_directory"/*; do
    if [[ -f $file ]]; then
      unzip "$file" -d "$dependencies_directory"
    fi
done

# Copy needed dependencies
for dependency in "${dependencies[@]}"; do
  find "$dependencies_directory" -name "$dependency" -type f -exec cp {} "$lib_directory" \;
done

rmdir "$dependencies_directory"
