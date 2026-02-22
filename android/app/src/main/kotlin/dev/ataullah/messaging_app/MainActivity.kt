package dev.ataullah.messaging_app

import android.app.role.RoleManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Telephony
import androidx.annotation.NonNull
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel

class MainActivity : FlutterActivity() {
    private val CHANNEL = "dev.ataullah.messaging_app/role"
    private val ROLE_REQUEST_CODE = 123

    private var pendingResult: MethodChannel.Result? = null

    override fun configureFlutterEngine(@NonNull flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, CHANNEL).setMethodCallHandler { call, result ->
            when (call.method) {
                "requestDefaultSmsRole" -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        val roleManager = getSystemService(Context.ROLE_SERVICE) as RoleManager
                        if (roleManager.isRoleAvailable(RoleManager.ROLE_SMS) && !roleManager.isRoleHeld(RoleManager.ROLE_SMS)) {
                            val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_SMS)
                            pendingResult = result
                            startActivityForResult(intent, ROLE_REQUEST_CODE)
                        } else {
                            result.success(roleManager.isRoleHeld(RoleManager.ROLE_SMS))
                        }
                    } else {
                        val intent = Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT)
                        intent.putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, packageName)
                        pendingResult = result
                        startActivityForResult(intent, ROLE_REQUEST_CODE)
                    }
                }
                "isDefaultSmsApp" -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        val roleManager = getSystemService(Context.ROLE_SERVICE) as RoleManager
                        result.success(roleManager.isRoleHeld(RoleManager.ROLE_SMS))
                    } else {
                        result.success(Telephony.Sms.getDefaultSmsPackage(this) == packageName)
                    }
                }
                "getSimCards" -> {
                    val sm = getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as android.telephony.SubscriptionManager
                    if (checkSelfPermission(android.Manifest.permission.READ_PHONE_STATE) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                        val activeSubs = sm.activeSubscriptionInfoList
                        val defaultSmsSubId = android.telephony.SubscriptionManager.getDefaultSmsSubscriptionId()
                        val resultList = activeSubs?.map { 
                            mapOf(
                              "id" to it.subscriptionId, 
                              "name" to it.displayName.toString(), 
                              "isDefault" to (it.subscriptionId == defaultSmsSubId),
                              "slot" to it.simSlotIndex
                            ) 
                        } ?: listOf<Map<String, Any>>()
                        result.success(resultList)
                    } else {
                        result.error("PERMISSION_DENIED", "Requires READ_PHONE_STATE", null)
                    }
                }
                "sendSmsViaSim" -> {
                    val address = call.argument<String>("address")
                    val message = call.argument<String>("message")
                    val subId = call.argument<Int>("simId")
                    if (address != null && message != null) {
                        val smsManager = if (subId != null && subId != android.telephony.SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
                            android.telephony.SmsManager.getSmsManagerForSubscriptionId(subId)
                        } else {
                            android.telephony.SmsManager.getDefault()
                        }
                        
                        // Send the actual SMS natively via the correct SIM
                        smsManager.sendTextMessage(address, null, message, null, null)

                        // If default SMS app, we must write to the Android outbox manually
                        try {
                            val values = ContentValues().apply {
                                put(Telephony.Sms.ADDRESS, address)
                                put(Telephony.Sms.BODY, message)
                                put(Telephony.Sms.DATE, System.currentTimeMillis())
                                put(Telephony.Sms.READ, 1)
                                put(Telephony.Sms.TYPE, Telephony.Sms.MESSAGE_TYPE_SENT)
                                if (subId != null) {
                                    put(Telephony.Sms.SUBSCRIPTION_ID, subId)
                                }
                            }
                            contentResolver.insert(Telephony.Sms.Sent.CONTENT_URI, values)
                        } catch (e: Exception) {
                            // Non-fatal, perhaps permission denied or not default yet
                        }

                        result.success(true)
                    } else {
                        result.error("INVALID_ARGS", "Address and message are required", null)
                    }
                }
                else -> result.notImplemented()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == ROLE_REQUEST_CODE) {
            val isDefault = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val roleManager = getSystemService(Context.ROLE_SERVICE) as RoleManager
                roleManager.isRoleHeld(RoleManager.ROLE_SMS)
            } else {
                Telephony.Sms.getDefaultSmsPackage(this) == packageName
            }
            pendingResult?.success(isDefault)
            pendingResult = null
        }
    }
}
