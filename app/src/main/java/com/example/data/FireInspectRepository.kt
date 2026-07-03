package com.example.data

import android.util.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class FireInspectRepository(private val dao: FireInspectDao) {

    val allExtinguishersFlow: Flow<List<FireExtinguisher>> = dao.getAllExtinguishersFlow()
    val allLogsFlow: Flow<List<InspectionLog>> = dao.getAllLogsFlow()

    suspend fun getExtinguisherById(id: String): FireExtinguisher? {
        return dao.getExtinguisherById(id)
    }

    suspend fun insertExtinguisher(extinguisher: FireExtinguisher) {
        dao.insertExtinguisher(extinguisher)
    }

    suspend fun updateExtinguisher(extinguisher: FireExtinguisher) {
        dao.updateExtinguisher(extinguisher)
    }

    suspend fun insertLog(log: InspectionLog) {
        dao.insertLog(log)
    }

    suspend fun updateExtinguisherStatus(id: String, status: String, inspectedAt: Long, inspectedBy: String, remarks: String?) {
        dao.updateExtinguisherStatus(id, status, inspectedAt, inspectedBy, remarks)
    }

    suspend fun getUnsyncedLogs(): List<InspectionLog> {
        return dao.getUnsyncedLogs()
    }

    suspend fun markLogSynced(logId: Int) {
        dao.markLogSynced(logId)
    }

    suspend fun clearAllData() {
        dao.deleteAllExtinguishers()
        dao.deleteAllLogs()
    }

    /**
     * Seeds initial fire extinguisher records for CPRAM Surat Thani if database is empty.
     */
    suspend fun seedInitialDataIfEmpty() {
        val count = dao.getAllExtinguishersList().size
        if (count == 0) {
            Log.d("FireInspectRepository", "Database is empty. Seeding CPRAM Surat Thani layout data...")
            val initialList = listOf(
                FireExtinguisher(
                    id = "FE-CPRAM-01",
                    location = "อาคารสำนักงานและประชาสัมพันธ์ (ห้องรับรองลูกค้า)",
                    type = "CO2 (คาร์บอนไดออกไซด์)",
                    size = "10 lbs",
                    installationDate = "2025-01-10",
                    lastMaintenanceDate = "2026-01-10",
                    nextMaintenanceDate = "2026-07-10",
                    status = "PENDING",
                    layoutX = 0.22f,
                    layoutY = 0.28f
                ),
                FireExtinguisher(
                    id = "FE-CPRAM-02",
                    location = "ไลน์ผลิตเบเกอรี่และอาหารพร้อมทาน (โซนเปลี่ยนชุด 1)",
                    type = "เคมีแห้ง (Dry Chemical)",
                    size = "15 lbs",
                    installationDate = "2024-11-15",
                    lastMaintenanceDate = "2025-11-15",
                    nextMaintenanceDate = "2026-08-15",
                    status = "PENDING",
                    layoutX = 0.48f,
                    layoutY = 0.35f
                ),
                FireExtinguisher(
                    id = "FE-CPRAM-03",
                    location = "ห้องบรรจุระบบปลอดเชื้อ (Aseptic Packaging Room ชั้น 1)",
                    type = "ฮาโลตรอน (Halotron Eco-friendly)",
                    size = "10 lbs",
                    installationDate = "2025-02-01",
                    lastMaintenanceDate = "2025-02-01",
                    nextMaintenanceDate = "2026-09-01",
                    status = "PENDING",
                    layoutX = 0.65f,
                    layoutY = 0.42f
                ),
                FireExtinguisher(
                    id = "FE-CPRAM-04",
                    location = "คลังสินค้าแช่เย็นและทำความเย็น (Cold Storage Row B)",
                    type = "โฟม (Foam AFFF)",
                    size = "20 lbs",
                    installationDate = "2024-08-20",
                    lastMaintenanceDate = "2025-08-20",
                    nextMaintenanceDate = "2026-07-20",
                    status = "PENDING",
                    layoutX = 0.82f,
                    layoutY = 0.58f
                ),
                FireExtinguisher(
                    id = "FE-CPRAM-05",
                    location = "ห้องควบคุมระบบไฟฟ้าหลัก (MDB Room ข้างโรงอาหาร)",
                    type = "CO2 (คาร์บอนไดออกไซด์)",
                    size = "15 lbs",
                    installationDate = "2025-01-10",
                    lastMaintenanceDate = "2026-01-10",
                    nextMaintenanceDate = "2026-07-10",
                    status = "PENDING",
                    layoutX = 0.35f,
                    layoutY = 0.72f
                ),
                FireExtinguisher(
                    id = "FE-CPRAM-06",
                    location = "ฝ่ายวิศวกรรมบำรุงรักษาและพลังงานนิวเคลียร์/ไอน้ำ (Boiler Area)",
                    type = "เคมีแห้ง (Dry Chemical)",
                    size = "20 lbs",
                    installationDate = "2024-09-05",
                    lastMaintenanceDate = "2025-09-05",
                    nextMaintenanceDate = "2026-06-30", // Near maintenance date!
                    status = "PENDING",
                    layoutX = 0.15f,
                    layoutY = 0.83f
                )
            )
            dao.insertExtinguishers(initialList)
        }
    }
}
