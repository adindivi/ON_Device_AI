package com.example.backend

import android.content.Context
import org.json.JSONObject
import java.io.File

object MappingDictionary {

    const val MAPPING_FILENAME = "mapping_dictionary.json"

    val DEFAULT_COMPONENTS = mapOf(
        "휠속도" to "휠속도센서",
        "속도센서" to "휠속도센서",
        "휠스피드" to "휠속도센서",
        "블로워" to "블로워 모터",
        "히터" to "블로워 모터",
        "히터모터" to "블로워 모터",
        "스위치" to "스위치",
        "브레이크 스위치" to "스위치",
        "AVH 스위치" to "스위치",
        "주차스위치" to "스위치",
        "초음파" to "초음파센서",
        "주차센서" to "초음파센서",
        "전방초음파" to "초음파센서",
        "후방초음파" to "초음파센서",
        "브레이크액" to "브레이크액",
        "브레이크 오일" to "브레이크액",
        "오일레벨" to "브레이크액",
        "조향" to "조향각센서",
        "조향각" to "조향각센서",
        "SAS" to "조향각센서",
        "ECU" to "엔진제어모듈(ECU)",
        "엔진" to "엔진제어모듈(ECU)",
        "MCU" to "엔진제어모듈(ECU)",
        "VCU" to "엔진제어모듈(ECU)",
        "HCU" to "엔진제어모듈(ECU)",
        "FCU" to "엔진제어모듈(ECU)",
        "EPB" to "EPB 모듈",
        "주차브레이크" to "EPB 모듈",
        "액추에이터" to "액추에이터",
        "모터액추에이터" to "액추에이터",
        "온도센서" to "온도센서",
        "외기온도" to "온도센서",
        "인카온도" to "온도센서",
        "증발기센서" to "온도센서",
        "덕트센서" to "온도센서",
        "습도센서" to "습도센서",
        "습도" to "습도센서",
        "PTC" to "PTC 히터",
        "PTC히터" to "PTC 히터",
        "BLDC" to "HVAC BLDC 모터",
        "BLDC모터" to "HVAC BLDC 모터",
        "릴레이" to "고전압 릴레이",
        "릴레이융착" to "고전압 릴레이",
        "인터록" to "고전압 릴레이",
        "인버터" to "인버터",
        "커패시터" to "인버터",
        "BMS" to "BMS (배터리 관리 시스템)",
        "배터리제어" to "BMS (배터리 관리 시스템)",
        "퓨즈" to "메인 퓨즈",
        "메인퓨즈" to "메인 퓨즈",
        "에어백" to "에어백 제어 모듈",
        "ACU" to "에어백 제어 모듈",
        "윈도우" to "윈도우 모터",
        "파워윈도우" to "윈도우 모터",
        "요레이트" to "요레이트 센서",
        "가속도센서" to "요레이트 센서",
        "YRS" to "요레이트 센서",
        "MDPS" to "MDPS (전동식 파워 스티어링)",
        "조향모터" to "MDPS (전동식 파워 스티어링)",
        "압력센서" to "압력센서",
        "압력" to "압력센서",
        "레버" to "전자식 변속 레버",
        "변속레버" to "전자식 변속 레버",
        "E-Shifter" to "전자식 변속 레버",
        "서보모터" to "서보 모터",
        "서보" to "서보 모터",
        "엔코더" to "엔코더 센서",
        "레이더" to "레이더 센서",
        "전방레이더" to "레이더 센서",
        "후측방레이더" to "레이더 센서",
        "카메라" to "카메라 모듈",
        "광각카메라" to "카메라 모듈",
        "SVM" to "카메라 모듈",
        "GPS" to "GPS 안테나",
        "GPS안테나" to "GPS 안테나",
        "마이크" to "마이크",
        "스피커" to "스피커",
        "USB" to "USB 단자",
        "USB포트" to "USB 단자",
        "냉각팬" to "냉각 팬",
        "팬" to "냉각 팬",
        "LDC" to "LDC (저전압 DC/DC 컨버터)",
        "컨버터" to "LDC (저전압 DC/DC 컨버터)",
        "OBC" to "OBC (온보드 충전기)",
        "충전기" to "OBC (온보드 충전기)",
        "V2L" to "V2L/V2G 모듈",
        "V2G" to "V2L/V2G 모듈",
        "도어" to "도어 제어 모듈",
        "슬라이딩" to "도어 제어 모듈",
        "슬라이딩도어" to "도어 제어 모듈",
        "파워슬라이딩도어" to "도어 제어 모듈",
        "PSD" to "도어 제어 모듈",
        "도어모듈" to "도어 제어 모듈",
        "도어핸들" to "도어핸들 모듈",
        "스마트키" to "스마트키 제어 모듈(SMK)",
        "SMK" to "스마트키 제어 모듈(SMK)",
        "TPMS" to "TPMS (타이어 공기압 감지 시스템)",
        "공기압센서" to "TPMS (타이어 공기압 감지 시스템)",
        "타이어센서" to "TPMS (타이어 공기압 감지 시스템)",
        "HOD" to "HOD (핸들 잡음 감지 센서)",
        "그립센서" to "HOD (핸들 잡음 감지 센서)",
        "EBB" to "EBB (전자식 브레이크 부스터)",
        "E-부스터" to "EBB (전자식 브레이크 부스터)",
        "솔레노이드" to "솔레노이드 밸브",
        "점화" to "점화 코일/플러그",
        "플러그" to "점화 플러그",
        "코일" to "점화 코일",
        "인젝터" to "연료 인젝터"
    )

