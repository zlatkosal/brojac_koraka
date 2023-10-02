package com.example.brojac_koraka

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.components.YAxis.YAxisLabelPosition
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.fitness.Fitness
import com.google.android.gms.fitness.FitnessOptions
import com.google.android.gms.fitness.data.DataSet
import com.google.android.gms.fitness.data.DataType
import com.google.android.gms.fitness.request.DataReadRequest
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.*
import java.util.concurrent.TimeUnit


class CounterForStepsActivity : AppCompatActivity() {

    private lateinit var barChart: BarChart
    private val fitnessOptions = FitnessOptions.builder()
        .addDataType(DataType.TYPE_STEP_COUNT_CUMULATIVE)
        .addDataType(DataType.TYPE_STEP_COUNT_DELTA)
        .build()
    private var wantedSteps = 0f
    private var chosenPeriod = TimePeriod.LAST_DAY

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_counter_for_steps)

        barChart = findViewById(R.id.bar_chart)

        setListeners()
        checkIfWantedStepsEntered()
    }

    private fun checkIfWantedStepsEntered() {
        wantedSteps = SharedPrefs.getStepsFromPrefs(this)
        if (wantedSteps != 0f) {
            findViewById<EditText>(R.id.enter_wanted_steps).setText(wantedSteps.toString())
            readDataForSpecificTime(chosenPeriod)
        } else {
            Toast.makeText(this, getString(R.string.enter_wanted_steps_error), Toast.LENGTH_LONG)
                .show()
        }
    }

    private fun setListeners() {
        (findViewById<RadioGroup>(R.id.time_layout))?.setOnCheckedChangeListener { _, i ->
            when (i) {
                R.id.day_option -> readDataForSpecificTime(TimePeriod.LAST_DAY)
                R.id.week_option -> readDataForSpecificTime(TimePeriod.LAST_WEEK)
                R.id.month_option -> readDataForSpecificTime(TimePeriod.LAST_MONTH)
            }
        }
        (findViewById<Button>(R.id.save_wanted_steps))?.setOnClickListener {
            val wantedStepsEditText: EditText = findViewById(R.id.enter_wanted_steps)
            if (wantedStepsEditText.text.isNullOrBlank()) {
                Toast.makeText(
                    this,
                    getString(R.string.enter_wanted_steps_error),
                    Toast.LENGTH_LONG
                ).show()
            } else {
                Toast.makeText(this, getString(R.string.wanted_steps_saved), Toast.LENGTH_LONG)
                    .show()
                wantedSteps = wantedStepsEditText.text.toString().toFloat()
                SharedPrefs.saveStepsInPrefs(this, wantedSteps)
                readDataForSpecificTime(chosenPeriod)
            }
        }
    }

    private fun getGoogleAccount() = GoogleSignIn.getAccountForExtension(this, fitnessOptions)

    private fun readDataForSpecificTime(timePeriod: TimePeriod) {
        chosenPeriod = timePeriod
        val wantedStepsEditText: EditText = findViewById(R.id.enter_wanted_steps)
        if (wantedStepsEditText.text.isNullOrBlank()) {
            Toast.makeText(
                this,
                getString(R.string.enter_wanted_steps_error),
                Toast.LENGTH_LONG
            ).show()
        } else {
            val endTime = LocalDateTime.now().atZone(ZoneId.systemDefault())
            val startTime: ZonedDateTime? = when (timePeriod) {
                TimePeriod.LAST_DAY -> endTime.minusDays(1)
                TimePeriod.LAST_WEEK -> endTime.minusDays(7)
                TimePeriod.LAST_MONTH -> endTime.minusDays(30)
            }

            val readRequest: DataReadRequest = DataReadRequest.Builder()
                .aggregate(DataType.TYPE_STEP_COUNT_DELTA, DataType.AGGREGATE_STEP_COUNT_DELTA)
                .bucketByTime(1, TimeUnit.DAYS)
                .setTimeRange(
                    startTime?.toEpochSecond() ?: 0,
                    endTime.toEpochSecond(),
                    TimeUnit.SECONDS
                )
                .build()

            Fitness.getHistoryClient(this, getGoogleAccount())
                .readData(readRequest)
                .addOnSuccessListener { dataSet ->
                    val totalAvgSteps = arrayListOf<Int>()
                    for (bucket in dataSet.buckets) {
                        for (set in bucket.dataSets) {
                            totalAvgSteps.add(dumpDataSet(set))
                        }
                    }
                    val sum = totalAvgSteps.sum()
                    updateChart(sum.toFloat())

                    val averageSteps: TextView = findViewById(R.id.average_steps_count)
                    when (timePeriod) {
                        TimePeriod.LAST_WEEK -> {
                            averageSteps.visibility = View.VISIBLE
                            averageSteps.text = String.format(
                                Locale.getDefault(),
                                getString(R.string.average_steps_last_week),
                                totalAvgSteps.average()
                            )
                        }
                        TimePeriod.LAST_MONTH -> {
                            averageSteps.visibility = View.VISIBLE
                            averageSteps.text = String.format(
                                Locale.getDefault(),
                                getString(R.string.average_steps_last_month),
                                totalAvgSteps.average()
                            )
                        }
                        else -> averageSteps.visibility = View.GONE
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(
                        this,
                        getString(R.string.load_step_count_problem),
                        Toast.LENGTH_LONG
                    )
                        .show()
                }
        }
    }

    private fun dumpDataSet(dataSet: DataSet): Int {
        var totalSteps = 0
        for (dp in dataSet.dataPoints) {
            for (field in dp.dataType.fields) {
                totalSteps += dp.getValue(field).asInt()
            }
        }
        return totalSteps
    }

    private fun updateChart(achievedSteps: Float) {
        barChart.invalidate()
        val entries = ArrayList<BarEntry>()
        entries.add(BarEntry(0f, achievedSteps))
        entries.add(BarEntry(1f, wantedSteps))

        val dataSet = BarDataSet(entries, getString(R.string.steps_count))
        dataSet.valueTextColor = ContextCompat.getColor(this, R.color.black)
        dataSet.color = ContextCompat.getColor(this, R.color.black)
        dataSet.colors = listOf(
            ContextCompat.getColor(this, R.color.purple_500),
            ContextCompat.getColor(this, R.color.teal_700)
        )

        val data = BarData(dataSet)
        data.barWidth = 0.3f
        barChart.data = data
        barChart.notifyDataSetChanged()

        val leftAxis: YAxis = barChart.axisLeft
        leftAxis.textColor = ContextCompat.getColor(this, R.color.black)
        leftAxis.setPosition(YAxisLabelPosition.OUTSIDE_CHART)
        leftAxis.axisMinimum = 0f
        val maximum = if (wantedSteps > achievedSteps) wantedSteps else achievedSteps
        leftAxis.axisMaximum = if (maximum == 0f) 1000f else maximum
        barChart.axisRight.isEnabled = false

        val xAxis: XAxis = barChart.xAxis
        xAxis.textColor = ContextCompat.getColor(this, R.color.black)
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.valueFormatter = IndexAxisValueFormatter(
            arrayOf(getString(R.string.achieved), getString(R.string.wanted))
        )
        xAxis.granularity = 1f
        barChart.description.isEnabled = false
        barChart.legend.textColor = ContextCompat.getColor(this, R.color.black)
    }
}