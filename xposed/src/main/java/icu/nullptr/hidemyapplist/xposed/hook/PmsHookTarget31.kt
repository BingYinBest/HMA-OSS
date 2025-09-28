package icu.nullptr.hidemyapplist.xposed.hook

import android.os.Binder
import android.os.Build
import androidx.annotation.RequiresApi
import com.github.kyuubiran.ezxhelper.utils.findConstructor
import com.github.kyuubiran.ezxhelper.utils.findMethod
import com.github.kyuubiran.ezxhelper.utils.findMethodOrNull
import com.github.kyuubiran.ezxhelper.utils.hookBefore
import com.github.kyuubiran.ezxhelper.utils.paramCount
import icu.nullptr.hidemyapplist.common.Constants
import icu.nullptr.hidemyapplist.common.Constants.VENDING_PACKAGE_NAME
import icu.nullptr.hidemyapplist.common.Utils
import icu.nullptr.hidemyapplist.xposed.HMAService
import icu.nullptr.hidemyapplist.xposed.Utils4Xposed
import icu.nullptr.hidemyapplist.xposed.logD
import icu.nullptr.hidemyapplist.xposed.logE
import icu.nullptr.hidemyapplist.xposed.logI

@RequiresApi(Build.VERSION_CODES.S)
class PmsHookTarget31(service: HMAService) : PmsHookTargetBase(service) {

    companion object {
        private const val TAG = "PmsHookTarget31"
    }

    override val fakeSystemPackageInstallInfo: Any by lazy {
        findConstructor(
            "android.content.pm.InstallSourceInfo"
        ) {
            paramCount == 4
        }.newInstance(
            null,
            null,
            null,
            null,
        )
    }

    override val fakeUserPackageInstallInfo: Any by lazy {
        findConstructor(
            "android.content.pm.InstallSourceInfo"
        ) {
            paramCount == 4
        }.newInstance(
            VENDING_PACKAGE_NAME,
            psSigningInfo,
            VENDING_PACKAGE_NAME,
            VENDING_PACKAGE_NAME,
        )
    }

    override fun load() {
        logI(TAG, "Load hook")

        /*
        findMethodOrNull("com.android.server.pm.PackageManagerService") {
            name == "checkPermission"
        }?.hookBefore { param ->
            val targetApp = param.args[1] as String
            val callingApps = Utils4Xposed.getCallingApps(service)
            for (caller in callingApps) {
                if (service.shouldHide(caller, targetApp)) {
                    logD(TAG, "@checkPermission - PkgMgr: insecure query from $caller to $targetApp")
                    param.result = PackageManager.PERMISSION_DENIED
                    service.filterCount++
                    return@hookBefore
                }
            }
        }

        findMethodOrNull("com.android.server.pm.permission.PermissionManagerService", findSuper = true) {
            name == "checkPermission"
        }?.hookBefore { param ->
            val targetApp = param.args[0] as String
            val callingApps = Utils4Xposed.getCallingApps(service)
            for (caller in callingApps) {
                if (service.shouldHide(caller, targetApp)) {
                    logD(TAG, "@checkPermission - PermMgr: insecure query from $caller to $targetApp")
                    param.result = PackageManager.PERMISSION_DENIED
                    service.filterCount++
                    return@hookBefore
                }
            }
        }
         */

        findMethodOrNull("com.android.server.pm.PackageManagerService\$ComputerTracker") {
            name == "getPackageSetting"
        }?.hookBefore { param ->
            val targetApp = param.args[0] as String
            val callingUid = Binder.getCallingUid()
            val callingApps = Utils4Xposed.getCallingApps(service, callingUid)
            for (caller in callingApps) {
                if (service.shouldHide(caller, targetApp)) {
                    logD(TAG, "@getPackageSetting - Computer: insecure query from $caller to $targetApp")
                    param.result = null
                    service.filterCount++
                    return@hookBefore
                }
            }
        }?.let {
            hooks += it
        }

        findMethodOrNull("com.android.server.pm.PackageManagerService\$ComputerTracker") {
            name == "getPackageSettingInternal"
        }?.hookBefore { param ->
            val targetApp = param.args[0] as String
            val callingUid = param.args[1] as Int
            val callingApps = Utils4Xposed.getCallingApps(service, callingUid)
            for (caller in callingApps) {
                if (service.shouldHide(caller, targetApp)) {
                    logD(TAG, "@getPackageSettingInternal - Computer: insecure query from $caller to $targetApp")
                    param.result = null
                    service.filterCount++
                    return@hookBefore
                }
            }
        }?.let {
            hooks += it
        }

        hooks += findMethod("com.android.server.pm.AppsFilter") {
            name == "shouldFilterApplication"
        }.hookBefore { param ->
            runCatching {
                val callingUid = param.args[0] as Int
                if (callingUid == Constants.UID_SYSTEM) return@hookBefore
                val callingApps = Utils.binderLocalScope {
                    service.pms.getPackagesForUid(callingUid)
                } ?: return@hookBefore
                val targetApp = Utils4Xposed.getPackageNameFromPackageSettings(param.args[2])
                for (caller in callingApps) {
                    if (service.shouldHide(caller, targetApp)) {
                        param.result = true
                        service.filterCount++
                        val last = lastFilteredApp.getAndSet(caller)
                        if (last != caller) logI(TAG, "@shouldFilterApplication: query from $caller")
                        logD(TAG, "@shouldFilterApplication caller: $callingUid $caller, target: $targetApp")
                        return@hookBefore
                    }
                }
            }.onFailure {
                logE(TAG, "Fatal error occurred, disable hooks", it)
                unload()
            }
        }

       super.load()
    }
}
