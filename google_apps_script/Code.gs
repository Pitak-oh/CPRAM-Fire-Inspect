/**
 * CPRAM Fire Inspect - Google Apps Script Backend (Code.gs)
 * ระบบตรวจสอบถังดับเพลิงอัจฉริยะ ซีพีแรม จำกัด (สุราษฎร์ธานี)
 * 
 * วิธีการติดตั้ง:
 * 1. สร้าง Google Sheets ใหม่ขึ้นมา 1 ไฟล์
 * 2. ไปที่เมนู ส่วนขยาย (Extensions) -> Apps Script
 * 3. วางโค้ดลบไฟล์ซ้ำออกแล้ววางโค้ดจากไฟล์ Code.gs นี้ลงไป
 * 4. สร้างไฟล์ HTML เพิ่มเติมตามที่ให้มา (Index.html, Dashboard.html, Styles.html)
 * 5. กด บันทึก และ กด เผยแพร่ (Deploy) -> การทำให้ใช้งานได้ใหม่ (New Deployment) -> ชนิด: เว็บแอป (Web App)
 */

function doGet(e) {
  var page = e.parameter.page || 'index';
  if (page === 'dashboard') {
    return HtmlService.createTemplateFromFile('Dashboard')
        .evaluate()
        .setTitle('CPRAM Fire Inspect - แดชบอร์ดผู้ดูแลระบบ')
        .addMetaTag('viewport', 'width=device-width, initial-scale=1')
        .setXFrameOptionsMode(HtmlService.XFrameOptionsMode.ALLOWALL);
  }
  return HtmlService.createTemplateFromFile('Index')
      .evaluate()
      .setTitle('CPRAM Fire Inspect - ตรวจสอบถังดับเพลิง')
      .addMetaTag('viewport', 'width=device-width, initial-scale=1')
      .setXFrameOptionsMode(HtmlService.XFrameOptionsMode.ALLOWALL);
}

// ช่วยดึงไฟล์ HTML อื่นๆ เข้ามารวมกับหน้าหลัก
function include(filename) {
  return HtmlService.createHtmlOutputFromFile(filename).getContent();
}

/**
 * ตั้งค่าและสแกนโครงสร้างชีตฐานข้อมูลเริ่มต้น
 */
function initDatabase() {
  var ss = SpreadsheetApp.getActiveSpreadsheet();
  
  // 1. ชีตข้อมูลถังดับเพลิง (Extinguishers)
  var extSheet = ss.getSheetByName('Extinguishers');
  if (!extSheet) {
    extSheet = ss.insertSheet('Extinguishers');
    var headers = ['ID', 'Location', 'Type', 'Size', 'InstallationDate', 'NextMaintenanceDate', 'Status', 'LastInspectedBy', 'LastInspectedAt', 'Remarks', 'LayoutX', 'LayoutY'];
    extSheet.getRange(1, 1, 1, headers.length).setValues([headers]).setFontWeight('bold').setBackground('#E21C26').setFontColor('white');
    
    // ใส่ข้อมูลจำลองเริ่มต้นสำหรับ ซีพีแรม สุราษฎร์ธานี
    var prepopulated = [
      ['FE-CPRAM-01', 'อาคารสำนักงานและประชาสัมพันธ์ (ห้องรับรอง)', 'CO2 (คาร์บอนไดออกไซด์)', '10 lbs', '2025-01-10', '2026-07-10', 'PENDING', '-', '-', '-', 0.22, 0.28],
      ['FE-CPRAM-02', 'ไลน์ผลิตเบเกอรี่ (โซนเปลี่ยนชุด 1)', 'เคมีแห้ง (Dry Chemical)', '15 lbs', '2024-11-15', '2026-08-15', 'PENDING', '-', '-', '-', 0.48, 0.35],
      ['FE-CPRAM-03', 'ห้องบรรจุปลอดเชื้อ (Aseptic Packaging ชั้น 1)', 'ฮาโลตรอน (Halotron)', '10 lbs', '2025-02-01', '2026-09-01', 'PENDING', '-', '-', '-', 0.65, 0.42],
      ['FE-CPRAM-04', 'คลังสินค้าแช่เย็น (Cold Storage Row B)', 'โฟม (Foam AFFF)', '20 lbs', '2024-08-20', '2026-07-20', 'PENDING', '-', '-', '-', 0.82, 0.58],
      ['FE-CPRAM-05', 'ห้องควบคุมระบบไฟฟ้าหลัก (MDB Room)', 'CO2 (คาร์บอนไดออกไซด์)', '15 lbs', '2025-01-10', '2026-07-10', 'PENDING', '-', '-', '-', 0.35, 0.72]
    ];
    extSheet.getRange(2, 1, prepopulated.length, prepopulated[0].length).setValues(prepopulated);
    extSheet.autoResizeColumns(1, headers.length);
  }
  
  // 2. ชีตประวัติการตรวจเช็ค (Logs)
  var logsSheet = ss.getSheetByName('Logs');
  if (!logsSheet) {
    logsSheet = ss.insertSheet('Logs');
    var logHeaders = ['LogID', 'ExtinguisherID', 'Location', 'InspectorName', 'Timestamp', 'PressureGaugeOk', 'SafetyPinOk', 'NozzleOk', 'PhysicalStatusOk', 'OverallStatus', 'PhotoUrl', 'Remarks'];
    logsSheet.getRange(1, 1, 1, logHeaders.length).setValues([logHeaders]).setFontWeight('bold').setBackground('#1B365D').setFontColor('white');
    logsSheet.autoResizeColumns(1, logHeaders.length);
  }
  
  return "ระบบติดตั้งและจำลองฐานข้อมูล CPRAM สำเร็จเรียบร้อย!";
}

