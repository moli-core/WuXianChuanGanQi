package com.passingguest.myapplication.ui.addedit

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.tabs.TabLayout
import com.google.android.material.textfield.TextInputEditText
import com.passingguest.myapplication.R
import com.passingguest.myapplication.data.entity.Category
import com.passingguest.myapplication.ui.adapter.CategoryAdapter
import com.passingguest.myapplication.util.CurrencyUtils
import com.passingguest.myapplication.util.DateUtils
import kotlinx.coroutines.launch
import java.util.Calendar

class AddEditRecordActivity : AppCompatActivity() {

    private lateinit var viewModel: AddEditRecordViewModel
    private lateinit var tabLayout: TabLayout
    private lateinit var etAmount: TextInputEditText
    private lateinit var rvCategories: RecyclerView
    private lateinit var tvDate: TextView
    private lateinit var etNote: TextInputEditText
    private lateinit var btnSave: MaterialButton

    private var selectedType = Category.TYPE_EXPENSE
    private var selectedCategoryId: Long? = null
    private var selectedDate = System.currentTimeMillis()
    private var recordId: Long? = null

    private lateinit var expenseAdapter: CategoryAdapter
    private lateinit var incomeAdapter: CategoryAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_edit_record)

        viewModel = ViewModelProvider(this)[AddEditRecordViewModel::class.java]

        recordId = intent.getLongExtra("record_id", -1).takeIf { it != -1L }

        setupViews()
        setupCategoryAdapters()
        setupObservers()

        if (recordId != null) {
            loadRecord(recordId!!)
        }
    }

    private fun setupViews() {
        val toolbar: MaterialToolbar = findViewById(R.id.toolbar)
        toolbar.setTitle(if (recordId != null) R.string.edit_record else R.string.add_record)
        toolbar.setNavigationOnClickListener { finish() }

        tabLayout = findViewById(R.id.tabLayout)
        etAmount = findViewById(R.id.etAmount)
        rvCategories = findViewById(R.id.rvCategories)
        tvDate = findViewById(R.id.tvDate)
        etNote = findViewById(R.id.etNote)
        btnSave = findViewById(R.id.btnSave)

        tvDate.text = DateUtils.formatYearMonthDay(selectedDate)
        tvDate.setOnClickListener {
            showDatePicker()
        }

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                selectedType = if (tab?.position == 0) Category.TYPE_EXPENSE else Category.TYPE_INCOME
                switchCategoryList(selectedType)
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        btnSave.setOnClickListener { saveRecord() }
    }

    private fun setupCategoryAdapters() {
        expenseAdapter = CategoryAdapter(emptyList()) { category ->
            selectedCategoryId = category.id
        }
        incomeAdapter = CategoryAdapter(emptyList()) { category ->
            selectedCategoryId = category.id
        }

        rvCategories.layoutManager = GridLayoutManager(this, 4)
        switchCategoryList(selectedType)
    }

    private fun switchCategoryList(type: String) {
        val adapter = if (type == Category.TYPE_EXPENSE) expenseAdapter else incomeAdapter
        rvCategories.adapter = adapter
        if (type == Category.TYPE_EXPENSE && expenseAdapter.itemCount > 0) {
            // Keep the first one selected by default if nothing selected
            if (selectedCategoryId == null) {
                selectedCategoryId = 1L // Will be updated from observer
            }
        }
    }

    private fun setupObservers() {
        viewModel.expenseCategories.observe(this) { categories ->
            expenseAdapter = CategoryAdapter(categories) { category ->
                selectedCategoryId = category.id
            }
            if (selectedType == Category.TYPE_EXPENSE) {
                rvCategories.adapter = expenseAdapter
                if (selectedCategoryId == null && categories.isNotEmpty()) {
                    selectedCategoryId = categories[0].id
                }
            }
        }

        viewModel.incomeCategories.observe(this) { categories ->
            incomeAdapter = CategoryAdapter(categories) { category ->
                selectedCategoryId = category.id
            }
            if (selectedType == Category.TYPE_INCOME) {
                rvCategories.adapter = incomeAdapter
                if (selectedCategoryId == null && categories.isNotEmpty()) {
                    selectedCategoryId = categories[0].id
                }
            }
        }
    }

    private fun loadRecord(recordId: Long) {
        lifecycleScope.launch {
            val record = viewModel.loadRecord(recordId)
            record?.let {
                selectedType = it.type
                selectedCategoryId = it.categoryId
                selectedDate = it.date

                tabLayout.getTabAt(if (it.type == Category.TYPE_EXPENSE) 0 else 1)?.select()
                etAmount.setText(CurrencyUtils.formatWithoutSymbol(it.amount))
                tvDate.text = DateUtils.formatYearMonthDay(it.date)
                etNote.setText(it.note ?: "")
            }
        }
    }

    private fun showDatePicker() {
        val cal = Calendar.getInstance()
        cal.timeInMillis = selectedDate

        DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                cal.set(Calendar.YEAR, year)
                cal.set(Calendar.MONTH, month)
                cal.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                selectedDate = cal.timeInMillis
                tvDate.text = DateUtils.formatYearMonthDay(selectedDate)
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun saveRecord() {
        val amountText = etAmount.text?.toString()?.trim() ?: ""
        val amount = amountText.toDoubleOrNull() ?: 0.0

        if (amount <= 0) {
            Toast.makeText(this, "请输入有效金额", Toast.LENGTH_SHORT).show()
            return
        }

        val categoryId = selectedCategoryId
        if (categoryId == null) {
            Toast.makeText(this, "请选择分类", Toast.LENGTH_SHORT).show()
            return
        }

        val note = etNote.text?.toString()?.trim()?.takeIf { it.isNotEmpty() }

        btnSave.isEnabled = false
        viewModel.saveRecord(
            type = selectedType,
            categoryId = categoryId,
            amount = amount,
            date = selectedDate,
            note = note
        ) { success ->
            if (success) {
                Toast.makeText(this, "保存成功", Toast.LENGTH_SHORT).show()
                finish()
            } else {
                Toast.makeText(this, "保存失败", Toast.LENGTH_SHORT).show()
                btnSave.isEnabled = true
            }
        }
    }
}
