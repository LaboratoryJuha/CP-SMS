package com.smsforwarder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Telephony
import android.telephony.SmsMessage
import android.util.Log

/**
 * SMS 수신을 감지하는 BroadcastReceiver
 * 문자 메시지가 도착하면 자동으로 설정된 조건에 따라 전달합니다.
 */
class SmsReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "SmsReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "SMS 수신 감지됨")

        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            val bundle: Bundle? = intent.extras

            if (bundle != null) {
                try {
                    // SMS 메시지 추출
                    val pdus = bundle.get("pdus") as Array<*>?
                    val format = bundle.getString("format")

                    if (pdus != null) {
                        val messages = arrayOfNulls<SmsMessage>(pdus.size)
                        val messageBody = StringBuilder()
                        var senderNumber = ""

                        for (i in pdus.indices) {
                            messages[i] = SmsMessage.createFromPdu(pdus[i] as ByteArray, format)
                            senderNumber = messages[i]?.displayOriginatingAddress ?: ""
                            messageBody.append(messages[i]?.messageBody ?: "")
                        }

                        val fullMessage = messageBody.toString()
                        Log.d(TAG, "발신자: $senderNumber")
                        Log.d(TAG, "메시지: $fullMessage")

                        // PreferenceManager를 통해 설정 확인
                        val prefs = PreferenceManager.getInstance(context)
                        
                        // 자동 전달 기능이 활성화되어 있는지 확인
                        if (prefs.isForwardingEnabled()) {
                            // 발신자 필터 확인
                            val senderFilters = prefs.getSenderFilters()
                            val senderMatches = if (senderFilters.isEmpty()) {
                                // 발신자 필터가 없으면 모든 발신자 허용
                                true
                            } else {
                                // 발신자 필터가 있으면 발신자가 필터 목록에 있는지 확인
                                senderFilters.any { filter ->
                                    senderNumber.contains(filter, ignoreCase = true)
                                }
                            }
                            
                            if (!senderMatches) {
                                Log.d(TAG, "발신자가 필터와 일치하지 않아 전달하지 않음: $senderNumber")
                                return
                            }
                            
                            val keywords = prefs.getKeywords()
                            val shouldForward = if (keywords.isEmpty()) {
                                // 키워드가 없으면 모든 메시지 전달
                                true
                            } else {
                                // 키워드가 있으면 메시지에 키워드가 포함되어 있는지 확인
                                keywords.any { keyword ->
                                    fullMessage.contains(keyword, ignoreCase = true)
                                }
                            }

                            if (shouldForward) {
                                // SMS 전달
                                SmsForwarder.forwardSms(
                                    context = context,
                                    originalSender = senderNumber,
                                    originalMessage = fullMessage
                                )
                            } else {
                                Log.d(TAG, "키워드가 일치하지 않아 전달하지 않음")
                            }
                        } else {
                            Log.d(TAG, "자동 전달 기능이 비활성화되어 있음")
                        }

                    }
                } catch (e: Exception) {
                    Log.e(TAG, "SMS 처리 중 오류 발생", e)
                }
            }
        }
    }
}
