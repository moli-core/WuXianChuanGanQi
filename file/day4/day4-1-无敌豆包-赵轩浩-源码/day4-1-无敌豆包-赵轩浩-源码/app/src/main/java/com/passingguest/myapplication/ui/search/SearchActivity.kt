package com.passingguest.myapplication.ui.search

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.passingguest.myapplication.R
import com.passingguest.myapplication.ui.adapter.DisplayItem
import com.passingguest.myapplication.ui.adapter.RecordAdapter
import com.passingguest.myapplication.util.DateUtils
import java.util.Calendar

class SearchActivity : AppCompatActivity() {

    private lateinit var viewModel: SearchViewModel
    private lateinit var etSearch: EditText
    private lateinit var recyclerView: RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var filterPanel: android.widget.LinearLayout
    private lateinit var btnFilter: MaterialButton
    private lateinit var toggleType: MaterialButtonToggleGroup
    private lateinit var tvStartDate: TextView
    private lateinit var tvEndDate: TextView
    private lateinit var adapter: RecordAdapter

    private var startDate: Long? = null
    private var endDate: Long? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)

        viewModel = ViewModelProvider(this)[SearchViewModel::class.java]

        setupViews()
        setupRecyclerView()
        setupObservers()
        setupClickListeners()
    }

    private fun setupViews() {
        val toolbar: MaterialToolbar = findViewById(R.id.toolbar)
        toolbar.setNavigationOnClickListener { finish() }

        etSearch = findViewById(R.id.etSearch)
        recyclerView = findViewById(R.id.recyclerView)
        tvEmpty = findViewById(R.id.tvEmpty)
        filterPanel = findViewById(R.id.filterPanel)
        btnFilter = findViewById(R.id.btnFilter)
        toggleType = findViewById(R.id.toggleType)
        tvStartDate = findViewById(R.id.tvStartDate)
        tvEndDate = findViewById(R.id.tvEndDate)
    }

    private fun setupRecyclerView() {
        adapter = RecordAdapter(
            onRecordClick = { /* Navigate to edit - same as before */ },
            onRecordLongClick = { /* Delete */ }
        )
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
    }

    private fun setupObservers() {
        viewModel.searchResults.observe(this) { results ->
            val displayItems = results.map { record ->
                DisplayItem(
                    type = DisplayItem.TYPE_RECORD,
                    date = record.date,
                    recordId = record.id,
                    categoryName = "",
                    categoryIcon = "?",
                    amount = record.amount,
                    note = record.note,
                    recordType = record.type
                )
            }
            adapter.submitList(displayItems)
            tvEmpty.visibility = if (displayItems.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    private fun setupClickListeners() {
        etSearch.setOnEditorActionListener { _, _, _ ->
            viewModel.setKeyword(etSearch.text.toString())
            true
        }

        btnFilter.setOnClickListener {
            filterPanel.visibility = if (filterPanel.visibility == View.VISIBLE) View.GONE else View.VISIBLE
        }

        toggleType.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            val type = when (checkedId) {
                R.id.btnExpense -> "EXPENSE"
                R.id.btnIncome -> "INCOME"
                else -> null
            }
            viewModel.setType(type)
        }

        tvStartDate.setOnClickListener { showDatePicker(true) }
        tvEndDate.setOnClickListener { showDatePicker(false) }
    }

    private fun showDatePicker(isStart: Boolean) {
        val cal = Calendar.getInstance()
        DatePickerDialog(
            this,
            { _, year, month, day ->
                cal.set(Calendar.YEAR, year)
                cal.set(Calendar.MONTH, month)
                cal.set(Calendar.DAY_OF_MONTH, day)
                val timestamp = cal.timeInMillis
                if (isStart) {
                    startDate = DateUtils.getStartOfDay(timestamp)
                    tvStartDate.text = DateUtils.formatDate(timestamp)
                } else {
                    endDate = DateUtils.getEndOfDay(timestamp)
                    tvEndDate.text = DateUtils.formatDate(timestamp)
                }
                viewModel.setDateRange(startDate, endDate)
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }
}
