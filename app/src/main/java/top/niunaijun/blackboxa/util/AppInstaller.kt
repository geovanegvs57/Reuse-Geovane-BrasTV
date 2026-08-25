package top.niunaijun.blackboxa.util

import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import top.niunaijun.blackbox.BlackBoxCore
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class AppInstaller(private val context: Context) {

    private val TAG = "🔥RESET_TRIAL"
    private val PREFS_NAME = "trial_control"
    private val KEY_FIRST_OPEN = "first_open_"
    private val TRIAL_DAYS = 2

    fun launchApk(packageName: String, userId: Int): Boolean {
        Log.d(TAG, "🚀 Iniciando $packageName com userId=$userId")

        if (isTrialExpired(packageName)) {
            Log.d(TAG, "⏰ Trial expirado! Resetando dados do clone...")
            resetTrial(packageName, userId)
            saveFirstOpenDate(packageName)
        } else {
            Log.d(TAG, "✅ Trial ainda válido para $packageName")
        }

        return try {
            BlackBoxCore.get().launchApk(packageName, userId)
            Log.d(TAG, "✅ $packageName lançado com sucesso via BlackBox")
            Toast.makeText(context, "✅ $packageName aberto!", Toast.LENGTH_SHORT).show()
            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro ao lançar $packageName: ${e.message}")
            Toast.makeText(context, "❌ Erro: ${e.message}", Toast.LENGTH_LONG).show()
            false
        }
    }

    fun resetAppData(packageName: String, userId: Int) {
        resetTrial(packageName, userId)
    }

    private fun isTrialExpired(packageName: String): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val key = KEY_FIRST_OPEN + packageName
        val firstOpenStr = prefs.getString(key, null)

        if (firstOpenStr == null) {
            saveFirstOpenDate(packageName)
            return false
        }

        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val firstOpen = sdf.parse(firstOpenStr) ?: return false
        val hoje = Date()
        val diff = (hoje.time - firstOpen.time) / (1000 * 60 * 60 * 24)

        Log.d(TAG, "📅 Dias desde a primeira abertura: $diff dias (limite: $TRIAL_DAYS)")
        return diff >= TRIAL_DAYS
    }

    private fun saveFirstOpenDate(packageName: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val hoje = sdf.format(Date())
        prefs.edit().putString(KEY_FIRST_OPEN + packageName, hoje).apply()
        Log.d(TAG, "💾 Data salva: $hoje para $packageName")
    }

    private fun resetTrial(packageName: String, userId: Int) {
        try {
            Log.d(TAG, "🔥 resetTrial chamado para $packageName (userId=$userId)")

            val baseDir = context.filesDir
            val cacheDir = context.cacheDir
            val externalFilesDir = context.getExternalFilesDir(null)
            val externalCacheDir = context.externalCacheDir

            val caminhos = mutableListOf(
                File(baseDir, "users/$userId/apps/$packageName"),
                File(baseDir, "virtual/$userId/$packageName"),
                File(baseDir, "apps/$packageName"),
                File(cacheDir, "users/$userId/$packageName"),
                File(cacheDir, "virtual/$userId/$packageName"),
                File(baseDir, "users/$userId"),
                File(baseDir, "virtual/$userId")
            )

            externalFilesDir?.let {
                caminhos.add(File(it, "users/$userId/apps/$packageName"))
                caminhos.add(File(it, "virtual/$userId/$packageName"))
            }
            externalCacheDir?.let {
                caminhos.add(File(it, "users/$userId/apps/$packageName"))
                caminhos.add(File(it, "virtual/$userId/$packageName"))
            }

            for (caminho in caminhos) {
                if (caminho.exists()) {
                    Log.d(TAG, "🗑️ Deletando: ${caminho.absolutePath}")
                    deleteRecursive(caminho)
                }
            }

            val prefNames = listOf(
                "trial", "demo", "premium", "vip", "subscription",
                "license", "activation", "user_data", "prefs", "settings"
            )
            for (prefName in prefNames) {
                try {
                    context.getSharedPreferences(prefName, Context.MODE_PRIVATE).edit().clear().apply()
                    Log.d(TAG, "✅ SP limpa: $prefName")
                } catch (_: Exception) {}
            }

            Log.d(TAG, "🔥 RESET FINALIZADO PARA $packageName")
            Toast.makeText(context, "✅ Reset executado para $packageName", Toast.LENGTH_SHORT).show()

        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro no reset: ${e.message}")
            Toast.makeText(context, "❌ Erro no reset: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun deleteRecursive(file: File?) {
        if (file == null || !file.exists()) return
        try {
            if (file.isDirectory) {
                file.listFiles()?.forEach { deleteRecursive(it) }
            }
            file.delete()
        } catch (_: Exception) {}
    }

    fun installApk(apkPath: String): Boolean {
        val apkFile = File(apkPath)
        if (!apkFile.exists()) {
            Log.e(TAG, "❌ APK não encontrado: $apkPath")
            return false
        }
        try {
            val uri = FileProvider.getUriForFile(
                context,
                context.packageName + ".provider",
                apkFile
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            return true
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro ao instalar: ${e.message}")
            return false
        }
    }
}