    val DEFAULT_LOCATIONS = mapOf(
        "전방" to "전방", "앞쪽" to "전방", "앞" to "전방", "front" to "전방", "프론트" to "전방", "fr" to "전방",
        "앞범퍼" to "전방", "범퍼" to "전방", "헤드라이트" to "전방", "라이트" to "전방", "그릴" to "전방", "라디에이터그릴" to "전방", "fnt" to "전방", "forward" to "전방",
        "후방" to "후방", "뒤쪽" to "후방", "뒤" to "후방", "후방 좌우" to "후방", "rr" to "후방", "리어" to "후방",
        "뒷범퍼" to "후방", "백범퍼" to "후방", "트렁크" to "후방", "러기지" to "후방", "후미" to "후방", "후미등" to "후방", "데일" to "후방", "back" to "후방", "rear_side" to "후방",
        "하부" to "하부", "바닥" to "하부", "밑" to "하부", "하단" to "하부", "언더" to "하부",
        "바닥면" to "하부", "하판" to "하부", "머플러" to "하부", "배기" to "하부", "촉매" to "하부", "샤시" to "하부", "하체" to "하부", "멤버" to "하부", "bottom" to "하부", "low" to "하부", "lower" to "하부",
        "엔진룸" to "PE룸", "본넷" to "PE룸", "보닛" to "PE룸", "후드" to "PE룸", "eng" to "PE룸", "engine_room" to "PE룸", "격벽" to "PE룸", "카울" to "PE룸", "pe" to "PE룸", "pe룸" to "PE룸",
        "실내" to "실내", "대시보드" to "실내", "글로브박스" to "실내", "다시방" to "실내", "콘솔" to "실내", "센터페시아" to "실내", "클래시패드" to "실내", "c/pad" to "실내", "cpad" to "실내", "시트" to "실내", "의자" to "실내", "핸들" to "실내", "cabin" to "실내", "inside" to "실내", "interior" to "실내",
        "좌" to "좌측", "좌측" to "좌측", "왼쪽" to "좌측", "운전석측" to "좌측", "LH" to "좌측", "left" to "좌측",
        "우" to "우측", "우측" to "우측", "오른쪽" to "우측", "동승석측" to "우측", "RH" to "우측", "right" to "우측"
    )

    private var componentsMap: MutableMap<String, String> = DEFAULT_COMPONENTS.toMutableMap()
    private var locationsMap: MutableMap<String, String> = DEFAULT_LOCATIONS.toMutableMap()

    fun loadMappingDictionary(dbDirectory: File): Pair<Map<String, String>, Map<String, String>> {
        val mappingFile = File(dbDirectory, MAPPING_FILENAME)
        if (mappingFile.exists()) {
            try {
                val jsonStr = mappingFile.readText(Charsets.UTF_8)
                val json = JSONObject(jsonStr)
                if (json.has("components")) {
                    val compJson = json.getJSONObject("components")
                    val newComps = mutableMapOf<String, String>()
                    compJson.keys().forEach { key ->
                        newComps[key] = compJson.getString(key)
                    }
                    componentsMap = newComps
                }
                if (json.has("locations")) {
                    val locJson = json.getJSONObject("locations")
                    val newLocs = mutableMapOf<String, String>()
                    locJson.keys().forEach { key ->
                        newLocs[key] = locJson.getString(key)
                    }
                    locationsMap = newLocs
                }
                return Pair(componentsMap, locationsMap)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Create mapping_dictionary.json if missing
        saveMappingDictionary(dbDirectory)
        return Pair(componentsMap, locationsMap)
    }

    private fun saveMappingDictionary(dbDirectory: File) {
        try {
            if (!dbDirectory.exists()) {
                dbDirectory.mkdirs()
            }
            val mappingFile = File(dbDirectory, MAPPING_FILENAME)
            val json = JSONObject()
            
            val compObj = JSONObject()
            componentsMap.forEach { (k, v) -> compObj.put(k, v) }
            json.put("components", compObj)

            val locObj = JSONObject()
            locationsMap.forEach { (k, v) -> locObj.put(k, v) }
            json.put("locations", locObj)

            mappingFile.writeText(json.toString(2), Charsets.UTF_8)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getComponents(): Map<String, String> = componentsMap
    fun getLocations(): Map<String, String> = locationsMap
}
