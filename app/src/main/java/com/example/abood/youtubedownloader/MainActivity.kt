package com.example.abood.youtubedownloader

import android.Manifest
import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.support.v4.app.ActivityCompat
import android.support.v7.app.AppCompatActivity
import com.example.abood.youtubedownloader.TerminalSessionMod.processId
import com.example.abood.youtubedownloader.TerminalSessionMod.wrapFileDescriptor
import com.termux.app.BackgroundJob
import com.termux.app.TermuxInstaller
import com.termux.app.TermuxService
import com.termux.terminal.JNI
import kotlinx.coroutines.experimental.asCoroutineDispatcher
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import org.jetbrains.anko.toast
import java.util.concurrent.Executor



object MainDispatcher {

    fun create() = Executor {
        Handler(Looper.getMainLooper()).post { it.run() }
    }.asCoroutineDispatcher()

}

val FILES_PATH = "/data/data/com.example.abood.youtubedownloader/files"
val PREFIX_PATH = FILES_PATH + "/usr"
val LIB_PATH = PREFIX_PATH + "/lib"
val HOME_PATH = FILES_PATH + "/home"
val LD_LIBRARY_PATH = "LD_LIBRARY_PATH=" + LIB_PATH

val SHELL_PATH = FILES_PATH + "/usr/bin/login"


val TERM_VARIABLE = "TERM=xterm-256color"
val HOME_VARIABLE = "HOME=" + HOME_PATH
val PREFIX_VARIABLE = "PREFIX=" + PREFIX_PATH
val PSI_VARIABLE = "PS1=\$"
val LD_LIBRARY_VARIABLE = "LD_LIBRARY_PATH=" + LIB_PATH
val LANG_VARIABLE = "LANG=en_US.UTF-8"
val PATH_VARIABLE = "PATH=/data/data/com.example.abood.youtubedownloader/files/usr/bin:/data/data/com.example.abood.youtubedownloader/files/usr/bin/applets"
val PWD_VARIABLE = "PWD=" + HOME_PATH
val ANDROID_ROOT_VARIABLE= "ANDROID_ROOT=/system"
val ANDROID_DATA_VARIABLE = "ANDROID_DATA=/data"
val EXTERNAL_STORAGE_VARIABLE = "EXTERNAL_STORAGE=/sdcard"
val TEMP_DIR_PATH = "TMPDIR=" + PREFIX_PATH + "/tmp"

val arg = "-login"
val env = arrayOf(TERM_VARIABLE
        , HOME_VARIABLE
        , PREFIX_VARIABLE
        , PSI_VARIABLE
        , LD_LIBRARY_VARIABLE
        , LANG_VARIABLE
        , PATH_VARIABLE
        , PWD_VARIABLE
        , ANDROID_ROOT_VARIABLE
        , ANDROID_DATA_VARIABLE
        , EXTERNAL_STORAGE_VARIABLE
        , TEMP_DIR_PATH)

class MainActivity : AppCompatActivity() , AnkoLogger{




    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        for (en in env){
            info { en }
        }

        val descID = JNI.createSubprocess(SHELL_PATH, HOME_PATH, arrayOf(arg), env, processId,2,2)
        val fileDescriptor = wrapFileDescriptor(descID)

        val threadUtil = ThreadUtil(fileDescriptor, processId[0])


        threadUtil.write("ls\n")






    }

    private fun requestperms() {
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE), 0)
    }


}



