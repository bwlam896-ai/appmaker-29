package com.appmaker.app

import android.os.Build
import android.os.Bundle
import android.os.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.appmaker.app.databinding.ActivityMainBinding

// Model Representation
data class DhikrItem(
    val id: Long,
    val text: String,
    var count: Int = 0,
    val target: Int = 33,
    val category: String = "custom"
)

// ViewModel for managing Athkar Data and state
class DhikrViewModel : ViewModel() {
    private val _athkarList = MutableLiveData<MutableList<DhikrItem>>(mutableListOf())
    val athkarList: LiveData<MutableList<DhikrItem>> = _athkarList

    private val _selectedCategory = MutableLiveData<String>("all")
    val selectedCategory: LiveData<String> = _selectedCategory

    private val _currentIndex = MutableLiveData<Int>(0)
    val currentIndex: LiveData<Int> = _currentIndex

    init {
        // Seed default Athkar
        val initialData = mutableListOf(
            DhikrItem(1, "سُبْحَانَ اللَّهِ وَبِحَمْدِهِ ، سُبْحَانَ اللَّهِ الْعَظِيمِ", 0, 33, "morning"),
            DhikrItem(2, "أَسْتَغْفِرُ اللَّهَ وَأَتُوبُ إِلَيْهِ", 0, 100, "morning"),
            DhikrItem(3, "اللَّهُمَّ صَلِّ وَسَلِّمْ عَلَى نَبِيِّنَا مُحَمَّدٍ", 0, 10, "evening"),
            DhikrItem(4, "لا حَوْلَ وَلا قُوَّةَ إِلاَّ بِاللَّهِ الْعَلِيِّ الْعَظِيمِ", 0, 33, "custom")
        )
        _athkarList.value = initialData
    }

    fun getFilteredList(): List<DhikrItem> {
        val list = _athkarList.value ?: return emptyList()
        val category = _selectedCategory.value ?: "all"
        return if (category == "all") list else list.filter { it.category == category }
    }

    fun setCategory(category: String) {
        _selectedCategory.value = category
        _currentIndex.value = 0
    }

    fun incrementCount() {
        val currentList = getFilteredList()
        if (currentList.isEmpty()) return
        val index = _currentIndex.value ?: 0
        if (index in currentList.indices) {
            val item = currentList[index]
            if (item.count < item.target) {
                item.count++
                _athkarList.value = _athkarList.value // Trigger observers
            }
        }
    }

    fun resetCount() {
        val currentList = getFilteredList()
        if (currentList.isEmpty()) return
        val index = _currentIndex.value ?: 0
        if (index in currentList.indices) {
            currentList[index].count = 0
            _athkarList.value = _athkarList.value
        }
    }

    fun nextDhikr() {
        val currentList = getFilteredList()
        if (currentList.isEmpty()) return
        val curr = _currentIndex.value ?: 0
        _currentIndex.value = (curr + 1) % currentList.size
    }

    fun prevDhikr() {
        val currentList = getFilteredList()
        if (currentList.isEmpty()) return
        val curr = _currentIndex.value ?: 0
        _currentIndex.value = if (curr - 1 < 0) currentList.size - 1 else curr - 1
    }

    fun setCurrentIndex(index: Int) {
        val currentList = getFilteredList()
        if (index in currentList.indices) {
            _currentIndex.value = index
        }
    }

    fun addDhikr(text: String, target: Int, category: String) {
        val newItem = DhikrItem(
            id = System.currentTimeMillis(),
            text = text,
            count = 0,
            target = target,
            category = category
        )
        val current = _athkarList.value ?: mutableListOf()
        current.add(newItem)
        _athkarList.value = current
        _selectedCategory.value = category
        _currentIndex.value = getFilteredList().size - 1
    }
}

// Adapter for displaying list of Athkar below the main counter
class DhikrAdapter(
    private var items: List<DhikrItem>,
    private var selectedIndex: Int,
    private val onItemClick: (Int) -> Unit
) : RecyclerView.Adapter<DhikrAdapter.DhikrViewHolder>() {

    class DhikrViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvText: TextView = view.findViewById(android.R.id.text1)
        val tvCount: TextView = view.findViewById(android.R.id.text2)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DhikrViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(android.R.layout.simple_list_item_2, parent, false)
        return DhikrViewHolder(view)
    }

    override fun onBindViewHolder(holder: DhikrViewHolder, position: Int) {
        val item = items[position]
        holder.tvText.text = item.text
        holder.tvText.setTextColor(if (position == selectedIndex) 0xFFF59E0B.toInt() else 0xFFFFFFFF.toInt())
        
        holder.tvCount.text = "العدد: ${item.count} / ${item.target}"
        holder.tvCount.setTextColor(0xFFA7F3D0.toInt())

        holder.itemView.setBackgroundColor(
            if (position == selectedIndex) 0x33F59E0B.toInt() else 0x00000000
        )

        holder.itemView.setOnClickListener {
            onItemClick(position)
        }
    }

    override fun getItemCount(): Int = items.size

    fun updateData(newItems: List<DhikrItem>, newIndex: Int) {
        this.items = newItems
        this.selectedIndex = newIndex
        notifyDataSetChanged()
    }
}

