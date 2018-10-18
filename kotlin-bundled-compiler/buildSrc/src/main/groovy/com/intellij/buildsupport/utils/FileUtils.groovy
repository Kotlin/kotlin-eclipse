package com.intellij.buildsupport.utils


import groovy.transform.CompileStatic


@CompileStatic
class FileUtils {
    private FileUtils() {}


    static void cleanDir(File dir) {
        dir.deleteDir()
        dir.mkdirs()
    }

    static void cleanDirExceptSubDirName(File dir, String retainSubDirName) {
        if (dir.exists()) {
            dir.eachFile { File file ->
                if (file.isFile())
                    file.delete()
                else if (file.isDirectory()) {
                    if (file.name != retainSubDirName)
                        file.deleteDir()
                }
            }
        }

        dir.mkdirs()

        new File("$dir/$retainSubDirName").mkdirs()
    }
}
