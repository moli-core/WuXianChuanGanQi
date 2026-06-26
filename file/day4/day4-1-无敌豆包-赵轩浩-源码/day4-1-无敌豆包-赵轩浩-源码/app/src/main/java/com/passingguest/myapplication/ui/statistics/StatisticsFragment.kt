package com.passingguest.myapplication.ui.statistics

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.PercentFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.passingguest.myapplication.R
import com.passingguest.myapplication.util.CurrencyUtils

class StatisticsFragment : Fragment() {

    private lateinit var viewModel: StatisticsViewModel

    private lateinit var tvTotalIncome: TextView
    private lateinit var tvTotalExpense: TextView
    private lateinit var pieChart: com.github.mikephil.charting.charts.PieChart
    private lateinit var barChart: com.github.mikephil.charting.charts.BarChart
    private lateinit var layoutRanking: LinearLayout
    private lateinit var togglePeriod: MaterialButtonToggleGroup

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_statistics, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(this)[StatisticsViewModel::class.java]

        setupViews(view)
        setupPieChart()
        setupBarChart()
        setupObservers()
        setupClickListeners(view)
    }

    private fun setupViews(view: View) {
        tvTotalIncome = view.findViewById(R.id.tvTotalIncome)
        tvTotalExpense = view.findViewById(R.id.tvTotalExpense)
        pieChart = view.findViewById(R.id.pieChart)
        barChart = view.findViewById(R.id.barChart)
        layoutRanking = view.findViewById(R.id.layoutRanking)
        togglePeriod = view.findViewById(R.id.togglePeriod)
    }

    private fun setupPieChart() {
        pieChart.apply {
            setUsePercentValues(true)
            description.isEnabled = false
            setEntryLabelTextSize(11f)
            setEntryLabelColor(Color.DKGRAY)
            isDrawHoleEnabled = true
            holeRadius = 40f
            setHoleColor(Color.TRANSPARENT)
            transparentCircleRadius = 45f
            legend.apply {
                verticalAlignment = Legend.LegendVerticalAlignment.BOTTOM
                horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
                orientation = Legend.LegendOrientation.HORIZONTAL
                setDrawInside(false)
                textSize = 11f
            }
            invalidate()
        }
    }

    private fun setupBarChart() {
        barChart.apply {
            description.isEnabled = false
            setFitBars(true)
            setScaleEnabled(false)
            legend.apply {
                verticalAlignment = Legend.LegendVerticalAlignment.TOP
                horizontalAlignment = Legend.LegendHorizontalAlignment.RIGHT
                textSize = 12f
            }
            axisLeft.apply {
                setDrawGridLines(false)
                axisMinimum = 0f
            }
            xAxis.apply {
                setDrawGridLines(false)
                setDrawLabels(true)
                setCenterAxisLabels(true)
                textSize = 12f
            }
            axisRight.isEnabled = false
            invalidate()
        }
    }

    private fun setupObservers() {
        viewModel.periodSummary.observe(viewLifecycleOwner) { summary ->
            if (summary != null) {
                tvTotalIncome.text = CurrencyUtils.format(summary.totalIncome)
                tvTotalExpense.text = CurrencyUtils.format(summary.totalExpense)
                updateBarChart(summary.totalIncome, summary.totalExpense)
            }
        }

        viewModel.expenseCategorySummary.observe(viewLifecycleOwner) { categories ->
            updatePieChart(categories)
            updateCategoryRanking(categories)
        }
    }

    private fun setupClickListeners(view: View) {
        togglePeriod.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            when (checkedId) {
                R.id.btnThisMonth -> viewModel.setPeriod("this_month")
                R.id.btnLastMonth -> viewModel.setPeriod("last_month")
            }
        }
        togglePeriod.check(R.id.btnThisMonth)
    }

    private fun updatePieChart(categories: List<com.passingguest.myapplication.data.dao.CategorySummary>) {
        val entries = categories.filter { it.total > 0 }.mapIndexed { index, cat ->
            PieEntry(cat.total.toFloat(), cat.categoryName)
        }

        if (entries.isEmpty()) {
            pieChart.data = null
            pieChart.setNoDataText("暂无支出数据")
            pieChart.invalidate()
            return
        }

        val colors = mutableListOf<Int>()
        colors.addAll(ColorTemplate.MATERIAL_COLORS.toList())
        colors.addAll(ColorTemplate.JOYFUL_COLORS.toList())
        colors.addAll(ColorTemplate.PASTEL_COLORS.toList())

        val dataSet = PieDataSet(entries, "").apply {
            this.colors = colors.take(entries.size)
            valueTextSize = 11f
            valueTextColor = Color.DKGRAY
            valueFormatter = PercentFormatter(pieChart)
            sliceSpace = 2f
        }

        pieChart.data = PieData(dataSet).apply {
            setValueFormatter(PercentFormatter(pieChart))
        }
        pieChart.highlightValues(null)
        pieChart.invalidate()
    }

    private fun updateBarChart(income: Double, expense: Double) {
        val entries = listOf(
            BarEntry(0f, income.toFloat()),
            BarEntry(1f, expense.toFloat())
        )

        val dataSet = BarDataSet(entries, "金额").apply {
            colors = listOf(
                resources.getColor(R.color.income_color, null),
                resources.getColor(R.color.expense_color, null)
            )
            valueTextSize = 12f
            valueTextColor = Color.DKGRAY
        }

        barChart.xAxis.apply {
            valueFormatter = object : com.github.mikephil.charting.formatter.IndexAxisValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    return if (value.toInt() == 0) "收入" else "支出"
                }
            }
            granularity = 1f
        }

        barChart.data = BarData(dataSet).apply {
            barWidth = 0.4f
        }
        barChart.invalidate()
    }

    private fun updateCategoryRanking(categories: List<com.passingguest.myapplication.data.dao.CategorySummary>) {
        layoutRanking.removeAllViews()

        if (categories.isEmpty()) {
            val tv = TextView(requireContext()).apply {
                text = "暂无数据"
                textSize = 14f
                setTextColor(Color.GRAY)
            }
            layoutRanking.addView(tv)
            return
        }

        val totalExpense = categories.sumOf { it.total }

        categories.forEachIndexed { index, cat ->
            val itemView = LayoutInflater.from(requireContext())
                .inflate(android.R.layout.simple_list_item_2, layoutRanking, false)

            val text1 = itemView.findViewById<TextView>(android.R.id.text1)
            val text2 = itemView.findViewById<TextView>(android.R.id.text2)

            val percentage = if (totalExpense > 0) (cat.total / totalExpense * 100).toInt() else 0
            text1.text = "${index + 1}. ${cat.categoryName} ($percentage%)"
            text2.text = CurrencyUtils.format(cat.total)

            layoutRanking.addView(itemView)
        }
    }
}
