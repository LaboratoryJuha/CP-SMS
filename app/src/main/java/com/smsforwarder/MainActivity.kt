package com.smsforwarder

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

/**
 * SMS 자동 전달 앱의 메인 액티비티
 * 수신자, 키워드 필터, 추가 메시지를 설정할 수 있습니다.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var prefs: PreferenceManager
    
    // UI 컴포넌트
    private lateinit var switchForwarding: Switch
    private lateinit var tvRecipients: TextView
    private lateinit var btnAddRecipient: Button
    private lateinit var tvKeywords: TextView
    private lateinit var btnAddKeyword: Button
    private lateinit var etAdditionalMessage: EditText
    private lateinit var btnSaveAdditionalMessage: Button
    private lateinit var btnClearSettings: Button

    companion object {
        private const val PERMISSION_REQUEST_CODE = 100
        private val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.SEND_SMS,
            Manifest.permission.READ_SMS
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        prefs = PreferenceManager.getInstance(this)
        
        initViews()
        setupListeners()
        updateUI()
        
        // 권한 확인 및 요청
        checkAndRequestPermissions()
    }

    private fun initViews() {
        switchForwarding = findViewById(R.id.switchForwarding)
        tvRecipients = findViewById(R.id.tvRecipients)
        btnAddRecipient = findViewById(R.id.btnAddRecipient)
        tvKeywords = findViewById(R.id.tvKeywords)
        btnAddKeyword = findViewById(R.id.btnAddKeyword)
        etAdditionalMessage = findViewById(R.id.etAdditionalMessage)
        btnSaveAdditionalMessage = findViewById(R.id.btnSaveAdditionalMessage)
        btnClearSettings = findViewById(R.id.btnClearSettings)
    }

    private fun setupListeners() {
        // 자동 전달 스위치
        switchForwarding.setOnCheckedChangeListener { _, isChecked ->
            prefs.setForwardingEnabled(isChecked)
            Toast.makeText(
                this,
                if (isChecked) "자동 전달 활성화됨" else "자동 전달 비활성화됨",
                Toast.LENGTH_SHORT
            ).show()
        }

        // 수신자 추가
        btnAddRecipient.setOnClickListener {
            showAddRecipientDialog()
        }

        // 수신자 목록 클릭 (삭제용)
        tvRecipients.setOnClickListener {
            showRemoveRecipientDialog()
        }

        // 키워드 추가
        btnAddKeyword.setOnClickListener {
            showAddKeywordDialog()
        }

        // 키워드 목록 클릭 (삭제용)
        tvKeywords.setOnClickListener {
            showRemoveKeywordDialog()
        }

        // 추가 메시지 저장
        btnSaveAdditionalMessage.setOnClickListener {
            val message = etAdditionalMessage.text.toString()
            prefs.setAdditionalMessage(message)
            Toast.makeText(this, "추가 메시지가 저장되었습니다", Toast.LENGTH_SHORT).show()
        }

        // 모든 설정 초기화
        btnClearSettings.setOnClickListener {
            showClearSettingsDialog()
        }
    }

    private fun updateUI() {
        // 자동 전달 스위치 상태
        switchForwarding.isChecked = prefs.isForwardingEnabled()

        // 수신자 목록
        val recipients = prefs.getRecipients()
        tvRecipients.text = if (recipients.isEmpty()) {
            "설정된 수신자가 없습니다"
        } else {
            recipients.joinToString("\n")
        }

        // 키워드 목록
        val keywords = prefs.getKeywords()
        tvKeywords.text = if (keywords.isEmpty()) {
            "모든 메시지 전달 (키워드 필터 없음)"
        } else {
            keywords.joinToString(", ")
        }

        // 추가 메시지
        etAdditionalMessage.setText(prefs.getAdditionalMessage())
    }

    private fun showAddRecipientDialog() {
        val input = EditText(this)
        input.hint = "전화번호 (예: 010-1234-5678)"

        AlertDialog.Builder(this)
            .setTitle("수신자 추가")
            .setMessage("문자를 받을 전화번호를 입력하세요")
            .setView(input)
            .setPositiveButton("추가") { _, _ ->
                val phoneNumber = input.text.toString().trim()
                if (phoneNumber.isNotEmpty()) {
                    prefs.addRecipient(phoneNumber)
                    updateUI()
                    Toast.makeText(this, "수신자가 추가되었습니다", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun showRemoveRecipientDialog() {
        val recipients = prefs.getRecipients()
        if (recipients.isEmpty()) {
            Toast.makeText(this, "삭제할 수신자가 없습니다", Toast.LENGTH_SHORT).show()
            return
        }

        AlertDialog.Builder(this)
            .setTitle("수신자 삭제")
            .setItems(recipients.toTypedArray()) { _, which ->
                val removed = recipients[which]
                prefs.removeRecipient(removed)
                updateUI()
                Toast.makeText(this, "수신자가 삭제되었습니다", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun showAddKeywordDialog() {
        val input = EditText(this)
        input.hint = "키워드 (예: 인증번호)"

        AlertDialog.Builder(this)
            .setTitle("키워드 추가")
            .setMessage("전달할 메시지에 포함되어야 할 키워드를 입력하세요\n(키워드가 없으면 모든 메시지를 전달합니다)")
            .setView(input)
            .setPositiveButton("추가") { _, _ ->
                val keyword = input.text.toString().trim()
                if (keyword.isNotEmpty()) {
                    prefs.addKeyword(keyword)
                    updateUI()
                    Toast.makeText(this, "키워드가 추가되었습니다", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun showRemoveKeywordDialog() {
        val keywords = prefs.getKeywords()
        if (keywords.isEmpty()) {
            Toast.makeText(this, "삭제할 키워드가 없습니다", Toast.LENGTH_SHORT).show()
            return
        }

        AlertDialog.Builder(this)
            .setTitle("키워드 삭제")
            .setItems(keywords.toTypedArray()) { _, which ->
                val removed = keywords[which]
                prefs.removeKeyword(removed)
                updateUI()
                Toast.makeText(this, "키워드가 삭제되었습니다", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun showClearSettingsDialog() {
        AlertDialog.Builder(this)
            .setTitle("설정 초기화")
            .setMessage("모든 설정을 초기화하시겠습니까?")
            .setPositiveButton("초기화") { _, _ ->
                prefs.clearAll()
                updateUI()
                Toast.makeText(this, "모든 설정이 초기화되었습니다", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun checkAndRequestPermissions() {
        val permissionsToRequest = REQUIRED_PERMISSIONS.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                permissionsToRequest.toTypedArray(),
                PERMISSION_REQUEST_CODE
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        if (requestCode == PERMISSION_REQUEST_CODE) {
            val allGranted = grantResults.all { it == PackageManager.PERMISSION_GRANTED }
            
            if (allGranted) {
                Toast.makeText(this, "모든 권한이 허용되었습니다", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(
                    this,
                    "일부 권한이 거부되었습니다. 앱이 정상적으로 작동하지 않을 수 있습니다.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}
