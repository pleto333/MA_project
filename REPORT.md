# 모바일 앱 프로그래밍 최종과제 보고서

## 1. 프로젝트 개요

앱 이름은 **FocusBloom**이다. FocusBloom은 사용자가 학습할 일을 등록하고, 선택한 할 일에 대해 집중 타이머를 실행하며, 완료된 세션 기록을 바탕으로 학습 통계와 추천 집중 시간을 보여주는 Kotlin Android 앱이다.

단순한 할 일 목록 앱에서 끝나지 않도록, 본 프로젝트는 **타이머**, **로컬 저장**, **통계 계산**, **추천 알고리즘**, **배지 기능**을 함께 구현했다. 이를 통해 사용자는 오늘 해야 할 일을 관리하면서 실제 집중 시간도 기록할 수 있다.

## 2. 핵심 기능 목록

본 앱의 핵심 기능은 다음과 같다.

1. **화면 구성 기능**
   - Kotlin 코드에서 직접 화면을 구성한다.
   - 타이머 영역, 할 일 입력 영역, 할 일 목록 영역, 성과 분석 영역으로 나뉜다.

2. **할 일 등록 기능**
   - 사용자가 할 일 제목과 집중 시간을 입력할 수 있다.
   - 입력한 할 일은 앱 내부 목록에 추가된다.

3. **집중 타이머 기능**
   - 선택한 할 일의 목표 시간만큼 카운트다운을 실행한다.
   - 남은 시간을 mm:ss 형식으로 표시하고 진행률 바를 갱신한다.

4. **세션 완료 처리 기능**
   - 타이머가 끝나면 선택된 할 일을 완료 상태로 바꾼다.
   - 완료 세션 수와 누적 집중 시간을 저장한다.

5. **로컬 데이터 저장 기능**
   - 앱을 종료해도 할 일 목록과 학습 기록이 유지된다.
   - SharedPreferences와 JSON을 사용한다.

6. **성과 분석 기능**
   - 완료 세션 수, 누적 집중 시간, 평균 세션, 연속 학습일을 보여준다.

7. **추천 집중 시간 기능**
   - 남은 할 일 수와 기존 평균 집중 시간을 기준으로 다음 집중 시간을 추천한다.

8. **배지 기능**
   - 일정 조건을 만족하면 성취 배지 문구를 보여준다.

## 3. 주요 코드 파일

핵심 코드는 다음 파일에 들어 있다.

```text
app/src/main/java/com/example/malast_project/MainActivity.kt
```

이 파일 하나에 화면 구성, 타이머 동작, 할 일 관리, 데이터 저장, 통계 계산이 모두 구현되어 있다.

## 4. 기능별 코드 설명

### 4.1 앱 시작 및 초기화

관련 코드:

```text
MainActivity.kt 40~47줄: onCreate()
```

`onCreate()`는 앱이 처음 실행될 때 호출된다. 이 함수에서는 `SharedPreferences`를 준비하고, 저장된 할 일 목록을 불러온 뒤, 저장된 데이터가 없으면 기본 예시 할 일을 생성한다. 이후 `buildUi()`로 화면을 만들고 `refresh()`로 화면에 데이터를 표시한다.

주요 흐름은 다음과 같다.

1. `prefs = getSharedPreferences(...)`로 저장소 준비
2. `loadTasks()`로 기존 할 일 불러오기
3. 할 일이 없으면 `seedTasks()`로 예시 데이터 생성
4. `buildUi()`로 화면 구성
5. `refresh()`로 화면 갱신

### 4.2 화면 구성 기능

관련 코드:

```text
MainActivity.kt 57~104줄: buildUi()
MainActivity.kt 292~336줄: card(), row(), text(), input(), primaryButton(), secondaryButton()
```

`buildUi()`는 앱 화면 전체를 만드는 함수이다. XML 레이아웃을 사용하지 않고 Kotlin 코드로 `ScrollView`, `LinearLayout`, `TextView`, `Button`, `EditText`, `ProgressBar`를 직접 생성한다.

화면은 다음 영역으로 구성된다.

- 앱 제목 영역: `FocusBloom` 제목 표시
- 타이머 카드: 현재 집중 시간과 진행률 표시
- 입력 카드: 할 일 제목과 집중 시간 입력
- 목록 카드: 등록된 할 일 목록 표시
- 통계 카드: 완료 세션, 누적 시간, 평균 시간, 배지 표시

`card()`, `row()`, `text()` 같은 보조 함수는 반복되는 UI 코드를 줄이기 위해 만들었다. 예를 들어 `card()`는 카드 형태의 `LinearLayout`을 만들고, `primaryButton()`은 주요 버튼 스타일을 적용한다.

### 4.3 할 일 등록 기능

관련 코드:

```text
MainActivity.kt 107~123줄: addTask()
MainActivity.kt 339줄: FocusTask 데이터 클래스
```

