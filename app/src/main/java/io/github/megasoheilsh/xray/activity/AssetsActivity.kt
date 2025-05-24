package io.github.megasoheilsh.xray.activity

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import io.github.megasoheilsh.xray.R
import io.github.megasoheilsh.xray.Settings
import io.github.megasoheilsh.xray.databinding.ActivityAssetsBinding
import io.github.megasoheilsh.xray.helper.DownloadHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date

class AssetsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAssetsBinding
    private var downloading: Boolean = false

    private val settings by lazy { Settings(applicationContext) }
    private val geoIpLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { writeToFile(it, geoIpFile()) }
    private val geoSiteLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { writeToFile(it, geoSiteFile()) }

    private fun geoIpFile(): File = File(applicationContext.filesDir, "geoip.dat")
    private fun geoSiteFile(): File = File(applicationContext.filesDir, "geosite.dat")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val mimeType = "application/octet-stream"
        title = getString(R.string.assets)
        binding = ActivityAssetsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        setAssetStatus()

        // GeoIP
        binding.geoIpDownload.setOnClickListener {
            download(settings.geoIpAddress, geoIpFile(), binding.geoIpSetup, binding.geoIpProgress)
        }
        binding.geoIpFile.setOnClickListener { geoIpLauncher.launch(mimeType) }
        binding.geoIpDelete.setOnClickListener { delete(geoIpFile()) }

        // GeoSite
        binding.geoSiteDownload.setOnClickListener {
            download(settings.geoSiteAddress, geoSiteFile(), binding.geoSiteSetup, binding.geoSiteProgress)
        }
        binding.geoSiteFile.setOnClickListener { geoSiteLauncher.launch(mimeType) }
        binding.geoSiteDelete.setOnClickListener { delete(geoSiteFile()) }
    }

    @SuppressLint("SimpleDateFormat")
    private fun getFileDate(file: File): String {
        return if (file.exists()) {
            val date = Date(file.lastModified())
            SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(date)
        } else {
            getString(R.string.noValue)
        }
    }

    private fun setAssetStatus() {
        val geoIp = geoIpFile()
        val geoIpExists = geoIp.exists()
        binding.geoIpDate.text = getFileDate(geoIp)
        binding.geoIpSetup.visibility = if (geoIpExists) View.GONE else View.VISIBLE
        binding.geoIpInstalled.visibility = if (geoIpExists) View.VISIBLE else View.GONE
        binding.geoIpProgress.visibility = View.GONE

        val geoSite = geoSiteFile()
        val geoSiteExists = geoSite.exists()
        binding.geoSiteDate.text = getFileDate(geoSite)
        binding.geoSiteSetup.visibility = if (geoSiteExists) View.GONE else View.VISIBLE
        binding.geoSiteInstalled.visibility = if (geoSiteExists) View.VISIBLE else View.GONE
        binding.geoSiteProgress.visibility = View.GONE
    }

    private fun download(url: String, file: File, setup: LinearLayout, progressBar: ProgressBar) {
        if (downloading) {
            Toast.makeText(applicationContext, "Another download is running, please wait", Toast.LENGTH_SHORT).show()
            return
        }

        setup.visibility = View.GONE
        progressBar.visibility = View.VISIBLE
        progressBar.progress = 0

        downloading = true
        DownloadHelper(lifecycleScope, url, file, object : DownloadHelper.DownloadListener {
            override fun onProgress(progress: Int) {
                progressBar.progress = progress
            }

            override fun onError(exception: Exception) {
                downloading = false
                Toast.makeText(applicationContext, exception.message, Toast.LENGTH_SHORT).show()
                setAssetStatus()
            }

            override fun onComplete() {
                downloading = false
                setAssetStatus()
            }
        }).start()
    }

    private fun writeToFile(uri: Uri?, file: File) {
        if (uri == null) return
        lifecycleScope.launch {
            contentResolver.openInputStream(uri).use { input ->
                FileOutputStream(file).use { output ->
                    input?.copyTo(output)
                }
            }
            withContext(Dispatchers.Main) {
                setAssetStatus()
            }
        }
    }

    private fun delete(file: File) {
        lifecycleScope.launch {
            file.delete()
            withContext(Dispatchers.Main) {
                setAssetStatus()
            }
        }
    }

}
