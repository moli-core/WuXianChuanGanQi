package com.passingguest.myapplication.ui.budget

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.passingguest.myapplication.R
import com.passingguest.myapplication.ui.adapter.BudgetAdapter
import com.passingguest.myapplication.util.CurrencyUtils

class BudgetFragment : Fragment() {

    private lateinit var viewModel: BudgetViewModel
    private lateinit var cardOverallBudget: MaterialCardView
    private lateinit var tvBudgetAmount: TextView
    private lateinit var progressBudget: ProgressBar
    private lateinit var tvSpent: TextView
    private lateinit var tvRemaining: TextView
    private lateinit var rvBudgets: RecyclerView
    private lateinit var btnAddBudget: MaterialButton
    private lateinit var budgetAdapter: BudgetAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_budget, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(this)[BudgetViewModel::class.java]

        setupViews(view)
        setupRecyclerView()
        setupObservers()
        setupClickListeners()
    }

    private fun setupViews(view: View) {
        cardOverallBudget = view.findViewById(R.id.cardOverallBudget)
        tvBudgetAmount = view.findViewById(R.id.tvBudgetAmount)
        progressBudget = view.findViewById(R.id.progressBudget)
        tvSpent = view.findViewById(R.id.tvSpent)
        tvRemaining = view.findViewById(R.id.tvRemaining)
        rvBudgets = view.findViewById(R.id.rvBudgets)
        btnAddBudget = view.findViewById(R.id.btnAddBudget)
    }

    private fun setupRecyclerView() {
        budgetAdapter = BudgetAdapter()
        rvBudgets.layoutManager = LinearLayoutManager(requireContext())
        rvBudgets.adapter = budgetAdapter
    }

    private fun setupObservers() {
        viewModel.overallBudgetProgress.observe(viewLifecycleOwner) { (budget, spent) ->
            tvBudgetAmount.text = CurrencyUtils.format(budget)
            tvSpent.text = "已用: ${CurrencyUtils.format(spent)}"

            val remaining = budget - spent
            tvRemaining.text = if (remaining >= 0) {
                "剩余: ${CurrencyUtils.format(remaining)}"
            } else {
                "已超支: ${CurrencyUtils.format(-remaining)}"
            }
            tvRemaining.setTextColor(
                if (remaining >= 0) resources.getColor(R.color.income_color, null)
                else resources.getColor(R.color.expense_color, null)
            )

            val progress = if (budget > 0) (spent / budget * 100).toInt() else 0
            progressBudget.progress = progress.coerceIn(0, 100)
            progressBudget.progressDrawable.setTint(
                if (progress > 100) resources.getColor(R.color.expense_color, null)
                else resources.getColor(R.color.orange, null)
            )
        }

        viewModel.budgetItems.observe(viewLifecycleOwner) { items ->
            budgetAdapter.submitList(items)
        }
    }

    private fun setupClickListeners() {
        cardOverallBudget.setOnClickListener {
            showSetBudgetDialog(null)
        }

        btnAddBudget.setOnClickListener {
            showSetBudgetDialog()
        }
    }

    private fun showSetBudgetDialog(categoryId: Long? = null) {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_set_budget, null)
        val etBudget = dialogView.findViewById<TextInputEditText>(R.id.etBudgetAmount)

        if (categoryId == null) {
            // Set overall budget - pre-fill existing
            val currentBudget = viewModel.overallBudgetProgress.value?.first ?: 0.0
            if (currentBudget > 0) {
                etBudget.setText(CurrencyUtils.formatWithoutSymbol(currentBudget))
            }
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(if (categoryId == null) "设置总预算" else "设置分类预算")
            .setView(dialogView)
            .setPositiveButton("确定") { _, _ ->
                val amount = etBudget.text?.toString()?.toDoubleOrNull() ?: return@setPositiveButton
                if (categoryId == null) {
                    viewModel.setOverallBudget(amount)
                } else {
                    viewModel.setCategoryBudget(categoryId, amount)
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    // Simple dialog for category budget selection
    private fun showSetBudgetDialog() {
        // Collect categories from ViewModel
        viewModel.categories.observe(viewLifecycleOwner) { categories ->
            val names = categories.filter { it.type == "EXPENSE" }.map { it.name }.toTypedArray()
            val ids = categories.filter { it.type == "EXPENSE" }.map { it.id }.toLongArray()

            AlertDialog.Builder(requireContext())
                .setTitle("选择分类")
                .setItems(names) { _, which ->
                    if (which < ids.size) {
                        showSetBudgetDialog(ids[which])
                    }
                }
                .setNegativeButton("取消", null)
                .show()
        }
    }
}