`addTask()`는 사용자가 입력한 할 일을 목록에 추가하는 함수이다. 사용자가 제목을 입력하지 않으면 Toast 메시지로 안내하고, 정상 입력이면 `FocusTask` 객체를 생성해 `tasks` 리스트에 추가한다.

`FocusTask`는 하나의 할 일을 표현하는 데이터 클래스이다.

```kotlin
data class FocusTask(
    val title: String,
    val minutes: Int,
    var done: Boolean = false
)
```

각 할 일은 제목, 목표 집중 시간, 완료 여부를 가진다. 할 일이 추가되면 `saveTasks()`로 저장하고, `refresh()`로 화면을 다시 그린다.

### 4.4 집중 시간 입력 제한

관련 코드:

```text
MainActivity.kt 123줄: parseMinutes()
```

`parseMinutes()`는 사용자가 입력한 집중 시간을 숫자로 변환한다. 숫자가 아니면 추천 시간을 사용하고, 숫자라면 `coerceIn(5, 90)`을 통해 최소 5분, 최대 90분으로 제한한다.

이 처리를 넣은 이유는 0분이나 지나치게 큰 숫자처럼 앱 사용에 적절하지 않은 값이 들어가는 것을 막기 위해서이다.

### 4.5 집중 타이머 기능

관련 코드:

```text
MainActivity.kt 125~146줄: startTimer()
MainActivity.kt 206~212줄: updateTimerView()
```

`startTimer()`는 Android의 `CountDownTimer`를 사용해 카운트다운을 시작한다. 타이머가 실행되는 동안 `onTick()`이 1초마다 호출되고, 이때 남은 시간이 갱신된다.

`updateTimerView()`는 남은 시간을 화면에 표시하는 함수이다. 남은 시간을 초 단위로 계산한 뒤 `mm:ss` 형식으로 보여준다. 또한 전체 시간 대비 진행률을 계산해서 `ProgressBar`에 반영한다.

즉, 타이머 기능은 다음 두 부분으로 나뉜다.

- `startTimer()`: 실제 타이머 실행
- `updateTimerView()`: 화면에 남은 시간과 진행률 표시

### 4.6 타이머 초기화 기능

관련 코드:

```text
MainActivity.kt 148~154줄: resetTimer()
```

`resetTimer()`는 실행 중인 타이머를 멈추고, 선택된 할 일의 목표 시간으로 남은 시간을 다시 설정한다. 사용자가 다른 할 일을 선택했을 때도 이 함수가 호출되어 타이머 시간이 새로 맞춰진다.

### 4.7 세션 완료 처리 기능

관련 코드:

```text
MainActivity.kt 156~168줄: completeSession()
```

`completeSession()`은 타이머가 끝났을 때 실행된다. 선택된 할 일을 완료 상태로 바꾸고, 누적 집중 시간과 완료 세션 수를 증가시킨다.

이 함수에서 처리하는 내용은 다음과 같다.

- 선택된 할 일의 `done` 값을 `true`로 변경
- `saveTasks()`로 할 일 목록 저장
- `updateStreak()`로 연속 학습일 갱신
- 누적 집중 시간 저장
- 완료 세션 수 저장
- 완료 Toast 메시지 표시

### 4.8 화면 갱신 및 통계 표시

관련 코드:

```text
MainActivity.kt 170~204줄: refresh()
```

`refresh()`는 현재 앱 데이터를 화면에 다시 표시하는 함수이다. 할 일이 추가되거나 선택이 바뀌거나 타이머가 끝났을 때 호출된다.

이 함수는 다음 내용을 처리한다.

- 기존 할 일 목록 UI 삭제
- `tasks` 리스트를 반복하면서 할 일 목록 다시 생성
- 선택 버튼 클릭 이벤트 연결
- 남은 할 일 수 계산
- 완료 세션 수, 누적 집중 시간, 연속 학습일 불러오기
- 추천 집중 시간 표시
- 통계와 배지 문구 표시

이 함수 덕분에 내부 데이터가 바뀔 때마다 화면도 최신 상태로 유지된다.

### 4.9 추천 집중 시간 기능

관련 코드:

```text
MainActivity.kt 214~225줄: recommendMinutes()
```

`recommendMinutes()`는 앱의 독창적인 기능 중 하나이다. 사용자의 현재 상태를 보고 다음 집중 시간을 추천한다.

추천 규칙은 다음과 같다.

- 남은 할 일이 5개 이상이면 15분 추천
- 평균 집중 시간이 35분 이상이면 30분 추천
- 완료 세션이 3회 이상이면 25분 추천
- 위 조건에 해당하지 않으면 20분 추천

이 기능은 복잡한 인공지능 모델은 아니지만, 사용자의 기록을 바탕으로 개인화된 추천을 제공한다는 점에서 단순 타이머 앱과 차별화된다.

### 4.10 평균 시간, 남은 할 일, 배지 계산

관련 코드:

```text
MainActivity.kt 227~236줄: firstOpenTask(), countOpenTasks(), averageMinutes(), badgeText()
```

