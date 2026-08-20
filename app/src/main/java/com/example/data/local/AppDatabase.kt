package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [DiagnosticHistory::class, RagDocument::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun carDiagDao(): CarDiagDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "car_diagnostic_db"
                )
                .addCallback(DatabaseCallback(context))
                .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(
            private val context: Context
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    CoroutineScope(Dispatchers.IO).launch {
                        populateInitialData(database.carDiagDao())
                    }
                }
            }

            private suspend fun populateInitialData(dao: CarDiagDao) {
                dao.insertHistory(
                    DiagnosticHistory(
                        title = "파워 테일게이트 래치 점검",
                        dtcCode = "B24BC96",
                        symptomText = "트렁크 테일게이트 래치가 닫히지 않고 멈춤 현상 발생.",
                        summary = "통합형 래치 스위치 조합 회로 신호 이상이 감지되었습니다.",
                        fullAnalysis = "B24BC96 코드는 파워 테일게이트 통합형 래치 스위치의 접점 신호 오작동을 나타냅니다. 후방 우측 트렁크 배선의 핀 텐션 점검이 필요합니다.",
                        checksListJson = "후방 우측 트렁크 배선 커넥터 점검|래치 모터 작동 테스트|테일게이트 릴레이 전압 측정",
                        solutionText = "후방 우측 트렁크 배선 커넥터 단자를 세척하고 핀 텐션을 재조정하십시오.",
                        warningText = "방치 시 주행 중 트렁크 열림 위험이 있으므로 즉시 점검을 권장합니다.",
                        timestamp = System.currentTimeMillis() - 86400000L * 2,
                        statusType = "WARNING"
                    )
                )

                dao.insertHistory(
                    DiagnosticHistory(
                        title = "에어컨 효율 진단",
                        dtcCode = "B124111",
                        symptomText = "에어컨 바람 세기가 약하고 냉기가 적게 나옴.",
                        summary = "에어컨 블로워 모터 및 냉매 상태 정상",
                        fullAnalysis = "블로워 모터 출력 및 에바포레이터 온도 센서 정상 범위. 에어컨 필터 교체 권장.",
                        checksListJson = "캐빈 에어컨 필터 오염도 검사|냉매 압력 밸브 누설 검사",
                        solutionText = "실내 캐빈 에어컨 필터 청소 및 교체를 진행하세요.",
                        warningText = "필터 막힘 시 블로워 모터 과열의 원인이 됩니다.",
                        timestamp = System.currentTimeMillis() - 86400000L * 7,
                        statusType = "NORMAL"
                    )
                )
            }
        }
    }
}
