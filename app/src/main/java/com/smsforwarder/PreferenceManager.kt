package com.smsforwarder

import android.content.Context
import android.content.SharedPreferences

/**
 * SharedPreferences를 사용하여 앱 설정을 관리하는 클래스
 */
class PreferenceManager private constructor(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "SmsForwarderPrefs"
        
        // 설정 키
        private const val KEY_FORWARDING_ENABLED = "forwarding_enabled"
        private const val KEY_RECIPIENTS = "recipients"
        private const val KEY_KEYWORDS = "keywords"
        private const val KEY_ADDITIONAL_MESSAGE = "additional_message"

        // 구분자
        private const val DELIMITER = "|||"

        @Volatile
        private var instance: PreferenceManager? = null

        fun getInstance(context: Context): PreferenceManager {
            return instance ?: synchronized(this) {
                instance ?: PreferenceManager(context.applicationContext).also { instance = it }
            }
        }
    }

    /**
     * 자동 전달 기능 활성화 여부
     */
    fun isForwardingEnabled(): Boolean {
        return prefs.getBoolean(KEY_FORWARDING_ENABLED, false)
    }

    fun setForwardingEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_FORWARDING_ENABLED, enabled).apply()
    }

    /**
     * 수신자 목록 관리
     */
    fun getRecipients(): List<String> {
        val recipientsString = prefs.getString(KEY_RECIPIENTS, "") ?: ""
        return if (recipientsString.isEmpty()) {
            emptyList()
        } else {
            recipientsString.split(DELIMITER).filter { it.isNotEmpty() }
        }
    }

    fun setRecipients(recipients: List<String>) {
        val recipientsString = recipients.joinToString(DELIMITER)
        prefs.edit().putString(KEY_RECIPIENTS, recipientsString).apply()
    }

    fun addRecipient(phoneNumber: String) {
        val currentRecipients = getRecipients().toMutableList()
        if (!currentRecipients.contains(phoneNumber)) {
            currentRecipients.add(phoneNumber)
            setRecipients(currentRecipients)
        }
    }

    fun removeRecipient(phoneNumber: String) {
        val currentRecipients = getRecipients().toMutableList()
        currentRecipients.remove(phoneNumber)
        setRecipients(currentRecipients)
    }

    /**
     * 키워드 필터 관리
     * 비어있으면 모든 메시지를 전달, 키워드가 있으면 해당 키워드가 포함된 메시지만 전달
     */
    fun getKeywords(): List<String> {
        val keywordsString = prefs.getString(KEY_KEYWORDS, "") ?: ""
        return if (keywordsString.isEmpty()) {
            emptyList()
        } else {
            keywordsString.split(DELIMITER).filter { it.isNotEmpty() }
        }
    }

    fun setKeywords(keywords: List<String>) {
        val keywordsString = keywords.joinToString(DELIMITER)
        prefs.edit().putString(KEY_KEYWORDS, keywordsString).apply()
    }

    fun addKeyword(keyword: String) {
        val currentKeywords = getKeywords().toMutableList()
        if (!currentKeywords.contains(keyword)) {
            currentKeywords.add(keyword)
            setKeywords(currentKeywords)
        }
    }

    fun removeKeyword(keyword: String) {
        val currentKeywords = getKeywords().toMutableList()
        currentKeywords.remove(keyword)
        setKeywords(currentKeywords)
    }

    /**
     * 전달 메시지에 추가할 내용
     */
    fun getAdditionalMessage(): String {
        return prefs.getString(KEY_ADDITIONAL_MESSAGE, "") ?: ""
    }

    fun setAdditionalMessage(message: String) {
        prefs.edit().putString(KEY_ADDITIONAL_MESSAGE, message).apply()
    }

    /**
     * 모든 설정 초기화
     */
    fun clearAll() {
        prefs.edit().clear().apply()
    }
}
