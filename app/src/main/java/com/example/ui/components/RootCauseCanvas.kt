package com.example.ui.components

import android.util.Log
import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONObject

@Composable
fun RootCauseGraphView(
    jsonGraphString: String,
    onFallback: () -> Unit
) {
    val context = LocalContext.current
    var isParsed by remember { mutableStateOf(false) }
    var rootCause by remember { mutableStateOf("") }
    var symptoms by remember { mutableStateOf<List<String>>(emptyList()) }
    var edges by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }

    LaunchedEffect(jsonGraphString) {
        val rawStr = jsonGraphString.substringAfter("[JSON_GRAPH_START]")
        val jsonStr = if (rawStr.contains("</think>")) {
            rawStr.substringAfterLast("</think>").trim()
        } else {
            rawStr.trim()
        }

        var parsedObj: JSONObject? = null

        // 1. 시도: 있는 그대로 파싱 시도 (스트리밍 완료 시 성공함)
        try {
            parsedObj = JSONObject(jsonStr)
        } catch (e: Exception) {
            // 2. 시도: 스트리밍 중이거나 잘린 경우, 마지막 } 기준으로 잘라서 강제 복구 시도
            val lastBraceIndex = jsonStr.lastIndexOf('}')
            if (lastBraceIndex != -1) {
                var cleanJson = jsonStr.substring(0, lastBraceIndex + 1)
                cleanJson = cleanJson.replace(Regex("'([^']+)'\\s*:"), "\"$1\":")

                val openBraces = cleanJson.count { it == '{' } - cleanJson.count { it == '}' }
                val openBrackets = cleanJson.count { it == '[' } - cleanJson.count { it == ']' }

                cleanJson += "]".repeat(openBrackets.coerceAtLeast(0))
                cleanJson += "}".repeat(openBraces.coerceAtLeast(0))

                try {
                    parsedObj = JSONObject(cleanJson)
                } catch (e2: Exception) {
                    // 여전히 파싱 실패
                }
            }
        }

        if (parsedObj != null && parsedObj.has("root_cause")) {
            rootCause = parsedObj.optString("root_cause", "")
            val symptomsArray = parsedObj.optJSONArray("symptoms")
            val sympList = mutableListOf<String>()
            if (symptomsArray != null) {
                for (i in 0 until symptomsArray.length()) {
                    sympList.add(symptomsArray.optString(i))
                }
            }
            symptoms = sympList
            
            if (rootCause.isNotBlank()) {
                isParsed = true
            }
        } else {
            // 파싱 실패 시 바로 우회하지 않음!
            // LaunchedEffect는 jsonGraphString이 바뀔 때마다 취소되고 재시작되므로,
            // 2초 동안 추가 텍스트가 안 들어온다는 것은 스트리밍이 멈췄다는 뜻임 (진짜 실패/에러)
            kotlinx.coroutines.delay(2000)
            Log.e("RootCauseCanvas", "JSON stream stopped and parsing failed. Fallback triggered.")
            Toast.makeText(context, "트리 구조 생성에 실패하여 텍스트로 전환합니다.", Toast.LENGTH_LONG).show()
            onFallback()
        }
    }

    if (!isParsed) {
        Box(
            modifier = Modifier.fillMaxWidth().height(150.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "연쇄 고장 트리를 분석 중입니다...",
                color = Color(0xFF64748B),
                fontWeight = FontWeight.Medium
            )
        }
        return
    }

    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(250.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFF8FAFC))
            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale = (scale * zoom).coerceIn(0.5f, 3f)
                    offset += pan
                }
            }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offset.x,
                    translationY = offset.y
                )
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val canvasWidth = size.width
                val rootPos = Offset(canvasWidth / 2f, 40f.dp.toPx())
                val sympCount = symptoms.size
                val sympY = 160f.dp.toPx()
                
                symptoms.forEachIndexed { index, _ ->
                    val sympX = if (sympCount == 1) canvasWidth / 2f else {
                        val spacing = canvasWidth / (sympCount + 1)
                        spacing * (index + 1)
                    }
                    val sympPos = Offset(sympX, sympY)

                    val path = Path().apply {
                        moveTo(rootPos.x, rootPos.y + 20f.dp.toPx())
                        quadraticBezierTo(
                            rootPos.x, sympPos.y - 40f.dp.toPx(),
                            sympPos.x, sympPos.y - 20f.dp.toPx()
                        )
                    }
                    drawPath(
                        path = path,
                        color = Color(0xFFCBD5E1),
                        style = Stroke(width = 3f.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
            }
            
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = 20.dp)
                    .graphicsLayer(scaleX = pulseScale, scaleY = pulseScale)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFFFEF2F2))
                    .border(2.dp, Color(0xFFEF4444), RoundedCornerShape(16.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = "Root", tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = rootCause, color = Color(0xFF991B1B), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }

            val sympCount = symptoms.size
            if (sympCount > 0) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .offset(y = 140.dp)
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    symptoms.forEach { symp ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White)
                                .border(1.dp, Color(0xFF94A3B8), RoundedCornerShape(12.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(text = symp, color = Color(0xFF475569), fontWeight = FontWeight.Medium, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}
