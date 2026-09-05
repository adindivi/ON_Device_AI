# System Rules (시스템 동작 규칙)

## 1. 질문 처리 규칙 (Question Mark Rule)
- **물음표('?')가 포함된 요청**: 코드 수정이나 파일 변경 작업을 **절대로 적용하지 마시고**, 오직 **중학생도 쉽게 이해할 수 있는 쉬운 설명만** 제공하십시오.
- **물음표('?')가 없는 요청**: 사용자가 명시적으로 수정을 요청하는 경우에만 코드 및 파일 변경 작업을 진행하십시오.

## 2. 온디바이스 AI 개발 원칙 (On-Device AI Rules)
- **원칙 1: 대용량 모델 저장소 격리 (Scoped Storage)**
  APK assets에 포함된 대용량 AI 모델(ONNX, GGUF, TFLite 등)을 런타임에 디스크로 추출할 때는, 외부 공용 저장소(`/storage/emulated/0`)를 절대 사용하지 말고 반드시 앱 전용 내부 저장소(`context.filesDir` 또는 `context.noBackupFilesDir`)에 저장해야 한다. (Android Scoped Storage 쓰기 거부 방지 및 파일 존재 시 중복 복사 건너뛰기 필수 적용)
- **원칙 2: 텐서 입출력 스펙 사전 검증 (Strict Tensor Signature)**
  임베딩 및 추론 엔진 코드 작성 전, 반드시 모델의 실제 입력 텐서 이름과 개수(Signature)를 사전에 검증(Python onnxruntime 또는 Netron 도구)해야 한다. 특히 RoBERTa/SBERT 계열 모델은 `token_type_ids`를 받지 않으므로, 불필요한 텐서 주입으로 인한 `OrtException` 런타임 추락을 원천 방지한다.
- **원칙 3: 무음 실패(Silent Catch Fallback) 금지 및 로깅 의무화**
  AI 모델 추론 실패 시 catch 블록으로 에러를 조용히 삼키고 가짜 데이터나 해시(Hash) 폴백으로 넘어가지 않도록 한다. 모델 로드 상태(`isReady`), 입력 텐서 형태, 추론 시간, 예외 발생 내용을 반드시 Logcat(`Log.e`/`Log.w`)에 명시적으로 기록하여 임베딩 엔진이 정상 가동 중인지 런타임에 즉시 판별할 수 있어야 한다.

