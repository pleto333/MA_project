package com.example.malast_project

import android.app.Activity
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.os.CountDownTimer
import android.text.InputType
import android.view.Gravity
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max
import kotlin.math.roundToInt

// FocusBloom 앱의 메인 화면과 핵심 기능을 모두 담당하는 Activity이다.
class MainActivity : Activity() {
    // SharedPreferences는 앱을 꺼도 유지되어야 하는 데이터를 저장하는 로컬 저장소이다.
    private lateinit var prefs: SharedPreferences

    // 아래 View 변수들은 화면을 다시 그리거나 값을 갱신할 때 사용된다.
    private lateinit var taskList: LinearLayout
    private lateinit var timerText: TextView
    private lateinit var statsText: TextView
    private lateinit var guideText: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var titleInput: EditText
    private lateinit var minutesInput: EditText

    // 할 일 목록과 타이머 상태를 저장하는 앱의 핵심 상태값이다.
    private val tasks = mutableListOf<FocusTask>()
    private var timer: CountDownTimer? = null
    private var selectedIndex = -1
    private var activeMinutes = 25
    private var remainingMs = 25 * 60 * 1000L
    private var running = false

    // 앱이 처음 실행될 때 저장 데이터 로드, 기본 데이터 생성, 화면 구성을 순서대로 처리한다.
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE)
        loadTasks()
        if (tasks.isEmpty()) seedTasks()
        buildUi()
        refresh()
    }

    // 화면이 종료될 때 실행 중인 타이머를 정리해 메모리 누수를 막는다.
    override fun onDestroy() {
        timer?.cancel()
        super.onDestroy()
    }

    // XML 레이아웃 대신 Kotlin 코드로 앱 화면 전체를 만든다.
    private fun buildUi() {
        // ScrollView를 사용해 작은 화면에서도 모든 영역을 스크롤로 볼 수 있게 한다.
        val scroll = ScrollView(this).apply { setBackgroundColor(Color.rgb(247, 244, 239)) }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(20), dp(18), dp(24))
        }
        scroll.addView(root)

        // 앱 제목과 간단한 설명을 화면 상단에 배치한다.
        root.addView(text("FocusBloom", 30, "#20312E", true))
        root.addView(text("집중 타이머와 학습 통계를 제공하는 루틴 관리 앱", 14, "#62706B"))

        // 타이머 카드: 추천 문구, 남은 시간, 진행률, 시작/초기화 버튼을 포함한다.
        val timerCard = card()
        root.addView(timerCard)
        timerCard.addView(text("오늘의 집중", 17, "#20312E", true))
        guideText = text("", 14, "#5F6864")
        timerCard.addView(guideText)
        timerText = text("25:00", 48, "#2F766D", true).apply { gravity = Gravity.CENTER }
        timerCard.addView(timerText)
        progressBar = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply { max = 1000 }
        timerCard.addView(progressBar, LinearLayout.LayoutParams(-1, dp(16)))

        val timerButtons = row()
        timerButtons.addView(primaryButton("시작").apply { setOnClickListener { startTimer() } }, weightParams())
        timerButtons.addView(secondaryButton("초기화").apply { setOnClickListener { resetTimer() } }, weightParams())
        timerCard.addView(timerButtons)

        // 입력 카드: 할 일 제목과 목표 집중 시간을 입력받는다.
        val inputCard = card()
        root.addView(inputCard)
        inputCard.addView(text("할 일 추가", 17, "#20312E", true))
        titleInput = input("예: 최종 보고서 작성")
        minutesInput = input("집중 시간(분), 예: 25").apply { inputType = InputType.TYPE_CLASS_NUMBER }
        inputCard.addView(titleInput)
        inputCard.addView(minutesInput)
        inputCard.addView(primaryButton("추가").apply { setOnClickListener { addTask() } })

        // 목록 카드: 사용자가 등록한 할 일 목록을 동적으로 표시한다.
        val taskCard = card()
        root.addView(taskCard)
        taskCard.addView(text("집중 목록", 17, "#20312E", true))
        taskList = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        taskCard.addView(taskList)

        // 통계 카드: 완료 세션, 누적 집중 시간, 평균 세션, 배지 등을 표시한다.
        val statsCard = card()
        root.addView(statsCard)
        statsCard.addView(text("성과 분석", 17, "#20312E", true))
        statsText = text("", 14, "#384441")
        statsCard.addView(statsText)

        setContentView(scroll)
    }

    // 입력된 제목과 시간을 검증한 뒤 새 할 일을 목록에 추가한다.
    private fun addTask() {
        val title = titleInput.text.toString().trim()
        val minutes = parseMinutes(minutesInput.text.toString().trim())
        if (title.isEmpty()) {
            Toast.makeText(this, "할 일을 입력하세요.", Toast.LENGTH_SHORT).show()
            return
        }

        // 새 할 일을 추가한 뒤 저장, 타이머 초기화, 화면 갱신을 함께 수행한다.
        tasks.add(FocusTask(title, minutes))
        selectedIndex = tasks.lastIndex
        titleInput.setText("")
        minutesInput.setText("")
        saveTasks()
        resetTimer()
        refresh()
    }

    // 사용자가 입력한 시간을 숫자로 바꾸고, 5~90분 범위 안으로 제한한다.
    private fun parseMinutes(value: String): Int = value.toIntOrNull()?.coerceIn(5, 90) ?: recommendMinutes()

    // 선택된 할 일의 집중 시간을 기준으로 카운트다운 타이머를 시작한다.
    private fun startTimer() {
        // 이미 실행 중이면 중복 타이머가 생기지 않도록 바로 종료한다.
        if (running) return

        // 선택된 항목이 없으면 아직 완료되지 않은 첫 번째 할 일을 자동 선택한다.
        if (selectedIndex < 0 && tasks.isNotEmpty()) {
            selectedIndex = firstOpenTask()
            activeMinutes = tasks[selectedIndex].minutes
            remainingMs = activeMinutes * 60L * 1000L
        }

        running = true
        timer = object : CountDownTimer(remainingMs, 1000) {
            // 1초마다 남은 시간을 저장하고 화면 표시를 갱신한다.
            override fun onTick(millisUntilFinished: Long) {
                remainingMs = millisUntilFinished
                updateTimerView()
            }

            // 시간이 끝나면 세션 완료 처리와 화면 갱신을 실행한다.
            override fun onFinish() {
                running = false
                remainingMs = 0
                completeSession()
                refresh()
            }
        }.also { it.start() }
    }

    // 현재 타이머를 멈추고 선택된 할 일의 목표 시간으로 다시 맞춘다.
    private fun resetTimer() {
        timer?.cancel()
        running = false
        activeMinutes = if (selectedIndex in tasks.indices) tasks[selectedIndex].minutes else recommendMinutes()
        remainingMs = activeMinutes * 60L * 1000L
        updateTimerView()
    }

    // 집중 시간이 끝났을 때 할 일 완료 처리, 통계 저장, 다음 할 일 선택을 수행한다.
    private fun completeSession() {
        if (selectedIndex in tasks.indices) {
            tasks[selectedIndex].done = true
            saveTasks()
        }

        // 완료된 집중 시간을 누적 기록에 반영한다.
        updateStreak()
        val total = prefs.getInt(KEY_TOTAL_MINUTES, 0) + activeMinutes
        val sessions = prefs.getInt(KEY_COMPLETED_SESSIONS, 0) + 1
        prefs.edit().putInt(KEY_TOTAL_MINUTES, total).putInt(KEY_COMPLETED_SESSIONS, sessions).apply()

        Toast.makeText(this, "집중 세션이 완료되었습니다.", Toast.LENGTH_LONG).show()
        selectedIndex = firstOpenTask()
        resetTimer()
    }

    // 현재 데이터 상태를 기준으로 목록, 추천 문구, 통계를 모두 다시 그린다.
    private fun refresh() {
        taskList.removeAllViews()

        // tasks 리스트를 순회하면서 할 일 한 줄마다 TextView와 선택 버튼을 만든다.
        tasks.forEachIndexed { index, task ->
            val itemRow = row().apply {
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, dp(7), 0, dp(7))
            }
            val labelText = if (task.done) "[완료] ${task.title} - ${task.minutes} min" else "${task.title} - ${task.minutes} min"
            val labelColor = if (task.done) "#8B928F" else "#263633"
            val label = text(labelText, 15, labelColor)
            val select = secondaryButton(if (index == selectedIndex) "선택됨" else "선택").apply {
                setOnClickListener {
                    selectedIndex = index
                    resetTimer()
                    refresh()
                }
            }
            itemRow.addView(label, weightParams())
            itemRow.addView(select)
            taskList.addView(itemRow)
        }

        // 저장된 통계와 추천 값을 읽어 화면 문구로 표시한다.
        val open = countOpenTasks()
        val sessions = prefs.getInt(KEY_COMPLETED_SESSIONS, 0)
        val totalMinutes = prefs.getInt(KEY_TOTAL_MINUTES, 0)
        val streak = prefs.getInt(KEY_STREAK, 0)
        val recommended = recommendMinutes()
        guideText.text = "남은 할 일: ${open}개. 추천 집중 시간: ${recommended}분."
        statsText.text = "완료 세션: ${sessions}회\n" +
            "누적 집중: ${totalMinutes}분\n" +
            "연속 학습: ${streak}일\n" +
            "평균 세션: ${averageMinutes(totalMinutes, sessions)}분\n" +
            badgeText(totalMinutes, sessions, streak)
        updateTimerView()
    }

    // 남은 시간을 mm:ss 형식으로 표시하고 진행률 바를 갱신한다.
    private fun updateTimerView() {
        val seconds = max(0, remainingMs / 1000)
        timerText.text = String.format(Locale.KOREA, "%02d:%02d", seconds / 60, seconds % 60)

        // 전체 시간 대비 지난 시간을 0~1000 범위로 바꾸어 ProgressBar에 넣는다.
        val totalMs = max(1, activeMinutes * 60L * 1000L)
        val progress = (1000 - remainingMs * 1000 / totalMs).toInt()
        progressBar.progress = progress.coerceIn(0, 1000)
    }

    // 사용자의 현재 상태를 바탕으로 다음 집중 시간을 추천한다.
    private fun recommendMinutes(): Int {
        val open = countOpenTasks()
        val sessions = prefs.getInt(KEY_COMPLETED_SESSIONS, 0)
        val total = prefs.getInt(KEY_TOTAL_MINUTES, 0)
        val average = averageMinutes(total, sessions)

        // 남은 일과 기존 평균 시간을 기준으로 추천 시간을 다르게 반환한다.
        return when {
            open >= 5 -> 15
            average >= 35 -> 30
            sessions >= 3 -> 25
            else -> 20
        }
    }

    // 아직 완료되지 않은 첫 번째 할 일의 인덱스를 찾는다.
    private fun firstOpenTask(): Int = tasks.indexOfFirst { !it.done }.takeIf { it >= 0 } ?: if (tasks.isEmpty()) -1 else 0

    // 완료되지 않은 할 일 개수를 센다.
    private fun countOpenTasks(): Int = tasks.count { !it.done }

    // 누적 집중 시간을 완료 세션 수로 나누어 평균 집중 시간을 계산한다.
    private fun averageMinutes(total: Int, sessions: Int): Int = if (sessions == 0) 0 else (total.toFloat() / sessions).roundToInt()

    // 사용자의 기록 조건에 따라 화면에 보여줄 배지 문구를 반환한다.
    private fun badgeText(totalMinutes: Int, sessions: Int, streak: Int): String = when {
        streak >= 3 -> "배지: 3일 연속 루틴 유지"
        totalMinutes >= 120 -> "배지: 두 시간 집중 달성"
        sessions >= 3 -> "배지: 세션 3회 완료"
        else -> "배지: 첫 집중을 완료하면 열립니다."
    }

    // 오늘 날짜와 마지막 학습 날짜를 비교해 연속 학습일을 갱신한다.
    private fun updateStreak() {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.KOREA).format(Date())
        val lastDay = prefs.getString(KEY_LAST_DAY, "").orEmpty()
        var streak = prefs.getInt(KEY_STREAK, 0)
        if (today != lastDay) streak = if (isYesterday(lastDay, today)) streak + 1 else 1
        prefs.edit().putString(KEY_LAST_DAY, today).putInt(KEY_STREAK, streak).apply()
    }

    // 마지막 학습일이 오늘 기준으로 정확히 어제인지 확인한다.
    private fun isYesterday(lastDay: String, today: String): Boolean = try {
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.KOREA)
        val diff = format.parse(today)!!.time - format.parse(lastDay)!!.time
        diff == 24L * 60L * 60L * 1000L
    } catch (_: Exception) {
        false
    }

    // SharedPreferences에 저장된 JSON 문자열을 읽어 할 일 목록으로 복원한다.
    private fun loadTasks() {
        tasks.clear()
        try {
            val array = JSONArray(prefs.getString(KEY_TASKS, "[]"))
            for (i in 0 until array.length()) {
                val item = array.getJSONObject(i)
                tasks.add(FocusTask(item.getString("title"), item.getInt("minutes"), item.getBoolean("done")))
            }
        } catch (_: Exception) {
            tasks.clear()
        }

        // 불러온 목록 중 아직 완료되지 않은 할 일을 기본 선택 대상으로 삼는다.
        selectedIndex = firstOpenTask()
        if (selectedIndex >= 0) {
            activeMinutes = tasks[selectedIndex].minutes
            remainingMs = activeMinutes * 60L * 1000L
        }
    }

    // 현재 할 일 목록을 JSON 배열 문자열로 바꾸어 SharedPreferences에 저장한다.
    private fun saveTasks() {
        val array = JSONArray()
        try {
            tasks.forEach { task ->
                array.put(JSONObject().put("title", task.title).put("minutes", task.minutes).put("done", task.done))
            }
        } catch (_: Exception) {
            return
        }
        prefs.edit().putString(KEY_TASKS, array.toString()).apply()
    }

    // 첫 실행 시 빈 화면이 되지 않도록 기본 예시 할 일을 만든다.
    private fun seedTasks() {
        tasks.add(FocusTask("최종과제 보고서 초안 작성", 25))
        tasks.add(FocusTask("앱 기능 테스트", 20))
        tasks.add(FocusTask("발표용 화면 정리", 15))
        saveTasks()
        selectedIndex = 0
        activeMinutes = tasks[0].minutes
        remainingMs = activeMinutes * 60L * 1000L
    }

    // 흰색 카드 형태의 공통 레이아웃을 만든다.
    private fun card(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(16), dp(14), dp(16), dp(14))
        setBackgroundResource(R.drawable.card_bg)
        layoutParams = LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, dp(16), 0, 0) }
    }

    // 가로 방향으로 자식 View를 배치하는 공통 행 레이아웃을 만든다.
    private fun row(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER
    }

    // TextView를 반복 생성하기 위한 공통 함수이다.
    private fun text(value: String, sp: Int, color: String, bold: Boolean = false): TextView = TextView(this).apply {
        text = value
        textSize = sp.toFloat()
        setTextColor(Color.parseColor(color))
        setPadding(0, dp(4), 0, dp(4))
        if (bold) typeface = Typeface.DEFAULT_BOLD
    }

    // 한글 입력이 가능한 일반 텍스트 입력창을 만든다.
    private fun input(hintText: String): EditText = EditText(this).apply {
        hint = hintText
        inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        imeOptions = EditorInfo.IME_ACTION_DONE
        setSingleLine(true)
        textSize = 15f
        setPadding(0, dp(8), 0, dp(8))
    }

    // 주요 행동에 사용하는 초록색 버튼을 만든다.
    private fun primaryButton(label: String): Button = Button(this).apply {
        text = label
        setTextColor(Color.WHITE)
        setBackgroundResource(R.drawable.primary_button)
    }

    // 보조 행동에 사용하는 회색 버튼을 만든다.
    private fun secondaryButton(label: String): Button = Button(this).apply {
        text = label
        setTextColor(Color.rgb(38, 54, 51))
        setBackgroundResource(R.drawable.secondary_button)
    }

    // 가로 행에서 버튼과 텍스트가 자연스럽게 공간을 나누도록 하는 LayoutParams이다.
    private fun weightParams(): LinearLayout.LayoutParams = LinearLayout.LayoutParams(0, -2, 1f).apply {
        setMargins(0, 0, dp(8), 0)
    }

    // dp 단위를 현재 기기 해상도에 맞는 픽셀 값으로 변환한다.
    private fun dp(value: Int): Int = (value * resources.displayMetrics.density + 0.5f).toInt()

    // 할 일 하나를 표현하는 데이터 클래스이다.
    private data class FocusTask(val title: String, val minutes: Int, var done: Boolean = false)

    // SharedPreferences에서 사용할 저장소 이름과 key 값을 모아둔다.
    companion object {
        private const val PREFS = "focus_bloom_prefs"
        private const val KEY_TASKS = "tasks_json"
        private const val KEY_TOTAL_MINUTES = "total_minutes"
        private const val KEY_COMPLETED_SESSIONS = "completed_sessions"
        private const val KEY_STREAK = "streak"
        private const val KEY_LAST_DAY = "last_day"
    }
}