이 부분은 통계와 보조 계산을 담당한다.

- `firstOpenTask()`: 아직 완료되지 않은 첫 번째 할 일을 찾는다.
- `countOpenTasks()`: 완료되지 않은 할 일 개수를 계산한다.
- `averageMinutes()`: 누적 집중 시간을 완료 세션 수로 나누어 평균 집중 시간을 계산한다.
- `badgeText()`: 조건에 맞는 배지 문구를 반환한다.

배지는 사용자의 성취감을 높이기 위한 기능이다. 예를 들어 3일 연속 학습, 누적 2시간 집중, 세션 3회 완료 같은 조건을 만족하면 다른 문구가 표시된다.

### 4.11 연속 학습일 계산

관련 코드:

```text
MainActivity.kt 238~252줄: updateStreak(), isYesterday()
```

`updateStreak()`는 오늘 날짜와 마지막 학습 날짜를 비교해 연속 학습일을 계산한다. 날짜는 `yyyy-MM-dd` 형식으로 저장한다.

`isYesterday()`는 마지막 학습 날짜가 어제인지 확인하는 함수이다. 어제라면 streak 값을 1 증가시키고, 어제가 아니라면 다시 1일부터 시작한다.

### 4.12 로컬 데이터 저장 및 불러오기

관련 코드:

```text
MainActivity.kt 254~270줄: loadTasks()
MainActivity.kt 272~282줄: saveTasks()
MainActivity.kt 341~348줄: companion object 저장 키
```

앱 데이터는 `SharedPreferences`에 저장된다. 할 일 목록은 여러 개의 데이터를 포함하므로 `JSONArray`와 `JSONObject`를 사용해 문자열 형태로 변환한 뒤 저장한다.

`saveTasks()`는 현재 할 일 목록을 JSON 배열로 만들어 저장한다. `loadTasks()`는 저장된 JSON 문자열을 다시 읽어서 `FocusTask` 객체 목록으로 복원한다.

저장되는 주요 값은 다음과 같다.

- `tasks_json`: 할 일 목록
- `total_minutes`: 누적 집중 시간
- `completed_sessions`: 완료 세션 수
- `streak`: 연속 학습일
- `last_day`: 마지막 학습 날짜

### 4.13 기본 예시 데이터 생성

관련 코드:

```text
MainActivity.kt 284~290줄: seedTasks()
```

앱을 처음 실행했을 때 아무 할 일도 없으면 `seedTasks()`가 기본 예시 할 일을 추가한다. 사용자가 앱을 처음 켜도 빈 화면이 아니라 사용 예시를 바로 볼 수 있도록 하기 위한 기능이다.

## 5. 사용한 Android/Kotlin 요소

본 프로젝트에서 사용한 주요 요소는 다음과 같다.

- `Activity`: 앱의 기본 화면 역할
- `LinearLayout`, `ScrollView`, `TextView`, `Button`, `EditText`: 화면 구성
- `ProgressBar`: 타이머 진행률 표시
- `CountDownTimer`: 카운트다운 타이머 구현
- `SharedPreferences`: 로컬 데이터 저장
- `JSONArray`, `JSONObject`: 할 일 목록을 JSON 형태로 저장
- `Toast`: 사용자 안내 메시지 표시
- Kotlin `data class`: 할 일 데이터를 간결하게 표현
- Kotlin `when`: 추천 시간과 배지 조건 처리

## 6. 어려웠던 점과 해결 방법

첫 번째 어려움은 타이머 상태와 선택된 할 일을 함께 관리하는 것이었다. 이를 해결하기 위해 `selectedIndex`, `activeMinutes`, `remainingMs`, `running` 변수를 분리해서 관리했다.

두 번째 어려움은 여러 개의 할 일을 앱 종료 후에도 유지하는 것이었다. 단순 문자열 하나로는 목록 저장이 어렵기 때문에 `JSONArray`와 `JSONObject`를 사용해 할 일 목록을 구조적으로 저장했다.

세 번째 어려움은 추천 기능을 과제 규모에 맞게 구현하는 것이었다. 복잡한 AI 모델 대신 규칙 기반 알고리즘을 사용하여, 코드가 이해하기 쉽고 보고서에서 설명하기 쉬운 방식으로 만들었다.

## 7. 독창적인 부분

FocusBloom은 단순히 할 일을 체크하는 앱이 아니라, 사용자의 학습 기록을 바탕으로 다음 집중 시간을 추천한다. 또한 완료 세션 수, 누적 집중 시간, 연속 학습일, 배지 기능을 함께 제공하여 사용자가 자신의 학습 습관을 확인할 수 있도록 했다.

## 8. AI 활용 명시

본 프로젝트는 앱 주제 선정, 코드 작성, 보고서 구조화 과정에서 AI 도구의 도움을 받았다. 최종 제출 전에는 Kotlin 코드와 빌드를 직접 확인했으며, Android Studio 프로젝트에서 실행 가능한 형태로 정리했다.