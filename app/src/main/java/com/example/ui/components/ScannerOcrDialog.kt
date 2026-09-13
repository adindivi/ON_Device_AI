package com.example.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import com.example.backend.OcrEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 갤러리 EXIF 회전 보정 및 카메라 100% 구글 ML Kit OCR 비전 엔진 연동 스캐너
 */
@Composable
fun ScannerOcrDialog(
    onDismiss: () -> Unit,
    onSelectCode: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var scanJob by remember { mutableStateOf<Job?>(null) }

    var isScanning by remember { mutableStateOf(false) }
    var hasScanned by remember { mutableStateOf(false) }
    var capturedImage by remember { mutableStateOf<Bitmap?>(null) }
    var imageSourceTitle by remember { mutableStateOf("스캐너 촬영 화면") }

    val detectedCodes = remember { mutableStateListOf<String>() }

    // 비트맵에서 Google ML Kit 비전 엔진으로 실제 DTC 고장 코드를 추출하는 처리 함수
    fun runRealOcrProcessing(bitmap: Bitmap, sourceName: String) {
        capturedImage = bitmap
        imageSourceTitle = sourceName
        isScanning = true
        hasScanned = true
        scanJob?.cancel()

        scanJob = scope.launch(Dispatchers.Default) {
            // Google ML Kit 오프라인 비전 텍스트 심층 분석
            val ocrResult = OcrEngine.analyzeBitmapImage(bitmap)

            withContext(Dispatchers.Main) {
                detectedCodes.clear()
                if (ocrResult.dtcCodes.isNotEmpty()) {
                    detectedCodes.addAll(ocrResult.dtcCodes)
                    Toast.makeText(context, "OCR 해독 완료: ${detectedCodes.size}개 코드", Toast.LENGTH_SHORT).show()
                } else if (ocrResult.rawText.isBlank()) {
                    android.util.Log.w("ScannerOcrDialog", "ML Kit OCR: Blank text detected. Rejecting.")
                    Toast.makeText(context, "글자가 흐리거나 초점이 맞지 않습니다. 다시 찰칵! 찍어주세요.", Toast.LENGTH_LONG).show()
                } else {
                    android.util.Log.w("ScannerOcrDialog", "ML Kit OCR: No DTC codes detected in image ($imageSourceTitle)")
                    Toast.makeText(context, "고장 코드가 안 보입니다! 위치를 맞춰 다시 찍어주세요.", Toast.LENGTH_LONG).show()
                }

                isScanning = false
            }
        }
    }

    var showLiveCamera by remember { mutableStateOf(false) }

    // Camera Permission Launcher
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            showLiveCamera = true
        } else {
            Toast.makeText(context, "카메라 권한 필요", Toast.LENGTH_SHORT).show()
        }
    }

    // Gallery Image Picker Launcher (EXIF 회전 보정 연동)
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            try {
                val correctedBitmap = decodeGalleryUriWithExifCorrection(context, uri)
                if (correctedBitmap != null) {
                    runRealOcrProcessing(correctedBitmap, "갤러리 선택 이미지")
                } else {
                    Toast.makeText(context, "이미지 디코딩 실패", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "이미지 로드 오류: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun startCameraCapture() {
        if (isScanning) return
        val hasCameraPermission = androidx.core.content.ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.CAMERA
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        if (hasCameraPermission) {
            showLiveCamera = true
        } else {
            cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
        }
    }

    fun startGalleryPicker() {
        if (isScanning) return
        galleryLauncher.launch("image/*")
    }

    DisposableEffect(Unit) {
        onDispose {
            scanJob?.cancel()
        }
    }

    // Laser beam animation
    val transition = rememberInfiniteTransition(label = "laser")
    val laserOffset by transition.animateFloat(
        initialValue = 0f,
        targetValue = 120f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laser_y"
    )

    if (showLiveCamera) {
        CameraXLiveScanner(
            onBitmapCaptured = { bitmap ->
                runRealOcrProcessing(bitmap, "라이브 스캐너")
                showLiveCamera = false
            },
            onClose = {
                showLiveCamera = false
            }
        )
    } else {
        Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(24.dp)),
            color = Color.White,
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header: Toss Clean Style
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFF1F5F9)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCodeScanner,
                                contentDescription = "Scanner",
                                tint = Color(0xFF191F28),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "DTC 스캐너",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF191F28)
                                )
                            )
                            Text(
                                text = "카메라 / 이미지 AI 고장코드 인식",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color(0xFF64748B),
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF1F5F9))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color(0xFF64748B),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action Buttons Row: Camera Capture & Image Gallery Upload
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Primary Action: Camera Capture (Toss Black)
                    Button(
                        onClick = { startCameraCapture() },
                        enabled = !isScanning,
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("btn_capture_camera"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF191F28),
                            contentColor = Color.White
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "Camera",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "카메라 촬영", fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                    }

                    // Secondary Action: Gallery Upload (Soft Gray)
                    Button(
                        onClick = { startGalleryPicker() },
                        enabled = !isScanning,
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("btn_upload_image"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFF1F5F9),
                            contentColor = Color(0xFF334155)
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Image,
                            contentDescription = "Gallery",
                            tint = Color(0xFF334155),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "이미지 업로드",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF334155),
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (isScanning) {
                    // Scanning Viewfinder
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF1E293B))
                                .border(2.dp, Color(0xFF334155), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (capturedImage != null) {
                                Image(
                                    bitmap = capturedImage!!.asImageBitmap(),
                                    contentDescription = "Captured Image",
                                    modifier = Modifier.fillMaxWidth(),
                                    contentScale = ContentScale.Crop,
                                    alpha = 0.6f
                                )
                            }

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "[ $imageSourceTitle ]",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = "RUNNING GOOGLE ML KIT AI VISION...",
                                    color = Color(0xFF34D399),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }

                            // Laser Beam Line
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(3.dp)
                                    .offset(y = laserOffset.dp - 60.dp)
                                    .background(Color(0xFF34D399))
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "이미지에서 DTC 고장 코드를 정밀 분석 중입니다...",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF334155)
                            )
                        )
                        Text(
                            text = "OcrEngine EXIF 회전 보정 및 ML Kit 딥러닝 비전 구동 중",
                            style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF64748B))
                        )
                    }
                } else if (!hasScanned) {
                    // Initial Ready Waiting Box (Toss Soft Tone)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFFF8FAFC))
                            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(14.dp))
                            .padding(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFF1F5F9)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.QrCodeScanner,
                                    contentDescription = "Ready",
                                    tint = Color(0xFF191F28),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Google ML Kit OCR 준비 완료",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF1E293B)
                                    )
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = "카메라로 촬영하거나 사진을 업로드하면 고장 코드를 자동 인식합니다.",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = Color(0xFF64748B),
                                        fontSize = 11.sp
                                    )
                                )
                            }
                        }
                    }
                } else if (detectedCodes.isNotEmpty()) {
                    // Result Detected Code Selection List
                    Column {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFECFDF5))
                                .border(1.dp, Color(0xFFA7F3D0), RoundedCornerShape(10.dp))
                                .padding(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Detected",
                                    tint = Color(0xFF10B981),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "${detectedCodes.size}개의 실제 DTC 고장 코드가 추출되었습니다.",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF047857)
                                        )
                                    )
                                    Text(
                                        text = "출처: $imageSourceTitle (Google ML Kit EXIF 회전 보정 파싱)",
                                        fontSize = 10.sp,
                                        color = Color(0xFF059669)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "진단창에 입력할 코드를 클릭하세요.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color(0xFF64748B),
                                fontWeight = FontWeight.Medium
                            )
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        // [다중 고장코드 근본 원인 통합 진단 버튼]
                        // 스캔된 DTC가 2개 이상일 때 나타나며, ISO-ROOT 접두사를 부착하여 AI가 연관 공통 원인을 분석하도록 유도
                        if (detectedCodes.size > 1) {
                            Button(
                                onClick = { 
                                    val rootCauseCode = "ISO-ROOT: " + detectedCodes.joinToString(",")
                                    onSelectCode(rootCauseCode) 
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("🔍 다중 코드 근본 원인(Root Cause) 통합 진단", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(detectedCodes) { code ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onSelectCode(code) }
                                        .testTag("ocr_code_item_$code"),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = Color.White
                                    ),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
                                            .padding(14.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = code,
                                                style = MaterialTheme.typography.titleMedium.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF1E293B)
                                                )
                                            )
                                            Text(
                                                text = "Google ML Kit AI 비전 인지 고장 코드",
                                                style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF94A3B8))
                                            )
                                        }

                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFFDBE1FF)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                                contentDescription = "Select",
                                                tint = Color(0xFF2563EB),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Result: No DTC Codes Detected
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFFF8FAFC))
                            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(14.dp))
                            .padding(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFFFBEB)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = "No Code",
                                    tint = Color(0xFFD97706),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "고장 코드가 감지되지 않았습니다",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF1E293B)
                                    )
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = "텍스트가 선명하게 보이도록 수평 각도에서 다시 촬영하거나 다른 이미지를 선택해 주세요.",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = Color(0xFF64748B),
                                        fontSize = 11.sp
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
}
