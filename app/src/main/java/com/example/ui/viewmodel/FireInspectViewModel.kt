package com.example.ui.viewmodel

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.api.LineNotifyService
import com.example.data.FireExtinguisher
import com.example.data.FireInspectRepository
import com.example.data.InspectionLog
import com.example.util.ReportExporter
import java.text.SimpleDateFormat
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class FireInspectViewModel(private val repository: FireInspectRepository) : ViewModel() {

    // 1. Session & Auth States
    private val _currentUser = MutableStateFlow<String?>(null)
    val currentUser: StateFlow<String?> = _currentUser.asStateFlow()

    private val _userRole = MutableStateFlow<String?>(null) // "ADMIN" or "INSPECTOR"
    val userRole: StateFlow<String?> = _userRole.asStateFlow()

    private val _show2FA = MutableStateFlow(false)
    val show2FA: StateFlow<Boolean> = _show2FA.asStateFlow()

    private val _verificationError = MutableStateFlow<String?>(null)
    val verificationError: StateFlow<String?> = _verificationError.asStateFlow()

    private val _lineToken = MutableStateFlow("MOCK_TOKEN_CPRAM_SURAT")
    val lineToken: StateFlow<String> = _lineToken.asStateFlow()

    private val _isOfflineMode = MutableStateFlow(false)
    val isOfflineMode: StateFlow<Boolean> = _isOfflineMode.asStateFlow()

    private val _syncMessage = MutableStateFlow<String?>(null)
    val syncMessage: StateFlow<String?> = _syncMessage.asStateFlow()

    // Status Alert for LINE Notify Status Delivery Display
    private val _lineAlertMessage = MutableStateFlow<String?>(null)
    val lineAlertMessage: StateFlow<String?> = _lineAlertMessage.asStateFlow()

    init {
        viewModelScope.launch {
            repository.seedInitialDataIfEmpty()
        }
    }

    // 2. Room flows for reactive UI updates
    val extinguishers: StateFlow<List<FireExtinguisher>> = repository.allExtinguishersFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val inspectionLogs: StateFlow<List<InspectionLog>> = repository.allLogsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Derived Statistics
    val checkStatistics = extinguishers.map { list ->
        val total = list.size
        val checked = list.count { it.status != "PENDING" }
        val normal = list.count { it.status == "NORMAL" }
        val problem = list.count { it.status == "PROBLEM" }
        val percentage = if (total > 0) (checked * 100 / total) else 0

        Stats(total, checked, normal, problem, percentage)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Stats(0, 0, 0, 0, 0))

    // Login Action
    fun login(username: String, passSecret: String): Boolean {
        // Simple secure credential verification for CPRAM context
        if (username.lowercase() == "admin" && passSecret == "cpram123") {
            _currentUser.value = "ผู้ดูแลระบบ CPRAM (Admin)"
            _userRole.value = "ADMIN"
            _show2FA.value = true // Require 2FA!
            _verificationError.value = null
            return true
        } else if (username.lowercase() == "inspector" && passSecret == "cpram123") {
            _currentUser.value = "นายพิศักดิ์ มีสติ (Inspector)"
            _userRole.value = "INSPECTOR"
            _show2FA.value = true // Require 2FA!
            _verificationError.value = null
            return true
        }
        return false
    }

    fun verifyTwoFactor(code: String): Boolean {
        if (code == "2612" || code == "1234") { // Mock standard authorization code
            _show2FA.value = false
            _verificationError.value = null
            return true
        }
        _verificationError.value = "รหัสความปลอดภัยไม่ถูกต้อง กรุณาระบุรหัสผ่าน OTP สำรองสำหรับการทำงาน"
        return false
    }

    fun logout() {
        _currentUser.value = null
        _userRole.value = null
        _show2FA.value = false
        _verificationError.value = null
    }

    fun updateLineToken(newToken: String) {
        _lineToken.value = newToken
    }

    fun toggleOfflineMode() {
        _isOfflineMode.value = !_isOfflineMode.value
        if (!_isOfflineMode.value) {
            // Trigger auto Sync back online!
            syncData()
        }
    }

    private fun syncData() {
        viewModelScope.launch {
            val unsynced = repository.getUnsyncedLogs()
            if (unsynced.isNotEmpty()) {
                _syncMessage.value = "กำลังเชื่อมต่อคลาวด์ ซิงค์สำเร็จ ${unsynced.size} รายการตรวจเช็ค..."
                for (log in unsynced) {
                    repository.markLogSynced(log.id)
                }
                // Simulate LINE delivery for synced problem logs!
                val problemLogs = unsynced.filter { it.overallStatus == "PROBLEM" }
                if (problemLogs.isNotEmpty()) {
                    triggerLineAlertForMultiple(problemLogs)
                }
            } else {
                _syncMessage.value = "ข้อมูลล่าสุดซิงค์กับฐานข้อมูลคลาวด์เรียบร้อยแล้ว"
            }
        }
    }

    fun clearSyncMessage() {
        _syncMessage.value = null
    }

    fun clearLineAlert() {
        _lineAlertMessage.value = null
    }

    // Insert new fire extinguisher (Admin panel)
    fun addNewExtinguisher(
        id: String,
        location: String,
        type: String,
        size: String,
        layoutX: Float,
        layoutY: Float
    ) {
        viewModelScope.launch {
            val newExt = FireExtinguisher(
                id = id,
                location = location,
                type = type,
                size = size,
                installationDate = SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date()),
                lastMaintenanceDate = SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date()),
                nextMaintenanceDate = SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(
                    java.util.Date(System.currentTimeMillis() + 180L * 24L * 60L * 60L * 1000L) // +6 months
                ),
                status = "PENDING",
                layoutX = layoutX,
                layoutY = layoutY
            )
            repository.insertExtinguisher(newExt)
        }
    }

    // Submit Inspection Inspection Checklist Check (Offline support + automatic LINE notify on issue detection)
    fun submitInspection(
        extId: String,
        pressureGaugeOk: Boolean,
        safetyPinOk: Boolean,
        nozzleOk: Boolean,
        physicalStatusOk: Boolean,
        remarks: String?,
        photoPath: String?
    ) {
        viewModelScope.launch {
            val isNormal = pressureGaugeOk && safetyPinOk && nozzleOk && physicalStatusOk
            val overallStatus = if (isNormal) "NORMAL" else "PROBLEM"
            val inspectorName = _currentUser.value ?: "Unknown Inspector"

            val ext = repository.getExtinguisherById(extId) ?: return@launch

            // Save log locally via Room database
            val log = InspectionLog(
                extinguisherId = extId,
                location = ext.location,
                inspectorName = inspectorName,
                timestamp = System.currentTimeMillis(),
                pressureGaugeOk = pressureGaugeOk,
                safetyPinOk = safetyPinOk,
                nozzleOk = nozzleOk,
                physicalStatusOk = physicalStatusOk,
                overallStatus = overallStatus,
                photoPath = photoPath ?: "placeholder_photo_uri",
                remarks = remarks,
                isSynced = !_isOfflineMode.value
            )
            repository.insertLog(log)

            // Update status of FireExtinguisher
            repository.updateExtinguisherStatus(
                id = extId,
                status = overallStatus,
                inspectedAt = System.currentTimeMillis(),
                inspectedBy = inspectorName,
                remarks = remarks
            )

            // If overallStatus has problems and NOT offline, immediately fire LINE notification alert
            if (overallStatus == "PROBLEM" && !_isOfflineMode.value) {
                triggerLineAlertForSingle(ext, pressureGaugeOk, safetyPinOk, nozzleOk, physicalStatusOk, remarks)
            }
        }
    }

    private fun triggerLineAlertForSingle(
        ext: FireExtinguisher,
        pOk: Boolean,
        sOk: Boolean,
        nOk: Boolean,
        phOk: Boolean,
        remarks: String?
    ) {
        viewModelScope.launch {
            val defects = mutableListOf<String>()
            if (!pOk) defects.add("เกจวัดแรงดันบกพร่อง (Pressure Gauge Fail)")
            if (!sOk) defects.add("ซีลสลักล็อกชำรุด (Safety Pin Broken)")
            if (!nOk) defects.add("สายฉีดชำรุดหรืออุดตัน (Nozzle Damaged)")
            if (!phOk) defects.add("ตัวถังขึ้นสนิม/มีรอยบุบชำรุด (Physical Body Damage)")

            val msg = """
                
                ⚠️ แจ้งเหตุถังดับเพลิงชำรุดทันที!
                [บริษัท ซีพีแรม จำกัด (สุราษฎร์ธานี)]
                -------------------------------------
                รหัสถัง: ${ext.id}
                ตำแหน่ง: ${ext.location}
                ประเภท: ${ext.type} (${ext.size})
                ผู้ตรวจเช็ค: ${_currentUser.value ?: "พนักงานซ่อมบำรุง"}
                
                ข้อบกพร่องที่ตรวจพบ:
                ${defects.joinToString("\n- ", prefix = "- ")}
                
                หมายเหตุเพิ่มเติม: ${remarks ?: "ไม่มี"}
                ความเร่งด่วน: สูงสุด 🔴
                -------------------------------------
                *โปรดนำวิศวกรความปลอดภัยเข้าหน้าพิกัดเพื่อแก้ไขทันที!*
            """.trimIndent()

            val success = LineNotifyService.sendNotification(_lineToken.value, msg)
            if (success) {
                _lineAlertMessage.value = "ส่งข้อความแจ้งเตือนปัญหาสนับสนุนเข้าสู่ LINE Notify สำหรับกลุ่มเฝ้าระวังภัย CPRAM สุราษฎร์ธานี สำเร็จแล้ว!"
            } else {
                _lineAlertMessage.value = "การสื่อสารล้มเหลว: ไม่สามารถส่งข้อความ LINE Alerts ได้เนื่องจากสัญญาณเครือข่ายบกพร่อง"
            }
        }
    }

    private fun triggerLineAlertForMultiple(logs: List<InspectionLog>) {
        viewModelScope.launch {
            val summary = logs.joinToString("\n") { "- ถัง ${it.extinguisherId} | ${it.location} (${it.remarks ?: "มีสิ่งบกพร่อง"})" }
            val msg = """
                
                ⚠️ ซิงค์ข้อมูล: ตรวจพบถังดับเพลิงชำรุด [หลังกลับมาออนไลน์]
                [บริษัท ซีพีแรม จำกัด (สุราษฎร์ธานี)]
                -------------------------------------
                รายการถังขัดข้องสะสมรอสัญญาน:
                $summary
                
                โปรดจัดการตรวจสอบบำรุงรักษาโดยด่วน!
            """.trimIndent()

            LineNotifyService.sendNotification(_lineToken.value, msg)
        }
    }

    // 4. Exporters
    fun exportPdfReport(context: Context): Uri? {
        val currentExts = extinguishers.value
        val currentLogs = inspectionLogs.value
        return ReportExporter.exportToPdf(context, currentExts, currentLogs)
    }

    fun exportCsvReport(context: Context): Uri? {
        val currentExts = extinguishers.value
        return ReportExporter.exportToCsv(context, currentExts)
    }
}

class FireInspectViewModelFactory(private val repository: FireInspectRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FireInspectViewModel::class.java)) {
            return FireInspectViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

data class Stats(
    val total: Int,
    val checked: Int,
    val normal: Int,
    val problem: Int,
    val percentage: Int
)
