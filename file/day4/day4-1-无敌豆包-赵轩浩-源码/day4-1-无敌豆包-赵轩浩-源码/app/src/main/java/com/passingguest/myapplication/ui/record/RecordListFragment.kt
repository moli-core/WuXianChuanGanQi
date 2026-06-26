package com.passingguest.myapplication.ui.record

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.passingguest.myapplication.R
import com.passingguest.myapplication.ui.addedit.AddEditRecordActivity
import com.passingguest.myapplication.ui.adapter.RecordAdapter
import com.passingguest.myapplication.util.CurrencyUtils
import kotlinx.coroutines.launch

class RecordListFragment : Fragment() {

    private lateinit var viewModel: RecordViewModel
    private lateinit var adapter: RecordAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var tvTotalIncome: TextView
    private lateinit var tvTotalExpense: TextView
    private lateinit var tvBalance: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_record_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(this)[RecordViewModel::class.java]

        setupViews(view)
        setupRecyclerView()
        setupObservers()
        setupClickListeners(view)
    }

    private fun setupViews(view: View) {
        recyclerView = view.findViewById(R.id.recyclerView)
        tvEmpty = view.findViewById(R.id.tvEmpty)
        tvTotalIncome = view.findViewById(R.id.tvTotalIncome)
        tvTotalExpense = view.findViewById(R.id.tvTotalExpense)
        tvBalance = view.findViewById(R.id.tvBalance)
    }

    private fun setupRecyclerView() {
        adapter = RecordAdapter(
            onRecordClick = { recordId -> openEditRecord(recordId) },
            onRecordLongClick = { recordId -> confirmDelete(recordId) }
        )
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter
    }

    private fun setupObservers() {
        viewModel.displayItems.observe(viewLifecycleOwner) { items ->
            adapter.submitList(items)
            tvEmpty.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
        }

        viewModel.periodSummary.observe(viewLifecycleOwner) { summary ->
            if (summary != null) {
                tvTotalIncome.text = CurrencyUtils.format(summary.totalIncome)
                tvTotalExpense.text = CurrencyUtils.format(summary.totalExpense)
                val balance = summary.totalIncome - summary.totalExpense
                tvBalance.text = CurrencyUtils.format(balance)
            }
        }
    }

    private fun setupClickListeners(view: View) {
        view.findViewById<FloatingActionButton>(R.id.fabAdd).setOnClickListener {
            openAddRecord()
        }
    }

    override fun onResume() {
        super.onResume()
        // Refresh data when returning from add/edit
    }

    private fun openAddRecord() {
        val intent = Intent(requireContext(), AddEditRecordActivity::class.java)
        startActivity(intent)
    }

    private fun openEditRecord(recordId: Long) {
        val intent = Intent(requireContext(), AddEditRecordActivity::class.java)
        intent.putExtra("record_id", recordId)
        startActivity(intent)
    }

    private fun confirmDelete(recordId: Long) {
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.confirm_delete))
            .setMessage(getString(R.string.confirm_delete))
            .setPositiveButton(getString(R.string.confirm)) { _, _ ->
                deleteRecord(recordId)
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun deleteRecord(recordId: Long) {
        viewLifecycleOwner.lifecycleScope.launch {
            val record = viewModel.getRecordById(recordId)
            if (record != null) {
                viewModel.deleteRecord(record)
            }
        }
    }
}
