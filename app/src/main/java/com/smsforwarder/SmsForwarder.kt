package com.smsforwarder

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.telephony.SmsManager
import android.util.Log
import android.widget.Toast

/**
 * SMS를 자동으로 전달하는 클래스
 */
object SmsForwarder {

    private const val TAG = "SmsForwarder"

    /**
     * SMS를 설정된 수신자에게 전달
     * 
     * @param context 애플리케이션 컨텍스트
     * @param originalSender 원본 발신자 번호
     * @param originalMessage 원본 메시지 내용
     */
    fun forwardSms(context: Context, originalSender: String, originalMessage: String) {
        try {
            val prefs = PreferenceManager.getInstance(context)
            val recipients = prefs.getRecipients()
            val additionalMessage = prefs.getAdditionalMessage()

            if (recipients.isEmpty()) {
                Log.w(TAG, "수신자가 설정되지 않았습니다")
                return
            }

            // 전달할 메시지 구성
            val forwardMessage = buildForwardMessage(
                originalSender = originalSender,
                originalMessage = originalMessage,
                additionalMessage = additionalMessage
            )

            Log.d(TAG, "전달 메시지: $forwardMessage")

            // 각 수신자에게 메시지 전송
            val smsManager = context.getSystemService(SmsManager::class.java)
            
            recipients.forEach { recipient ->
                try {
                    sendSms(
                        smsManager = smsManager,
                        context = context,
                        phoneNumber = recipient,
                        message = forwardMessage
                    )
                    Log.d(TAG, "메시지 전송 성공: $recipient")
                } catch (e: Exception) {
                    Log.e(TAG, "메시지 전송 실패: $recipient", e)
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "SMS 전달 중 오류 발생", e)
        }
    }

    /**
     * 전달할 메시지 구성
     */
    private fun buildForwardMessage(
        originalSender: String,
        originalMessage: String,
        additionalMessage: String
    ): String {
        val builder = StringBuilder()
        
        // 추가 메시지가 있으면 먼저 추가
        if (additionalMessage.isNotEmpty()) {
            builder.append(additionalMessage)
            builder.append("\n\n")
        }
        
        // 원본 발신자 정보
        builder.append("[발신자: $originalSender]\n")
        
        // 원본 메시지
        builder.append(originalMessage)
        
        return builder.toString()
    }

    /**
     * SMS 전송 (긴 메시지 자동 분할 처리)
     */
    private fun sendSms(
        smsManager: SmsManager,
        context: Context,
        phoneNumber: String,
        message: String
    ) {
        val sentIntent = PendingIntent.getBroadcast(
            context,
            0,
            Intent("SMS_SENT"),
            PendingIntent.FLAG_IMMUTABLE
        )

        val deliveredIntent = PendingIntent.getBroadcast(
            context,
            0,
            Intent("SMS_DELIVERED"),
            PendingIntent.FLAG_IMMUTABLE
        )

        // 메시지가 160자를 초과하는 경우 자동으로 분할
        val parts = smsManager.divideMessage(message)
        
        if (parts.size > 1) {
            // 긴 메시지는 여러 부분으로 전송
            val sentIntents = ArrayList<PendingIntent>()
            val deliveredIntents = ArrayList<PendingIntent>()
            
            for (i in parts.indices) {
                sentIntents.add(sentIntent)
                deliveredIntents.add(deliveredIntent)
            }
            
            smsManager.sendMultipartTextMessage(
                phoneNumber,
                null,
                parts,
                sentIntents,
                deliveredIntents
            )
        } else {
            // 짧은 메시지는 한 번에 전송
            smsManager.sendTextMessage(
                phoneNumber,
                null,
                message,
                sentIntent,
                deliveredIntent
            )
        }
    }
}