/**
 * ดึงข้อมูลถังดับเพลิงทั้งหมดออกจากชีต
 */
function getExtinguishers() {
  var ss = SpreadsheetApp.getActiveSpreadsheet();
  var sheet = ss.getSheetByName('Extinguishers');
  if (!sheet) {
    initDatabase();
    sheet = ss.getSheetByName('Extinguishers');
  }
  
  var data = sheet.getDataRange().getValues();
  var headers = data[0];
  var result = [];
  
  for (var i = 1; i < data.length; i++) {
    var row = data[i];
    var obj = {};
    for (var j = 0; j < headers.length; j++) {
      obj[headers[j]] = row[j];
    }
    result.push(obj);
  }
  return result;
}

/**
 * ดึงข้อมูลประวัติการตรวจสอบล่าสุดทั้งหมด
 */
function getInspectionLogs() {
  var ss = SpreadsheetApp.getActiveSpreadsheet();
  var sheet = ss.getSheetByName('Logs');
  if (!sheet) return [];
  
  var data = sheet.getDataRange().getValues();
  if (data.length <= 1) return [];
  
  var headers = data[0];
  var result = [];
  
  for (var i = data.length - 1; i >= 1; i--) { // ดึงข้อมูลย้อนจากปัจจุบันเป็นหลัก
    var row = data[i];
    var obj = {};
    for (var j = 0; j < headers.length; j++) {
      obj[headers[j]] = row[j];
    }
    result.push(obj);
  }
  return result;
}

/**
 * ลงทะเบียนเพิ่มข้อมูลถังดับเพลิงใหม่ (Admin)
 */
function addExtinguisher(id, location, type, size, x, y) {
  var ss = SpreadsheetApp.getActiveSpreadsheet();
  var sheet = ss.getSheetByName('Extinguishers');
  
  var dateString = Utilities.formatDate(new Date(), "GMT+7", "yyyy-MM-dd");
  var futureDate = new Date();
  futureDate.setMonth(futureDate.getMonth() + 6);
  var nextMaintenance = Utilities.formatDate(futureDate, "GMT+7", "yyyy-MM-dd");
  
  // เพิ่มแถวข้อมูลท้ายชีต
  sheet.appendRow([
    id,
    location,
    type,
    size,
    dateString,
    nextMaintenance,
    'PENDING',
    '-',
    '-',
    '-',
    parseFloat(x) || 0.5,
    parseFloat(y) || 0.5
  ]);
  
  return { success: true, message: 'ลงทะเบียนถังรหัส ' + id + ' สำเร็จแล้ว' };
}

/**
 * บันทึกรายงานการตรวจสอบ พร้อมส่ง LINE Notify และอัปเดตสถิติลงฐานข้อมูลทันที
 */
function submitInspection(payload) {
  var ss = SpreadsheetApp.getActiveSpreadsheet();
  var extSheet = ss.getSheetByName('Extinguishers');
  var logsSheet = ss.getSheetByName('Logs');
  
  var extId = payload.extId;
  var isNormal = payload.pressureGaugeOk && payload.safetyPinOk && payload.nozzleOk && payload.physicalStatusOk;
  var overallStatus = isNormal ? 'NORMAL' : 'PROBLEM';
  var timestamp = new Date();
  var timestampStr = Utilities.formatDate(timestamp, "GMT+7", "yyyy-MM-dd HH:mm:ss");
  
  // 1. บันทึกข้อมูลรายงานการตรวจลงในชีต Logs
  var logId = Utilities.getUuid();
  logsSheet.appendRow([
    logId,
    extId,
    payload.location || '-',
    payload.inspectorName || 'พนักงานตรวจความปลอดภัย',
    timestampStr,
    payload.pressureGaugeOk ? 'OK' : 'FAIL',
    payload.safetyPinOk ? 'OK' : 'FAIL',
    payload.nozzleOk ? 'OK' : 'FAIL',
    payload.physicalStatusOk ? 'OK' : 'FAIL',
    overallStatus,
    payload.photoBase64 ? savePhotoToDrive(payload.photoBase64, extId) : 'no_photo',
    payload.remarks || '-'
  ]);
  
  // 2. อัปเดตสถานะของถังในชีต Extinguishers
  var extData = extSheet.getDataRange().getValues();
  for (var i = 1; i < extData.length; i++) {
    if (extData[i][0] === extId) {
      extSheet.getRange(i + 1, 7).setValue(overallStatus); // Status
      extSheet.getRange(i + 1, 8).setValue(payload.inspectorName); // LastInspectedBy
      extSheet.getRange(i + 1, 9).setValue(timestampStr); // LastInspectedAt
      extSheet.getRange(i + 1, 10).setValue(payload.remarks || '-'); // Remarks
      break;
    }
  }
  
  // 3. ส่งระบบแจ้งเตือนกรณีพบบัญหาชำรุดผ่าน LINE Notify ทันที
  if (!isNormal) {
    sendLineNotification(payload, overallStatus);
  }
  
  return { success: true, status: overallStatus };
}

