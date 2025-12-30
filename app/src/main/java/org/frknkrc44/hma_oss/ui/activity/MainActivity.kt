package org.frknkrc44.hma_oss.ui.activity

import android.os.Bundle
import androidx.navigation.findNavController
import icu.nullptr.hidemyapplist.service.ServiceClient
import com.miui.video.BuildConfig
import com.miui.video.R
import com.miui.video.databinding.ActivityMainBinding

class MainActivity : BaseActivity() {
    var readyToKill: Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = this.findNavController(R.id.nav_host_fragment)
        return navController.navigateUp() || super.onSupportNavigateUp()
    }

    override fun onDestroy() {
        if (readyToKill) {
            ServiceClient.forceStop(BuildConfig.APPLICATION_ID)
        } else {
            readyToKill = true
        }

        super.onDestroy()
    }
}
