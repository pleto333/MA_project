# FocusBloom

모바일 앱 프로그래밍 최종과제용 Kotlin Android 앱입니다. FocusBloom은 할 일을 등록하고, 집중 타이머로 학습 세션을 수행하며, 누적 기록을 바탕으로 다음 집중 시간을 추천하는 학습 루틴 관리 앱입니다.

## 주요 기능

- 할 일과 목표 집중 시간 등록
- 선택한 할 일 기준 카운트다운 타이머 실행
- 타이머 완료 시 할 일 완료 처리
- SharedPreferences와 JSON을 이용한 로컬 데이터 저장
- 완료 세션 수, 누적 집중 시간, 평균 세션, 연속 학습일 표시
- 남은 할 일 수와 평균 집중 시간에 따른 추천 시간 계산
- 조건 달성 시 배지 메시지 표시

## 기술 구성

- Language: Kotlin
- Platform: Android
- UI: Kotlin 코드 기반 동적 View 구성
- Storage: SharedPreferences, JSONArray, JSONObject
- Timer: CountDownTimer

## 실행 방법

Android Studio에서 아래 폴더를 열고 app 모듈을 실행합니다.

```text
C:\Users\user\AndroidStudioProjects\MAlast_project
```

명령어로 빌드할 경우:

```powershell
.\gradlew.bat assembleDebug
```

## 핵심 파일

```text
app/src/main/java/com/example/malast_project/MainActivity.kt
app/src/main/AndroidManifest.xml
app/src/main/res/drawable/card_bg.xml
app/src/main/res/drawable/primary_button.xml
app/src/main/res/drawable/secondary_button.xml
REPORT.md
```

## AI 활용 명시

본 과제는 앱 주제 선정, 코드 작성, 보고서 구조화 과정에서 AI 도구의 도움을 받았습니다. 최종 제출 시에는 코드와 기능을 직접 실행해 확인하고 제출합니다.