/**
 * บันทึกรูปภาพเข้าสู่ Google Drive ของผู้ใช้โดยอัตโนมัติ
 */
function savePhotoToDrive(base64Data, extId) {
  try {
    var raw = base64Data.split(',')[1] || base64Data;
    var decoded = Utilities.base64Decode(raw);
    var blob = Utilities.newBlob(decoded, 'image/jpeg', 'inspect_' + extId + '_' + Date.now() + '.jpg');
    
    // ค้นหาโฟลเดอร์สำหรับเก็บภาพ
    var folderName = 'CPRAM_Fire_Photos';
    var folders = DriveApp.getFoldersByName(folderName);
    var folder;
    if (folders.hasNext()) {
      folder = folders.next();
    } else {
      folder = DriveApp.createFolder(folderName);
    }
    
    var file = folder.createFile(blob);
    file.setSharing(DriveApp.Access.ANYONE_WITH_LINK, DriveApp.Permission.VIEW);
    return file.getUrl();
  } catch (e) {
    return 'error_save_drive: ' + e.toString();
  }
}

/**
 * ฟังก์ชันส่งข้อความ LINE Notify แจ้งปัญหาถังดับเพลิงมีปัญหาชำรุด
 */
function sendLineNotification(payload, status) {
  // ใส่ Token ของท่าน หรือหากเป็นโหมดตั้งค่า Dynamic ในระบบหลังบ้าน 
  // แนะนำให้ดึงจากสคริปต์ PropertiesService เพื่อความปลอดภัยและยืดหยุ่น
  var token = PropertiesService.getScriptProperties().getProperty('LINE_TOKEN') || 'YOUR_LINE_NOTIFY_TOKEN_HERE';
  if (token === 'YOUR_LINE_NOTIFY_TOKEN_HERE' || !token) {
    Logger.log('Line Token ยังไม่ได้ตั้งค่าระบบ ข้ามการส่งข้อความ');
    return;
  }
  
  var defects = [];
  if (!payload.pressureGaugeOk) defects.push('เกจวัดแรงดันบกพร่อง 🔴');
  if (!payload.safetyPinOk) defects.push('สลักล็อกชำรุด/หาย 🔴');
  if (!payload.nozzleOk) defects.push('สายฉีดชำรุดอุดตัน 🔴');
  if (!payload.physicalStatusOk) defects.push('ตัวถังบุบ/เป็นสนิม 🔴');
  
  var message = '\n⚠️ แจ้งเหตุถังดับเพลิงเกิดปัญหาขัดข้อง!' +
                '\n[บริษัท ซีพีแรม จำกัด (สุราษฎร์ธานี)]' +
                '\n-----------------------------------' +
                '\nรหัสถัง: ' + payload.extId +
                '\nสถานที่ติดตั้ง: ' + (payload.location || '-') +
                '\nประเภทอุปกรณ์: ' + (payload.type || '-') +
                '\nผู้ทำรายงาน: ' + (payload.inspectorName || 'พนักงาน') +
                '\n\nรายละเอียดเหตุขัดข้อง:' +
                '\n- ' + defects.join('\n- ') +
                '\n\nหมายเหตุ: ' + (payload.remarks || 'ไม่มี') +
                '\nระดับความเร่งด่วน: เฝ้าระวังสูงสุด 🚨' +
                '\n-----------------------------------' +
                '\n*โปรดส่งทีมบำรุงรักษาเข้าหน้างานทันที เพื่อความปลอดภัยของโรงผลิต*';
                
  try {
    UrlFetchApp.fetch('https://notify-api.line.me/api/notify', {
      method: "post",
      headers: {
        "Authorization": "Bearer " + token
      },
      payload: {
        "message": message
      }
    });
  } catch (e) {
    Logger.log('ล้มเหลวในการส่ง LINE Notify: ' + e.toString());
  }
}

/**
 * อัปเดตเก็บค่า LINE Notify Token
 */
function saveLineToken(token) {
  PropertiesService.getScriptProperties().setProperty('LINE_TOKEN', token);
  return 'บันทึกไลน์โธเค็นเรียบร้อย!';
}

function getLineToken() {
  return PropertiesService.getScriptProperties().getProperty('LINE_TOKEN') || '';
}
