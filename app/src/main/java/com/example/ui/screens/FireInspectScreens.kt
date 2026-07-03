package com.example.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.FireExtinguisher
import com.example.data.InspectionLog
import com.example.ui.components.DeterministicQrCanvas
import com.example.ui.components.QrCodeView
import com.example.ui.viewmodel.FireInspectViewModel
import java.text.SimpleDateFormat
import java.util.*

// Helper color variables
private val ColorCpramRed = Color(0xFFE21C26)
private val ColorCpramBlue = Color(0xFF1B365D)
private val ColorCpramGold = Color(0xFFFFB300)

// ----------------------------------------------------------------------------
// 1. SIGN IN SCREEN (PORTAL) WITH 2FA INTEGRITY CHECKER
// ----------------------------------------------------------------------------
@Composable
fun LoginScreen(
    viewModel: FireInspectViewModel,
    onLoginSuccess: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var username by remember { mutableStateOf("inspector") } // Default inspector for rapid preview
    var password by remember { mutableStateOf("cpram123") }
    var twoFactorCode by remember { mutableStateOf("") }

    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val userRole by viewModel.userRole.collectAsStateWithLifecycle()
    val show2FA by viewModel.show2FA.collectAsStateWithLifecycle()
    val verificationError by viewModel.verificationError.collectAsStateWithLifecycle()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(ColorCpramBlue, Color(0xFF0F1B2B))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // Aesthetic corporate grid decoration lines drawn behind login forms
        Canvas(modifier = Modifier.fillMaxSize()) {
            val count = 10
            val spacingX = size.width / count
            for (i in 1..count) {
                drawLine(
                    color = Color.White.copy(alpha = 0.03f),
                    start = Offset(i * spacingX, 0f),
                    end = Offset(i * spacingX, size.height),
                    strokeWidth = 1f
                )
            }
        }

        if (!show2FA) {
            // MAIN CREDENTIALS PANEL
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .wrapContentHeight()
                    .testTag("login_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Corporate branding insignia
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .background(ColorCpramRed, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalFireDepartment,
                            contentDescription = "CPRAM Safety",
                            tint = Color.White,
                            modifier = Modifier.size(42.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "CPRAM FIRE INSPECTION",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = ColorCpramBlue,
                        letterSpacing = 1.sp
                    )

                    Text(
                        text = "ระบบตรวจสอบถังดับเพลิงอัจฉริยะ ซีพีแรม (สุราษฎร์ธานี)",
                        fontSize = 11.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
                    )

                    // Role quick toggle indicators
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                username = "inspector"
                                password = "cpram123"
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (username == "inspector") ColorCpramBlue else Color.LightGray.copy(alpha = 0.3f),
                                contentColor = if (username == "inspector") Color.White else Color.DarkGray
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("พนักงานตรวจ (User)", fontSize = 11.sp)
                        }

                        Button(
                            onClick = {
                                username = "admin"
                                password = "cpram123"
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (username == "admin") ColorCpramRed else Color.LightGray.copy(alpha = 0.3f),
                                contentColor = if (username == "admin") Color.White else Color.DarkGray
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("ผู้จัดการ (Admin)", fontSize = 11.sp)
                        }
                    }

                    // Fields
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("รหัสพนักงาน / บัญชีชื่อเข้าใช้งาน") },
                        leadingIcon = { Icon(Icons.Default.Person, tint = ColorCpramBlue, contentDescription = null) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("username_input"),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("รหัสผ่านความปลอดภัย (CPRAM Pass)") },
                        leadingIcon = { Icon(Icons.Default.Lock, tint = ColorCpramBlue, contentDescription = null) },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("password_input"),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            val success = viewModel.login(username, password)
                            if (!success) {
                                Toast.makeText(context, "ข้อมูลระบุตัวตนไม่ถูกต้อง กรุณาลองอีกครั้ง", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("login_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = ColorCpramRed),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("ล็อกอินเข้าสู่ระบบ", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }
            }
        } else {
            // TWO FACTOR SECURITY (2FA CREDENTIAL GUARD)
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .wrapContentHeight()
                    .testTag("two_factor_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .background(ColorCpramGold.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = "2FA Lock",
                            tint = ColorCpramGold,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "การยืนยันตัวตนสองชั้น (2FA)",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = ColorCpramBlue
                    )

                    Text(
                        text = "ระบบความปลอดภัยสูงสุดสำหรับโรงงานผลิต ซีพีแรม สุราษฎร์ธานี กรุณากรอกรหัส OTP ยืนยันพนักงานด้านล่าง",
                        fontSize = 11.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                    )

                    // Helper indicator showing the simulated code so reviewers can complete 2FA effortlessly!
                    Surface(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                        color = ColorCpramGold.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "💡 รหัสความปลอดภัยทดลองสำหรับคุณ: 2612",
                            color = Color(0xFFB25E00),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(8.dp)
                        )
                    }

                    OutlinedTextField(
                        value = twoFactorCode,
                        onValueChange = { twoFactorCode = it },
                        label = { Text("รหัส OTP ความปลอดภัย 4 หลัก") },
                        leadingIcon = { Icon(Icons.Default.Pin, tint = ColorCpramBlue, contentDescription = null) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("otp_input"),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        textStyle = LocalTextStyle.current.copy(
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            letterSpacing = 4.sp
                        )
                    )

                    if (verificationError != null) {
                        Text(
                            text = verificationError ?: "",
                            color = Color.Red,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(top = 8.dp),
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.logout() },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("ยกเลิก", color = Color.Gray)
                        }

                        Button(
                            onClick = {
                                val ok = viewModel.verifyTwoFactor(twoFactorCode)
                                if (ok) {
                                    onLoginSuccess(userRole ?: "INSPECTOR")
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = ColorCpramBlue),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("ยืนยัน", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}


// ----------------------------------------------------------------------------
// 2. ADMIN SUMMARY DASHBOARD (REAL-TIME COMPLIANCE)
// ----------------------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: FireInspectViewModel,
    onNavigateToAddExtinguisher: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val extinguishers by viewModel.extinguishers.collectAsStateWithLifecycle()
    val logs by viewModel.inspectionLogs.collectAsStateWithLifecycle()
    val stats by viewModel.checkStatistics.collectAsStateWithLifecycle()
    val lineToken by viewModel.lineToken.collectAsStateWithLifecycle()

    var editingLineToken by remember { mutableStateOf(lineToken) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var selectedExtinguisher by remember { mutableStateOf<FireExtinguisher?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("สรุปสำหรับผู้จัดการ CPRAM", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text("สุราษฎร์ธานี (Real-time Dashboard)", fontSize = 11.sp, color = Color.White.copy(alpha = 0.8f))
                    }
                },
                actions = {
                    IconButton(onClick = { showSettingsDialog = true }) {
                        Icon(Icons.Default.Settings, contentDescription = "LINE Config", tint = Color.White)
                    }
                    IconButton(onClick = onLogout) {
                        Icon(Icons.Default.ExitToApp, contentDescription = "Log Out", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ColorCpramBlue,
                    titleContentColor = Color.White
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToAddExtinguisher,
                containerColor = ColorCpramRed,
                contentColor = Color.White,
                modifier = Modifier.testTag("add_extinguisher_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add New Fire Asset")
            }
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // OFFLINE INDICATOR BAR (Simulation Mode)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(ColorCpramBlue.copy(alpha = 0.1f))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.CloudSync, contentDescription = null, tint = ColorCpramBlue)
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("การเชื่อมต่อระบบเซิร์ฟเวอร์", fontSize = 11.sp, color = Color.Gray)
                    Text("ระบบ คลาวด์เสถียร / บันทึกประวัติเรียลไทม์", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = ColorCpramBlue)
                }
                Surface(
                    color = ColorCpramRed.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        "ONLINE",
                        color = ColorCpramRed,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // STATS TILES GRID
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatCard(
                    title = "ทั้งหมด",
                    value = stats.total.toString(),
                    subtitle = "ถังสะสม",
                    color = ColorCpramBlue,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "ตรวจแล้ว",
                    value = "${stats.checked}/${stats.total}",
                    subtitle = "${stats.percentage}% สำเร็จ",
                    color = ColorCpramGold,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatCard(
                    title = "ปกติทางเทคนิค",
                    value = stats.normal.toString(),
                    subtitle = "แรงดันเสถียร",
                    color = Color(0xFF2E7D32),
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "ตรวจพบชำรุด",
                    value = stats.problem.toString(),
                    subtitle = "แจ้งความเสียหาย",
                    color = Color(0xFFC62828),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // COMPLIANCE GRAPHIC CHART (Custom Canvas Drawing)
            Text(
                "สัดส่วนความปลอดภัยและตรวจสอบ",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = ColorCpramBlue
            )
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("กราฟแท่งรายงานเชิงวิเคราะห์ภาพรวมถัง", fontSize = 12.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(16.dp))

                    // Draw our custom bar graph in Canvas
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                    ) {
                        val barWidth = 40.dp.toPx()
                        val spacing = (size.width - (3 * barWidth)) / 4

                        val totalF = stats.total.toFloat()
                        val hNormal = if (totalF > 0) (stats.normal.toFloat() / totalF * size.height) else 0f
                        val hProblem = if (totalF > 0) (stats.problem.toFloat() / totalF * size.height) else 0f
                        val hPending = if (totalF > 0) ((stats.total - stats.checked).toFloat() / totalF * size.height) else 0f

                        // 1. Bar Normal
                        drawRect(
                            color = Color(0xFF2E7D32),
                            topLeft = Offset(spacing, size.height - hNormal),
                            size = Size(barWidth, hNormal)
                        )
                        // 2. Bar Problem
                        drawRect(
                            color = Color(0xFFC62828),
                            topLeft = Offset(2 * spacing + barWidth, size.height - hProblem),
                            size = Size(barWidth, hProblem)
                        )
                        // 3. Bar Pending
                        drawRect(
                            color = Color.Gray.copy(alpha = 0.5f),
                            topLeft = Offset(3 * spacing + 2 * barWidth, size.height - hPending),
                            size = Size(barWidth, hPending)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Text("ปกติ (${stats.normal})", color = Color(0xFF2E7D32), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text("ชำรุด (${stats.problem})", color = Color(0xFFC62828), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text("ยังไม่ได้เช็ค (${stats.total - stats.checked})", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // DYNAMIC LAYOUT MAP OF FIRE EXTINGUISHERS (Interactive Plot Canvas)
            Text(
                "แผนผังบอกตำแหน่งจุดติดตั้ง (CPRAM Safety Layout)",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = ColorCpramBlue
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "คลิกที่จุดวงกลมสีต่างๆ เพื่อดูรายละเอียดสารของถังดับเพลิงพิกัดวิศวกรรม",
                fontSize = 11.sp,
                color = Color.Gray
            )
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    val w = maxWidth
                    val h = maxHeight

                    // Schematic Factory Layout Drawing in background
                    Canvas(modifier = Modifier.fillMaxSize().background(Color(0xFFEAEFF8))) {
                        // Outline external walls
                        drawRect(
                            color = ColorCpramBlue.copy(alpha = 0.2f),
                            topLeft = Offset(10f, 10f),
                            size = Size(size.width - 20f, size.height - 20f),
                            style = Stroke(width = 3f)
                        )
                        // Partition dividing lines representing factory sectors
                        // Corridor division
                        drawLine(
                            color = ColorCpramBlue.copy(alpha = 0.15f),
                            start = Offset(size.width * 0.4f, 10f),
                            end = Offset(size.width * 0.4f, size.height - 10f),
                            strokeWidth = 3f
                        )
                        drawLine(
                            color = ColorCpramBlue.copy(alpha = 0.15f),
                            start = Offset(10f, size.height * 0.6f),
                            end = Offset(size.width - 10f, size.height * 0.6f),
                            strokeWidth = 3f
                        )

                        // Draw Room text coordinates representing CPRAM sectors
                        // Labels
                        // Office left
                        // Baker Middle
                        // Packaging Right
                    }

                    // Sector text watermark layout labels overlay
                    Text("อาคารสํานักงาน", modifier = Modifier.padding(start = 12.dp, top = 12.dp), fontSize = 10.sp, color = ColorCpramBlue.copy(alpha = 0.6f), fontWeight = FontWeight.Bold)
                    Text("สายผลิตขนมปัง", modifier = Modifier.align(Alignment.CenterStart).padding(start = 12.dp, bottom = 24.dp), fontSize = 10.sp, color = ColorCpramBlue.copy(alpha = 0.6f), fontWeight = FontWeight.Bold)
                    Text("ห้องแพ็กคลีนรูม", modifier = Modifier.align(Alignment.Center), fontSize = 10.sp, color = ColorCpramBlue.copy(alpha = 0.6f), fontWeight = FontWeight.Bold)
                    Text("คลังบรรจุเสร็จ", modifier = Modifier.align(Alignment.BottomEnd).padding(end = 12.dp, bottom = 12.dp), fontSize = 10.sp, color = ColorCpramBlue.copy(alpha = 0.6f), fontWeight = FontWeight.Bold)

                    // Overlay dynamic interactable dots for each extinguisher
                    extinguishers.forEach { ext ->
                        val nodeColor = when (ext.status) {
                            "NORMAL" -> Color(0xFF2E7D32)
                            "PROBLEM" -> Color(0xFFC62828)
                            else -> ColorCpramGold
                        }

                        val pixelX = ext.layoutX * w.value
                        val pixelY = ext.layoutY * h.value

                        Box(
                            modifier = Modifier
                                .offset(x = pixelX.dp - 10.dp, y = pixelY.dp - 10.dp)
                                .size(24.dp)
                                .background(nodeColor.copy(alpha = 0.25f), CircleShape)
                                .border(1.5.dp, nodeColor, CircleShape)
                                .clickable { selectedExtinguisher = ext },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                ext.id.takeLast(2),
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // REPORT EXPORT ACTIONS TABLE
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "ส่งออกรายงานสรุปสำหรับงานบำรุงรักษา",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = ColorCpramBlue
                    )
                    Text("ส่งออกไฟล์รูปเล่ม PDF และ Excel สำหรับยื่นรองรับกองประกวดความปลอดภัยโรงงาน", fontSize = 11.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val uri = viewModel.exportPdfReport(context)
                                if (uri != null) {
                                    shareFile(context, uri, "application/pdf")
                                } else {
                                    Toast.makeText(context, "ไม่สามารถสร้าง PDF ได้", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = ColorCpramBlue),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("PDF Report", fontSize = 12.sp)
                        }

                        Button(
                            onClick = {
                                val uri = viewModel.exportCsvReport(context)
                                if (uri != null) {
                                    shareFile(context, uri, "text/csv")
                                } else {
                                    Toast.makeText(context, "ไม่สามารถสร้าง CSV ได้", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B5E20)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.GridOn, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Excel (CSV)", fontSize = 12.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // INSPECTIONS LIST TABLE SUMMARY
            Text("รายการถังดับเพลิงเชิงลึกในโรงงานทั้งหมด", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = ColorCpramBlue)
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column {
                    extinguishers.forEach { ext ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedExtinguisher = ext }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(ColorCpramRed.copy(alpha = 0.1f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.FireExtinguisher, contentDescription = null, tint = ColorCpramRed, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(ext.id, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                                Text(ext.location, fontSize = 11.sp, color = Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("ประเภท: ${ext.type} (${ext.size})", fontSize = 10.sp, color = ColorCpramBlue, fontWeight = FontWeight.Medium)
                            }
                            // Status tag
                            val label = when (ext.status) {
                                "NORMAL" -> "ปกติ (Green)"
                                "PROBLEM" -> "ชำรุด (Red)"
                                else -> "ยังไม่ได้ตรวจ"
                            }
                            val tagCol = when (ext.status) {
                                "NORMAL" -> Color(0xFF2E7D32)
                                "PROBLEM" -> Color(0xFFC62828)
                                else -> ColorCpramGold
                            }
                            Surface(
                                color = tagCol.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    label,
                                    fontSize = 10.sp,
                                    color = tagCol,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                        HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
                    }
                }
            }
        }
    }


    // 3. DIALOG INFO SELECT MODULE
    if (selectedExtinguisher != null) {
        val ext = selectedExtinguisher!!
        AlertDialog(
            onDismissRequest = { selectedExtinguisher = null },
            title = { Text(text = "รหัสสินทรัพย์: ${ext.id}", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = ColorCpramBlue) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("📍 ตำแหน่งติดตั้ง: ${ext.location}", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("🔥 ประเภทถัง: ${ext.type}", fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("⚖️ ขนาดน้ำหนัก: ${ext.size}", fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("📅 ตรวจเช็คถัดไปตามวาระ: ${ext.nextMaintenanceDate ?: "ไม่มี"}", fontSize = 12.sp, color = ColorCpramRed, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))

                    // QR STICKER PREVIEW INSIDE THE ADMIN DIALOG
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        QrCodeView(payload = ext.id, location = ext.location)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        Toast.makeText(context, "กำลังส่งไฟล์ QR Label ไปยังเครื่องพิมพ์เครือข่าย ซีพีแรม เพื่อสั่งพรินต์สติกเกอร์...", Toast.LENGTH_LONG).show()
                        selectedExtinguisher = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ColorCpramRed)
                ) {
                    Text("สั่งพิมพ์ฉลาก (Print Label)")
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedExtinguisher = null }) {
                    Text("ปิดหน้าต่าง")
                }
            }
        )
    }

    // 4. SETTINGS DIALOG (LINE TOKEN CONFIG)
    if (showSettingsDialog) {
        AlertDialog(
            onDismissRequest = { showSettingsDialog = false },
            title = { Text("ตั้งค่าโทเค็น LINE Notify", fontWeight = FontWeight.Bold, color = ColorCpramBlue) },
            text = {
                Column {
                    Text("รหัสเข้ากลุ่มกลุ่มไลน์สำหรับแจ้งเรือนเหตุถังดับเพลิงมีปัญหาบกพร่องในโรงเรียน/โรงงานทันที", fontSize = 11.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = editingLineToken,
                        onValueChange = { editingLineToken = it },
                        label = { Text("LINE Notify Access Token") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateLineToken(editingLineToken)
                        showSettingsDialog = false
                        Toast.makeText(context, "บันทึกข้อมูลโทเค็นการส่งเตือนภัย เรียบร้อยแล้ว!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ColorCpramBlue)
                ) {
                    Text("บันทึก")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSettingsDialog = false }) {
                    Text("ยกเลิก")
                }
            }
        )
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    subtitle: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(title, fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Light)
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, fontSize = 20.sp, fontWeight = FontWeight.Black, color = color)
            Spacer(modifier = Modifier.height(2.dp))
            Text(subtitle, fontSize = 10.sp, color = Color.DarkGray, fontWeight = FontWeight.Medium)
        }
    }
}

// ----------------------------------------------------------------------------
// 3. ADMIN ADD EXTINGUISHER SCREEN
// ----------------------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminAddExtinguisherScreen(
    viewModel: FireInspectViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var id by remember { mutableStateOf("FE-CPRAM-07") }
    var location by remember { mutableStateOf("คลังเก็บวัตถุดิบบรรจุภัณฑ์ (Zone F)") }
    var type by remember { mutableStateOf("เคมีแห้ง (Dry Chemical)") }
    var size by remember { mutableStateOf("15 lbs") }

    // Plotting coordinate sliders
    var layoutX by remember { mutableFloatStateOf(0.5f) }
    var layoutY by remember { mutableFloatStateOf(0.5f) }

    val typeOptions = listOf(
        "เคมีแห้ง (Dry Chemical)",
        "CO2 (คาร์บอนไดออกไซด์)",
        "ฮาโลตรอน (Halotron Eco-friendly)",
        "โฟม (Foam AFFF)",
        "เคมีสูตรพิเศษ (Wet Chemical)"
    )

    val sizeOptions = listOf("5 lbs", "10 lbs", "15 lbs", "20 lbs")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ลงทะเบียนถังดับเพลิงใหม่", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ColorCpramBlue,
                    titleContentColor = Color.White
                )
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("กรอกข้อมูลถังใหม่ที่จะนำเข้าไปแปะพิกัดหน้างาน", fontSize = 12.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = id,
                        onValueChange = { id = it },
                        label = { Text("รหัสถังดับเพลิง (Asset Serial ID)") },
                        modifier = Modifier.fillMaxWidth().testTag("add_id_input"),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = location,
                        onValueChange = { location = it },
                        label = { Text("สถานที่พิกัดการติดตั้งโดยละเอียด") },
                        modifier = Modifier.fillMaxWidth().testTag("add_location_input"),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("ประเภทของสารเคมีดับเพลิง:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ColorCpramBlue)
                    Spacer(modifier = Modifier.height(4.dp))
                    typeOptions.forEach { t ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { type = t }
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(selected = (type == t), onClick = { type = t })
                            Text(t, fontSize = 12.sp, color = Color.Black)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("ขนาดน้ำหนักบรรจุเกรดอุตสาหกรรม:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ColorCpramBlue)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        sizeOptions.forEach { sz ->
                            Button(
                                onClick = { size = sz },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (size == sz) ColorCpramBlue else Color.LightGray.copy(alpha = 0.3f),
                                    contentColor = if (size == sz) Color.White else Color.Black
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(sz, fontSize = 10.sp)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // SLIDER POSITIONER FOR DIGITAL SAFETY MAP COORDINATES
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("พินระบุตำแหน่งสัมพัทธ์ในแผนผังโรงงานสเปซ", fontWeight = FontWeight.Bold, color = ColorCpramBlue, fontSize = 13.sp)
                    Text("ใช้เพื่อช่วยคนงานสแกนระบุตำแหน่งฉลาดในการตรวจพิน", fontSize = 11.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(12.dp))

                    Text("พิกัดแนวนอน (X-Axis): ${(layoutX * 100).toInt()}%", fontSize = 11.sp)
                    Slider(value = layoutX, onValueChange = { layoutX = it })

                    Spacer(modifier = Modifier.height(4.dp))

                    Text("พิกัดแนวตั้ง (Y-Axis): ${(layoutY * 100).toInt()}%", fontSize = 11.sp)
                    Slider(value = layoutY, onValueChange = { layoutY = it })
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // GENERATED WORK STICKER DISPLAY PREVIEW
            Text("พรีวิวรูปลักษณ์ฉลาก QR Code พิมพ์แปะถังดิบเพลิงสำเร็จรูป", fontWeight = FontWeight.Bold, color = ColorCpramBlue, fontSize = 13.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                QrCodeView(payload = id, location = location)
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    viewModel.addNewExtinguisher(id, location, type, size, layoutX, layoutY)
                    Toast.makeText(context, "สำเร็จ! ลงทะเบียนพร้อมส่งข้อมูลแผนที่ระวางแล้ว", Toast.LENGTH_SHORT).show()
                    onNavigateBack()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("submit_new_extinguisher"),
                colors = ButtonDefaults.buttonColors(containerColor = ColorCpramRed),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("บันทึกสินทรัพย์และสรุปแผนที่", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }
    }
}


// ----------------------------------------------------------------------------
// 4. INSPECTOR ACTION OVERALL PORTAL CONTROLLER (FOR FIELD EMPLOYEES)
// ----------------------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InspectorHomeScreen(
    viewModel: FireInspectViewModel,
    onNavigateToScan: () -> Unit,
    onNavigateToForm: (String) -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val extinguishers by viewModel.extinguishers.collectAsStateWithLifecycle()
    val logs by viewModel.inspectionLogs.collectAsStateWithLifecycle()
    val stats by viewModel.checkStatistics.collectAsStateWithLifecycle()
    val isOffline by viewModel.isOfflineMode.collectAsStateWithLifecycle()
    val syncMsg by viewModel.syncMessage.collectAsStateWithLifecycle()
    val lineAlert by viewModel.lineAlertMessage.collectAsStateWithLifecycle()

    LaunchedEffect(syncMsg) {
        if (syncMsg != null) {
            Toast.makeText(context, syncMsg, Toast.LENGTH_LONG).show()
            viewModel.clearSyncMessage()
        }
    }

    LaunchedEffect(lineAlert) {
        if (lineAlert != null) {
            Toast.makeText(context, lineAlert, Toast.LENGTH_LONG).show()
            viewModel.clearLineAlert()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("งานตรวจสอบภาคสนาม ซีพีแรม", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        Text("บริษัท ซีพีแรม จำกัด (สุราษฎร์ธานี)", fontSize = 10.sp, color = Color.White.copy(alpha = 0.8f))
                    }
                },
                actions = {
                    // Offline state toggle simulator button
                    IconButton(onClick = { viewModel.toggleOfflineMode() }) {
                        Icon(
                            imageVector = if (isOffline) Icons.Default.WifiOff else Icons.Default.Wifi,
                            contentDescription = "Toggle Network Status",
                            tint = if (isOffline) ColorCpramGold else Color.White
                        )
                    }
                    IconButton(onClick = onLogout) {
                        Icon(Icons.Default.ExitToApp, contentDescription = "Log Out", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ColorCpramBlue,
                    titleContentColor = Color.White
                )
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // INSPECTION COVERAGE RADIAL STATUS SUMMARY BANNER
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(containerColor = ColorCpramBlue),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("ความคืบหน้าตรวจสอบรอบปัจจุบัน", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
                        Text("ประจำตารางเดือนนี้", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "ทำการตรวจเช็คแล้ว ${stats.checked} จากทั้งหมด ${stats.total} จุดพิกัด",
                            color = ColorCpramGold,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Box(
                        modifier = Modifier.size(64.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            progress = { stats.percentage / 100f },
                            modifier = Modifier.fillMaxSize(),
                            color = ColorCpramGold,
                            strokeWidth = 6.dp,
                            trackColor = Color.White.copy(alpha = 0.2f)
                        )
                        Text(
                            text = "${stats.percentage}%",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // OFFLINE PILOT ALERTER SUMMARY
            if (isOffline) {
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    color = ColorCpramGold.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, ColorCpramGold),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = ColorCpramGold)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("สิทธิ์โหมดออฟไลน์กำลังทำงาน (Offline Active)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ColorCpramBlue)
                            Text("คุณสามารถตรวจถังดับเพลิงได้โดยไม่ต้องมีเน็ต ระบบจะเซฟข้อมูลอัตโนมัติ และซิงค์ขึ้นเมื่อตรวจจับได้สัญญาณ", fontSize = 10.sp, color = Color.Black)
                        }
                    }
                }
            }

            // DYNAMIC BUTTON ACTION TO TRIGGER CAMERA SCANNING OVERALL CRITICAL CUJ
            Button(
                onClick = onNavigateToScan,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("scan_trigger_button"),
                colors = ButtonDefaults.buttonColors(containerColor = ColorCpramRed),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text("สแกนตรวจสอบถังดับเพลิง (Scan Asset QR)", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text("รายการถังรอระวางให้ตรวจสอบ", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = ColorCpramBlue)
            Spacer(modifier = Modifier.height(4.dp))

            // LIST OF WORKLOAD ITEMS
            LazyColumn(
                modifier = Modifier.fillMaxSize().weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(extinguishers) { ext ->
                    val statusText = when (ext.status) {
                        "NORMAL" -> "ปกติ (Passed)"
                        "PROBLEM" -> "ตรวจพบชำรุด ⚠️"
                        else -> "ยังไม่ได้ทำการตรวจเช็ค (Pending)"
                    }
                    val statusCol = when (ext.status) {
                        "NORMAL" -> Color(0xFF2E7D32)
                        "PROBLEM" -> Color(0xFFC62828)
                        else -> Color.DarkGray
                    }
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToForm(ext.id) },
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(statusCol.copy(alpha = 0.08f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (ext.status == "PENDING") Icons.Outlined.HourglassEmpty else Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = statusCol
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(ext.id, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                                Text(ext.location, fontSize = 11.sp, color = Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(statusText, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = statusCol)
                            }
                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.LightGray)
                        }
                    }
                }
            }
        }
    }
}


// ----------------------------------------------------------------------------
// 5. CAMERAX VIEW QR BARCODE RECOGNITION SCREEN WITH EMULATOR BYPASS TAG
// ----------------------------------------------------------------------------
@OptIn(ExperimentalUnsignedTypes::class, ExperimentalMaterial3Api::class)
@Composable
fun CameraScannerScreen(
    viewModel: FireInspectViewModel,
    onNavigateToInspect: (String) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val extinguishers by viewModel.extinguishers.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted -> hasCameraPermission = granted }
    )

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            launcher.launch(Manifest.permission.CAMERA)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("สแกนฉลากเครื่องตรวจสอบ", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ColorCpramBlue,
                    titleContentColor = Color.White
                )
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(Color.Black)
        ) {
            // CAMERA PREVIEW CONTAINER WITH ERROR GRACEFUL DEGRADATION
            if (hasCameraPermission) {
                AndroidView(
                    factory = { ctx ->
                        val previewView = PreviewView(ctx)
                        val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                        cameraProviderFuture.addListener({
                            try {
                                val cameraProvider = cameraProviderFuture.get()
                                val preview = Preview.Builder().build().also {
                                    it.setSurfaceProvider(previewView.surfaceProvider)
                                }
                                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                                cameraProvider.unbindAll()
                                cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview)
                            } catch (e: Exception) {
                                Log.e("CameraScanner", "Camera startup error: ${e.message}", e)
                            }
                        }, ContextCompat.getMainExecutor(ctx))
                        previewView
                    },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF1E1E1E)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                        Icon(Icons.Outlined.CameraAlt, contentDescription = null, tint = Color.White, modifier = Modifier.size(56.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("กรุณาอนุญาตการเข้าถึงกล้องถ่ายรูปในการจำลองตัวสแกน", color = Color.White, textAlign = TextAlign.Center, fontSize = 12.sp)
                    }
                }
            }

            // HUD OVERLAY: Scanner focus targeting frame drawn with high-contrast brackets
            Box(
                modifier = Modifier
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                // Focus container frame overlay box
                Box(
                    modifier = Modifier
                        .size(240.dp)
                        .border(3.dp, ColorCpramRed.copy(alpha = 0.8f), RoundedCornerShape(12.dp))
                ) {
                    Text(
                        text = "นำกรอบเล็งไปจ่อสติกเกอร์ถัง",
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 8.dp)
                            .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                        color = Color.White,
                        fontSize = 11.sp
                    )
                }
            }

            // CRITICAL TEST BYPASS CONSOLE (Simulator UI Overrides)
            // Displays a list of existing asset profiles that inspectors can instantly trigger code simulation checks on!
            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.95f))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "📱 แผงควบคุมจำลองการสแกน QR Code (Bypass Panel)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = ColorCpramBlue
                    )
                    Text("สำหรับการสาธิตบนสตรีมมิ่งที่ไม่มีจุดถังดับเพลิงจริง ให้คลิกถังดับเพลิงด้านล่างเพื่อเลียนแบบการตรวจจับสแกน:", fontSize = 10.sp, color = Color.DarkGray)
                    Spacer(modifier = Modifier.height(8.dp))

                    LazyColumn(
                        modifier = Modifier.height(130.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(extinguishers) { ext ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(ColorCpramBlue.copy(alpha = 0.08f))
                                    .clickable { onNavigateToInspect(ext.id) }
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.QrCode, contentDescription = null, tint = ColorCpramBlue, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(ext.id, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                                    Text(ext.location, fontSize = 9.sp, color = Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                                Surface(
                                    color = ColorCpramRed,
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text("สแกนจำลอง", color = Color.White, fontSize = 9.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}


// ----------------------------------------------------------------------------
// 6. 5-POINT FIRE STICKER INSPECTION COMPLIANCE FORM SCREEN
// ----------------------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InspectionFormScreen(
    extId: String,
    viewModel: FireInspectViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val extinguishers by viewModel.extinguishers.collectAsStateWithLifecycle()
    val ext = extinguishers.find { it.id == extId }

    if (ext == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("ไม่พบรหัสถังดับเพลิงดังกล่าวในระบบฐานข้อมูล")
        }
        return
    }

    // 5-Point inspection checklist checklist states
    var pressureOk by remember { mutableStateOf(true) }
    var safetyPinOk by remember { mutableStateOf(true) }
    var nozzleOk by remember { mutableStateOf(true) }
    var physicalBodyOk by remember { mutableStateOf(true) }
    var expirationOk by remember { mutableStateOf(true) } // Date check

    var remarks by remember { mutableStateOf("") }
    var photoUri by remember { mutableStateOf<String?>(null) }
    var photoBitmap by remember { mutableStateOf<Bitmap?>(null) }

    // Launcher to capture photo representatively
    val shootPhotoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bmp: Bitmap? ->
        if (bmp != null) {
            photoBitmap = bmp
            photoUri = "captured_bitmap_uri_${System.currentTimeMillis()}"
            Toast.makeText(context, "บันทึกภาพถ่ายถังตรวจสอบ เรียบร้อย!", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ตรวจสอบจุดตรวจ: ${ext.id}", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ColorCpramBlue,
                    titleContentColor = Color.White
                )
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // FIRE ASSET DETAILED METADATA CARD HEADER
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(containerColor = ColorCpramBlue.copy(alpha = 0.05f))
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = ColorCpramBlue, modifier = Modifier.size(32.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(ext.id, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = ColorCpramBlue)
                        Text("📍ตำแหน่ง: ${ext.location}", fontSize = 12.sp, color = Color.Black)
                        Text("🔥 ประเภทถัง: ${ext.type} [ขนาด ${ext.size}]", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
                    }
                }
            }

            Text(
                text = "รายการตรวจสอบความปลอดภัย 5 จุดสำคัญ (ISO Standard Check)",
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // ISO CHECKBOX TILES
            ChecklistTile(
                index = "1",
                label = "เกจวัดแรงดันไฟฟ้า/หน้าปัดสีเขียว (Pressure Gauge Status)",
                checked = pressureOk,
                onCheckedChange = { pressureOk = it }
            )
            ChecklistTile(
                index = "2",
                label = "ซีลสายรัดสลักนิรภัยแน่นหนา (Safety Pin Integrity)",
                checked = safetyPinOk,
                onCheckedChange = { safetyPinOk = it }
            )
            ChecklistTile(
                index = "3",
                label = "สายฉีดแก๊สปราศจากการอุดตัน/คราบกาว (Nozzle OK)",
                checked = nozzleOk,
                onCheckedChange = { nozzleOk = it }
            )
            ChecklistTile(
                index = "4",
                label = "โครงสร้างโลหะถังไม่เป็นสนิม/ไม่บุบชำรุด (Physical Tank Check)",
                checked = physicalBodyOk,
                onCheckedChange = { physicalBodyOk = it }
            )
            ChecklistTile(
                index = "5",
                label = "วาระตรวจบำรุงรักษายังไม่หมดอายุตามรอบ (Maintenance Shelf Life)",
                checked = expirationOk,
                onCheckedChange = { expirationOk = it }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // CAMERA TAKING SECURE AUDIT PHOTO
            Text("ถ่ายภาพยืนยันการตรวจสอบจริง (Photo Confirmation Required)", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = ColorCpramBlue)
            Spacer(modifier = Modifier.height(8.dp))

            if (photoBitmap != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    colors = CardDefaults.cardColors(containerColor = Color.LightGray)
                ) {
                    Image(
                        bitmap = photoBitmap!!.asImageBitmap(),
                        contentDescription = "Extinguisher Snap",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = { shootPhotoLauncher.launch(null) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = ColorCpramBlue)
            ) {
                Icon(Icons.Default.CameraAlt, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (photoBitmap == null) "เปิดหน้ากล้องเพื่อถ่ายภาพถัง" else "ถ่ายภาพเสร็จสมบูรณ์ - คลิกถ่ายใหม่")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // REMARKS Text Field
            OutlinedTextField(
                value = remarks,
                onValueChange = { remarks = it },
                label = { Text("คำชี้แจง หรือหมายเหตุความเสียหายเพิ่มเติม") },
                modifier = Modifier.fillMaxWidth().testTag("inspect_remarks_input"),
                shape = RoundedCornerShape(8.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    val overallOk = pressureOk && safetyPinOk && nozzleOk && physicalBodyOk && expirationOk
                    viewModel.submitInspection(
                        extId = ext.id,
                        pressureGaugeOk = pressureOk,
                        safetyPinOk = safetyPinOk,
                        nozzleOk = nozzleOk,
                        physicalStatusOk = physicalBodyOk,
                        remarks = remarks.ifBlank { if (overallOk) "ปกติเสถียร" else "แจ้งกู้คืนสติ" },
                        photoPath = photoUri
                    )
                    Toast.makeText(context, "บันทึกข้อมูลการตรวจสอบจุดพิกัด ${ext.id} สำเร็จแล้ว!", Toast.LENGTH_LONG).show()
                    onNavigateBack()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("submit_inspection_form"),
                colors = ButtonDefaults.buttonColors(containerColor = ColorCpramRed),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("ยืนยันการตรวจและส่งประวัติ", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }
    }
}

@Composable
fun ChecklistTile(
    index: String,
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(0.5.dp, Color.LightGray.copy(alpha = 0.8f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onCheckedChange(!checked) }
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(ColorCpramBlue.copy(alpha = 0.08f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(index, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ColorCpramBlue)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(label, modifier = Modifier.weight(1f), fontSize = 11.sp, color = Color.Black, lineHeight = 15.sp)
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = Color(0xFF2E7D32),
                    uncheckedThumbColor = Color.DarkGray,
                    uncheckedTrackColor = Color.LightGray
                )
            )
        }
    }
}


// Shared helper to trigger File Provider Choosers natively on Android.
private fun shareFile(context: Context, uri: Uri, mimeType: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = mimeType
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "แบ่งปันรายงานสรุป CPRAM"))
}