// Main Activity UI logic
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: DhikrViewModel
    private lateinit var adapter: DhikrAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this)[DhikrViewModel::class.java]

        setupRecyclerView()
        setupListeners()
        setupObservers()
        setupTabs()
    }

    private fun setupRecyclerView() {
        adapter = DhikrAdapter(emptyList(), 0) { index ->
            viewModel.setCurrentIndex(index)
        }
        binding.rvDhikrList.layoutManager = LinearLayoutManager(this)
        binding.rvDhikrList.adapter = adapter
    }

    private fun setupListeners() {
        binding.btnCount.setOnClickListener { v ->
            v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            viewModel.incrementCount()
        }

        binding.btnReset.setOnClickListener {
            viewModel.resetCount()
        }

        binding.btnNext.setOnClickListener {
            viewModel.nextDhikr()
        }

        binding.btnPrev.setOnClickListener {
            viewModel.prevDhikr()
        }

        binding.btnAddDhikr.setOnClickListener {
            showAddDhikrDialog()
        }
    }

    private fun setupTabs() {
        binding.tabLayout.addOnTabSelectedListener(object : com.google.android.material.tabs.TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: com.google.android.material.tabs.TabLayout.Tab?) {
                val category = when (tab?.position) {
                    1 -> "morning"
                    2 -> "evening"
                    3 -> "custom"
                    else -> "all"
                }
                viewModel.setCategory(category)
            }

            override fun onTabUnselected(tab: com.google.android.material.tabs.TabLayout.Tab?) {}
            override fun onTabReselected(tab: com.google.android.material.tabs.TabLayout.Tab?) {}
        })
    }

    private fun setupObservers() {
        viewModel.athkarList.observe(this) { updateUI() }
        viewModel.selectedCategory.observe(this) { updateUI() }
        viewModel.currentIndex.observe(this) { updateUI() }
    }

    private fun updateUI() {
        val filteredList = viewModel.getFilteredList()
        val index = viewModel.currentIndex.value ?: 0

        if (filteredList.isEmpty()) {
            binding.tvDhikrText.text = getString(R.string.empty_list)
            binding.tvCount.text = "0"
            binding.tvTarget.text = getString(R.string.target_label, 0)
            binding.progressBar.progress = 0
            binding.tvCategoryBadge.text = "لا يوجد"
            adapter.updateData(emptyList(), 0)
            return
        }

        val safeIndex = if (index in filteredList.indices) index else 0
        val activeItem = filteredList[safeIndex]

        binding.tvDhikrText.text = activeItem.text
        binding.tvCount.text = activeItem.count.toString()
        binding.tvTarget.text = getString(R.string.target_label, activeItem.target)

        val progress = if (activeItem.target > 0) {
            ((activeItem.count.toFloat() / activeItem.target.toFloat()) * 100).toInt()
        } else 0
        binding.progressBar.progress = progress.coerceAtMost(100)

        val categoryName = when (activeItem.category) {
            "morning" -> getString(R.string.tab_morning)
            "evening" -> getString(R.string.tab_evening)
            else -> getString(R.string.tab_custom)
        }
        binding.tvCategoryBadge.text = categoryName

        adapter.updateData(filteredList, safeIndex)
    }

    private fun showAddDhikrDialog() {
        val dialogView = LayoutInflater.from(this).inflate(android.R.layout.select_dialog_item, null)
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 20, 40, 20)
        }

        val etText = EditText(this).apply {
            hint = getString(R.string.hint_dhikr_text)
            minLines = 2
        }
        val etTarget = EditText(this).apply {
            hint = getString(R.string.hint_target)
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setText("33")
        }

        val spinnerCategory = Spinner(this)
        val categories = arrayOf(
            getString(R.string.tab_morning),
            getString(R.string.tab_evening),
            getString(R.string.tab_custom)
        )
        val spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, categories)
        spinnerCategory.adapter = spinnerAdapter
        spinnerCategory.setSelection(2) // Default to custom

        layout.addView(etText)
        layout.addView(etTarget)
        layout.addView(spinnerCategory)

        AlertDialog.Builder(this)
            .setTitle(getString(R.string.add_dialog_title))
            .setView(layout)
            .setPositiveButton(getString(R.string.save)) { _, _ ->
                val text = etText.text.toString().trim()
                val targetStr = etTarget.text.toString().trim()
                val target = if (targetStr.isNotEmpty()) targetStr.toInt() else 33

                val categoryKey = when (spinnerCategory.selectedItemPosition) {
                    0 -> "morning"
                    1 -> "evening"
                    else -> "custom"
                }

                if (text.isNotEmpty()) {
                    viewModel.addDhikr(text, target, categoryKey)
                    Toast.makeText(this, "تمت إضافة الذكر بنجاح", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "يرجى كتابة نص الذكر", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }
}