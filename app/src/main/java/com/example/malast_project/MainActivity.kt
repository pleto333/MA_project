package com.example.malast_project

import android.app.Activity
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.os.CountDownTimer
import android.text.InputType
import android.view.Gravity
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

class MainActivity : Activity() {
    private lateinit var prefs: SharedPreferences
    private lateinit var taskList: LinearLayout
    private lateinit var timerText: TextView
    private lateinit var statsText: TextView
    private lateinit var guideText: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var titleInput: EditText
    private lateinit var minutesInput: EditText

    private val tasks = mutableListOf<FocusTask>()
    private var timer: CountDownTimer? = null
    private var selectedIndex = -1
    private var activeMinutes = 25
    private var remainingMs = 25 * 60 * 1000L
    private var running = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE)
        loadTasks()
        if (tasks.isEmpty()) seedTasks()
        buildUi()
        refresh()
    }

    override fun onDestroy() {
        timer?.cancel()
        super.onDestroy()
    }

    private fun buildUi() {
        val scroll = ScrollView(this).apply { setBackgroundColor(Color.rgb(247, 244, 239)) }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(20), dp(18), dp(24))
        }
        scroll.addView(root)

        root.addView(text("FocusBloom", 30, "#20312E", true))
        root.addView(text("집중 타이머와 학습 통계를 제공하는 루틴 관리 앱", 14, "#62706B"))

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

        val inputCard = card()
        root.addView(inputCard)
        inputCard.addView(text("할 일 추가", 17, "#20312E", true))
        titleInput = input("예: 최종 보고서 작성")
        minutesInput = input("집중 시간(분), 예: 25").apply { inputType = InputType.TYPE_CLASS_NUMBER }
        inputCard.addView(titleInput)
        inputCard.addView(minutesInput)
        inputCard.addView(primaryButton("추가").apply { setOnClickListener { addTask() } })

        val taskCard = card()
        root.addView(taskCard)
        taskCard.addView(text("집중 목록", 17, "#20312E", true))
        taskList = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        taskCard.addView(taskList)

        val statsCard = card()
        root.addView(statsCard)
        statsCard.addView(text("성과 분석", 17, "#20312E", true))
        statsText = text("", 14, "#384441")
        statsCard.addView(statsText)

        setContentView(scroll)
    }

    private fun addTask() {
        val title = titleInput.text.toString().trim()
        val minutes = parseMinutes(minutesInput.text.toString().trim())
        if (title.isEmpty()) {
            Toast.makeText(this, "할 일을 입력하세요.", Toast.LENGTH_SHORT).show()
            return
        }
        tasks.add(FocusTask(title, minutes))
        selectedIndex = tasks.lastIndex
        titleInput.setText("")
        minutesInput.setText("")
        saveTasks()
        resetTimer()
        refresh()
    }

    private fun parseMinutes(value: String): Int = value.toIntOrNull()?.coerceIn(5, 90) ?: recommendMinutes()

    private fun startTimer() {
        if (running) return
        if (selectedIndex < 0 && tasks.isNotEmpty()) {
            selectedIndex = firstOpenTask()
            activeMinutes = tasks[selectedIndex].minutes
            remainingMs = activeMinutes * 60L * 1000L
        }
        running = true
        timer = object : CountDownTimer(remainingMs, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                remainingMs = millisUntilFinished
                updateTimerView()
            }

            override fun onFinish() {
                running = false
                remainingMs = 0
                completeSession()
                refresh()
            }
        }.also { it.start() }
    }

    private fun resetTimer() {
        timer?.cancel()
        running = false
        activeMinutes = if (selectedIndex in tasks.indices) tasks[selectedIndex].minutes else recommendMinutes()
        remainingMs = activeMinutes * 60L * 1000L
        updateTimerView()
    }

    private fun completeSession() {
        if (selectedIndex in tasks.indices) {
            tasks[selectedIndex].done = true
            saveTasks()
        }
        updateStreak()
        val total = prefs.getInt(KEY_TOTAL_MINUTES, 0) + activeMinutes
        val sessions = prefs.getInt(KEY_COMPLETED_SESSIONS, 0) + 1
        prefs.edit().putInt(KEY_TOTAL_MINUTES, total).putInt(KEY_COMPLETED_SESSIONS, sessions).apply()
        Toast.makeText(this, "집중 세션이 완료되었습니다.", Toast.LENGTH_LONG).show()
        selectedIndex = firstOpenTask()
        resetTimer()
    }

    private fun refresh() {
        taskList.removeAllViews()
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

    private fun updateTimerView() {
        val seconds = max(0, remainingMs / 1000)
        timerText.text = String.format(Locale.KOREA, "%02d:%02d", seconds / 60, seconds % 60)
        val totalMs = max(1, activeMinutes * 60L * 1000L)
        val progress = (1000 - remainingMs * 1000 / totalMs).toInt()
        progressBar.progress = progress.coerceIn(0, 1000)
    }

    private fun recommendMinutes(): Int {
        val open = countOpenTasks()
        val sessions = prefs.getInt(KEY_COMPLETED_SESSIONS, 0)
        val total = prefs.getInt(KEY_TOTAL_MINUTES, 0)
        val average = averageMinutes(total, sessions)
        return when {
            open >= 5 -> 15
            average >= 35 -> 30
            sessions >= 3 -> 25
            else -> 20
        }
    }

    private fun firstOpenTask(): Int = tasks.indexOfFirst { !it.done }.takeIf { it >= 0 } ?: if (tasks.isEmpty()) -1 else 0
    private fun countOpenTasks(): Int = tasks.count { !it.done }
    private fun averageMinutes(total: Int, sessions: Int): Int = if (sessions == 0) 0 else (total.toFloat() / sessions).roundToInt()

    private fun badgeText(totalMinutes: Int, sessions: Int, streak: Int): String = when {
        streak >= 3 -> "배지: 3일 연속 루틴 유지"
        totalMinutes >= 120 -> "배지: 두 시간 집중 달성"
        sessions >= 3 -> "배지: 세션 3회 완료"
        else -> "배지: 첫 집중을 완료하면 열립니다."
    }

    private fun updateStreak() {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.KOREA).format(Date())
        val lastDay = prefs.getString(KEY_LAST_DAY, "").orEmpty()
        var streak = prefs.getInt(KEY_STREAK, 0)
        if (today != lastDay) streak = if (isYesterday(lastDay, today)) streak + 1 else 1
        prefs.edit().putString(KEY_LAST_DAY, today).putInt(KEY_STREAK, streak).apply()
    }

    private fun isYesterday(lastDay: String, today: String): Boolean = try {
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.KOREA)
        val diff = format.parse(today)!!.time - format.parse(lastDay)!!.time
        diff == 24L * 60L * 60L * 1000L
    } catch (_: Exception) {
        false
    }

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
        selectedIndex = firstOpenTask()
        if (selectedIndex >= 0) {
            activeMinutes = tasks[selectedIndex].minutes
            remainingMs = activeMinutes * 60L * 1000L
        }
    }

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

    private fun seedTasks() {
        tasks.add(FocusTask("최종과제 보고서 초안 작성", 25))
        tasks.add(FocusTask("앱 기능 테스트", 20))
        tasks.add(FocusTask("발표용 화면 정리", 15))
        saveTasks()
        selectedIndex = 0
        activeMinutes = tasks[0].minutes
        remainingMs = activeMinutes * 60L * 1000L
    }

    private fun card(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(16), dp(14), dp(16), dp(14))
        setBackgroundResource(R.drawable.card_bg)
        layoutParams = LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, dp(16), 0, 0) }
    }

    private fun row(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER
    }

    private fun text(value: String, sp: Int, color: String, bold: Boolean = false): TextView = TextView(this).apply {
        text = value
        textSize = sp.toFloat()
        setTextColor(Color.parseColor(color))
        setPadding(0, dp(4), 0, dp(4))
        if (bold) typeface = Typeface.DEFAULT_BOLD
    }

    private fun input(hintText: String): EditText = EditText(this).apply {
        hint = hintText
        setSingleLine(true)
        textSize = 15f
        setPadding(0, dp(8), 0, dp(8))
    }

    private fun primaryButton(label: String): Button = Button(this).apply {
        text = label
        setTextColor(Color.WHITE)
        setBackgroundResource(R.drawable.primary_button)
    }

    private fun secondaryButton(label: String): Button = Button(this).apply {
        text = label
        setTextColor(Color.rgb(38, 54, 51))
        setBackgroundResource(R.drawable.secondary_button)
    }

    private fun weightParams(): LinearLayout.LayoutParams = LinearLayout.LayoutParams(0, -2, 1f).apply {
        setMargins(0, 0, dp(8), 0)
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density + 0.5f).toInt()

    private data class FocusTask(val title: String, val minutes: Int, var done: Boolean = false)

    companion object {
        private const val PREFS = "focus_bloom_prefs"
        private const val KEY_TASKS = "tasks_json"
        private const val KEY_TOTAL_MINUTES = "total_minutes"
        private const val KEY_COMPLETED_SESSIONS = "completed_sessions"
        private const val KEY_STREAK = "streak"
        private const val KEY_LAST_DAY = "last_day"
    }
}
