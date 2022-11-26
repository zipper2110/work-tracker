package com.d.litvin.worktimer

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.view.View.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.d.litvin.worktimer.ActivityType.*
import com.d.litvin.worktimer.WorkingState.*
import com.d.litvin.worktimer.databinding.ActivityMainBinding
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.time.*
import java.util.UUID

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navView: BottomNavigationView = binding.navView

        val navController = findNavController(R.id.nav_host_fragment_activity_main)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        val appBarConfiguration = AppBarConfiguration(setOf(
                R.id.navigation_home, R.id.navigation_dashboard, R.id.navigation_notifications))
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)
    }

    private var state: WorkingState = NOT_STARTED
        set(value) {
            field = value
            render()
            val activityType = when (value) {
                NOT_STARTED -> STOP
                WORKING -> WORK
                RESTING -> REST
            }
            activities += Activity(activityType)

            // TODO: change when persistance is implemented
            if (value == NOT_STARTED) {
                activities.clear()
            }
        }

    private val activities = mutableListOf<Activity>()

    private val handler = Handler()
    private var updateText: Runnable = renderByTimer()

    fun onWorkButtonClick(view: View) {
        when (state) {
            NOT_STARTED -> startWork()
            WORKING -> goRest()
            RESTING -> backToWork()
        }
    }

    private fun startWork() {
        state = WORKING
        handler.postDelayed(updateText, 1000)
    }

    private fun goRest() {
        state = RESTING
    }

    private fun backToWork() {
        state = WORKING
    }

    fun onStopWorkButtonClick(view: View) {
        val builder = AlertDialog.Builder(this)
        builder.setMessage("Work time: ${timeToString(calculateTimeOf(WORK))}\n" +
                "Rest time: ${timeToString(calculateTimeOf(REST))}")
        val dialog = builder.create()
        dialog.setTitle("Work day sum up")
        dialog.show()

        state = NOT_STARTED
    }

    private fun calculateTimeOf(activityType: ActivityType): Duration {
        val activities = activities
        if (activities.size == 0) return Duration.ZERO

        var totalWorkTime = Duration.ZERO
        for ((index, activity) in activities.withIndex()) {
            if (activity.type == activityType) {
                val nextActivity = if (index == activities.lastIndex) {
                    null
                } else {
                    activities[index + 1]
                }
                val workEnd = nextActivity?.start ?: OffsetDateTime.now()
                totalWorkTime += Duration.between(activity.start, workEnd)
            }
        }

        return totalWorkTime
    }


    private fun render() {
        var workButtonText = ""
        var finishWorkButtonVisible = true
        when (state) {
            NOT_STARTED -> {
                workButtonText = "Start day"
                finishWorkButtonVisible = false
            }
            WORKING -> {
                workButtonText = "Go rest"
                finishWorkButtonVisible = true
            }
            RESTING -> {
                workButtonText = "Back to work"
                finishWorkButtonVisible = true
            }
        }
        val workButton = findViewById<Button>(R.id.workButton)
        val finishWorkButton = findViewById<Button>(R.id.finishWorkButton)
        workButton.text = workButtonText
        finishWorkButton.visibility = if (finishWorkButtonVisible) VISIBLE else INVISIBLE

        renderHistory()
    }

    private fun renderByTimer() = Runnable {
        findViewById<TextView>(R.id.workTime).text = timeToString(calculateTimeOf(WORK))
        findViewById<TextView>(R.id.restTime).text = timeToString(calculateTimeOf(REST))
        renderHistory()
        if (state != NOT_STARTED) {
            handler.postDelayed(updateText, 1000)
        }
    }

    private fun timeToString(time: Duration): String {
        val hours = time.toHours()
        val minutes = time.toMinutesPart()
        val seconds = time.toSecondsPart()
        return "${hours}H ${minutes}M ${seconds}S"
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun renderHistory() {
        val historyLayout = findViewById<LinearLayout>(R.id.historyLinearLayout)
        historyLayout.removeAllViews()

        activities.forEach {
            val itemLayout = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
            }
            itemLayout.addView(TextView(this).apply {
                text = "${it.type} ${it.start.toLocalDateTime()}"
                textSize = 16f

                val time = it.start
                setOnTouchListener { view, event ->
                    val dialog = TimePickerDialog(this@MainActivity,
                        { _, hourOfDay, minute ->
                            it.start = it.start.withHour(hourOfDay).withMinute(minute)
                        }, time.hour, time.minute, true
                    )
                    dialog.show()
//                    performClick()
                    false
                }
            })

            historyLayout.addView(itemLayout)
        }
    }
}

class Activity(val type: ActivityType) {
    val id = UUID.randomUUID()
    var start: OffsetDateTime = OffsetDateTime.now()
}

enum class ActivityType {
    WORK,
    REST,
    STOP
}

enum class WorkingState {
    NOT_STARTED,
    WORKING,
    RESTING
}
