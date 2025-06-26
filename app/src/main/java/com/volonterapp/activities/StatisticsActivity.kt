package com.volonterapp.activities

import android.graphics.Color
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.firebase.firestore.FirebaseFirestore
import com.volonterapp.R

class IntegerValueFormatter : ValueFormatter() {
    override fun getFormattedValue(value: Float): String {
        return value.toInt().toString()
    }
}

// Firebase data models
data class FirebaseUser(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val mobile: String = "",
    val image: String = "",
    val fcmToken: String = "",
    val selected: Boolean = false,
    val stability: Int = 0
)

data class FirebaseCard(
    val name: String = "",
    val assignedTo: List<String> = emptyList(),
    val createdBy: String = "",
    val dueDate: Long = 0,
    val labelColor: String = "",
    val typeOfWork: String = "",
    val workHours: Int = 0,
    val stability: Int = 0
)

data class FirebaseTaskList(
    val cards: List<FirebaseCard> = emptyList(),
    val createdBy: String = "",
    val stability: Int = 0
)

data class FirebaseBoard(
    val name: String = "",
    val assignedTo: List<String> = emptyList(),
    val createdBy: String = "",
    val documentId: String = "",
    val image: String = "",
    val taskList: List<FirebaseTaskList> = emptyList(),
    val stability: Int = 0
)

class StatisticsActivity : AppCompatActivity() {

    private lateinit var barChart: BarChart
    private lateinit var pieChart: PieChart
    private lateinit var statsContainer: LinearLayout
    private lateinit var mainContainer: LinearLayout
    private lateinit var barChartTitle: TextView
    private lateinit var pieChartTitle: TextView
    private var orgSelectorContainer: LinearLayout? = null

    private var selectedView = ""
    private var selectedOrgId = ""

    private val firestore = FirebaseFirestore.getInstance()

    private val currentUserId = "tqDBZJXz0fctheVt1FvpSXWnjgC2" // Luka's ID

    private var allUsers = listOf<FirebaseUser>()
    private var allBoards = listOf<FirebaseBoard>()

    data class UserAnalytics(
        val name: String,
        val totalHours: Int,
        val organizations: List<OrganizationData>
    )

    data class OrganizationData(
        val name: String,
        val hours: Int,
        val categories: Map<String, Int>
    )

    data class OrgAnalytics(
        val name: String,
        val totalHours: Int,
        val totalVolunteers: Int,
        val categories: Map<String, CategoryData>
    )

    data class CategoryData(
        val hours: Int,
        val volunteers: Int
    )

    data class SystemAnalytics(
        val totalHours: Int,
        val totalVolunteers: Int,
        val totalOrganizations: Int,
        val categories: Map<String, SystemCategoryData>
    )

    data class SystemCategoryData(
        val hours: Int,
        val volunteers: Int,
        val organizations: Int
    )

    data class StatCard(
        val label: String,
        val value: String,
        val colorRes: Int
    )

    @RequiresApi(Build.VERSION_CODES.N)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_statistics)

        initViews()
        createChartTitles()
        setupNavigation()

        showEmptyState()

        loadFirebaseData()
    }

    private fun showEmptyState() {
        createStatsCards(listOf(
            StatCard("Odaberi prikaz", "0", R.color.light_gray),
            StatCard("za podatke", "0", R.color.light_gray),
            StatCard("", "0", R.color.light_gray)
        ))

        barChart.clear()
        pieChart.clear()
        barChart.invalidate()
        pieChart.invalidate()

        barChartTitle.text = "Odaberi prikaz statistika"
        pieChartTitle.text = "Odaberi prikaz statistika"
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun loadFirebaseData() {
        firestore.collection("users")
            .get()
            .addOnSuccessListener { userDocuments ->
                allUsers = userDocuments.map { doc ->
                    doc.toObject(FirebaseUser::class.java).copy(id = doc.id)
                }

                firestore.collection("boards")
                    .get()
                    .addOnSuccessListener { boardDocuments ->
                        allBoards = boardDocuments.map { doc ->
                            doc.toObject(FirebaseBoard::class.java).copy(documentId = doc.id)
                        }

                        updateNavigationButtons()
                    }
                    .addOnFailureListener { e ->
                        e.printStackTrace()
                    }
            }
            .addOnFailureListener { e ->
                e.printStackTrace()
            }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun generateUserAnalytics(userId: String): UserAnalytics? {
        val user = allUsers.find { it.id == userId } ?: return null

        val organizations = mutableListOf<OrganizationData>()

        allBoards.filter { board ->
            board.assignedTo.contains(userId)
        }.forEach { board ->
            var totalBoardHours = 0
            val categoryHours = mutableMapOf<String, Int>()

            board.taskList.forEach { taskList ->
                taskList.cards.filter { card ->
                    card.assignedTo.contains(userId)
                }.forEach { card ->
                    totalBoardHours += card.workHours

                    val category = if (card.typeOfWork.isNotEmpty()) {
                        card.typeOfWork
                    } else {
                        "Ostalo"
                    }

                    categoryHours[category] = categoryHours.getOrDefault(category, 0) + card.workHours
                }
            }

            if (totalBoardHours > 0) {
                organizations.add(OrganizationData(
                    name = board.name,
                    hours = totalBoardHours,
                    categories = categoryHours
                ))
            }
        }

        val totalHours = organizations.sumOf { it.hours }

        return UserAnalytics(
            name = user.name,
            totalHours = totalHours,
            organizations = organizations
        )
    }

    private fun generateOrgAnalytics(boardId: String): OrgAnalytics? {
        val board = allBoards.find { it.documentId == boardId } ?: return null

        val categories = mutableMapOf<String, CategoryData>()
        val uniqueVolunteers = mutableSetOf<String>()
        val volunteersPerCategory = mutableMapOf<String, MutableSet<String>>()

        board.taskList.forEach { taskList ->
            taskList.cards.forEach { card ->
                val category = if (card.typeOfWork.isNotEmpty()) {
                    card.typeOfWork
                } else {
                    "Ostalo"
                }

                card.assignedTo.forEach { userId ->
                    uniqueVolunteers.add(userId)
                    volunteersPerCategory.getOrPut(category) { mutableSetOf() }.add(userId)
                }
            }
        }

        board.taskList.forEach { taskList ->
            taskList.cards.forEach { card ->
                val category = if (card.typeOfWork.isNotEmpty()) {
                    card.typeOfWork
                } else {
                    "Ostalo"
                }

                val existingData = categories[category] ?: CategoryData(0, 0)
                categories[category] = CategoryData(
                    hours = existingData.hours + card.workHours,
                    volunteers = volunteersPerCategory[category]?.size ?: 0
                )
            }
        }

        val totalHours = categories.values.sumOf { it.hours }

        return OrgAnalytics(
            name = board.name,
            totalHours = totalHours,
            totalVolunteers = uniqueVolunteers.size,
            categories = categories
        )
    }

    private fun generateSystemAnalytics(): SystemAnalytics {
        val categories = mutableMapOf<String, SystemCategoryData>()
        val uniqueVolunteers = mutableSetOf<String>()
        val organizationsPerCategory = mutableMapOf<String, MutableSet<String>>()
        val volunteersPerCategory = mutableMapOf<String, MutableSet<String>>()

        allBoards.forEach { board ->
            board.taskList.forEach { taskList ->
                taskList.cards.forEach { card ->
                    val category = if (card.typeOfWork.isNotEmpty()) {
                        card.typeOfWork
                    } else {
                        "Ostalo"
                    }

                    card.assignedTo.forEach { userId ->
                        uniqueVolunteers.add(userId)
                        volunteersPerCategory.getOrPut(category) { mutableSetOf() }.add(userId)
                    }

                    organizationsPerCategory.getOrPut(category) { mutableSetOf() }.add(board.documentId)
                }
            }
        }

        allBoards.forEach { board ->
            board.taskList.forEach { taskList ->
                taskList.cards.forEach { card ->
                    val category = if (card.typeOfWork.isNotEmpty()) {
                        card.typeOfWork
                    } else {
                        "Ostalo"
                    }

                    val existingData = categories[category] ?: SystemCategoryData(0, 0, 0)
                    categories[category] = SystemCategoryData(
                        hours = existingData.hours + card.workHours, // ISPRAVKA: samo workHours
                        volunteers = volunteersPerCategory[category]?.size ?: 0,
                        organizations = organizationsPerCategory[category]?.size ?: 0
                    )
                }
            }
        }

        val totalHours = categories.values.sumOf { it.hours }

        return SystemAnalytics(
            totalHours = totalHours,
            totalVolunteers = uniqueVolunteers.size,
            totalOrganizations = allBoards.size,
            categories = categories
        )
    }


    private fun initViews() {
        barChart = findViewById(R.id.barChart)
        pieChart = findViewById(R.id.pieChart)
        statsContainer = findViewById(R.id.statsContainer)
        mainContainer = statsContainer.parent as LinearLayout
    }

    private fun createChartTitles() {
        barChartTitle = TextView(this).apply {
            textSize = 16f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.BLACK)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 24, 0, 8)
            }
        }

        pieChartTitle = TextView(this).apply {
            textSize = 16f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.BLACK)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 24, 0, 8)
            }
        }

        var barChartIndex = -1
        var pieChartIndex = -1

        for (i in 0 until mainContainer.childCount) {
            val child = mainContainer.getChildAt(i)
            if (child is CardView) {
                if (barChartIndex == -1) {
                    barChartIndex = i
                } else {
                    pieChartIndex = i
                    break
                }
            }
        }

        if (barChartIndex != -1) {
            mainContainer.addView(barChartTitle, barChartIndex)
        }
        if (pieChartIndex != -1) {
            mainContainer.addView(pieChartTitle, pieChartIndex + 1) // +1 jer je barChartTitle dodao jedan element
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun setupNavigation() {
        findViewById<Button>(R.id.btnUserStats).setOnClickListener {
            selectedView = "korisnik"
            updateNavigationButtons()
            renderUserAnalytics()
        }

        findViewById<Button>(R.id.btnOrgStats).setOnClickListener {
            selectedView = "organizacija"
            updateNavigationButtons()
            renderOrgAnalytics()
        }

        findViewById<Button>(R.id.btnSystemStats).setOnClickListener {
            selectedView = "sustav"
            updateNavigationButtons()
            renderSystemAnalytics()
        }
    }

    private fun updateNavigationButtons() {
        val btnUser = findViewById<Button>(R.id.btnUserStats)
        val btnOrg = findViewById<Button>(R.id.btnOrgStats)
        val btnSystem = findViewById<Button>(R.id.btnSystemStats)

        listOf(btnUser, btnOrg, btnSystem).forEach { btn ->
            btn.setBackgroundColor(ContextCompat.getColor(this, R.color.light_gray))
            btn.setTextColor(ContextCompat.getColor(this, R.color.dark_gray))
        }

        when (selectedView) {
            "korisnik" -> {
                btnUser.setBackgroundColor(ContextCompat.getColor(this, R.color.blue_600))
                btnUser.setTextColor(Color.WHITE)
            }
            "organizacija" -> {
                btnOrg.setBackgroundColor(ContextCompat.getColor(this, R.color.green_600))
                btnOrg.setTextColor(Color.WHITE)
            }
            "sustav" -> {
                btnSystem.setBackgroundColor(ContextCompat.getColor(this, R.color.purple_600))
                btnSystem.setTextColor(Color.WHITE)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun createOrgSelector() {
        orgSelectorContainer?.let { container ->
            mainContainer.removeView(container)
        }

        orgSelectorContainer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, 16)
            }
        }

        val userBoards = allBoards.filter { board ->
            board.assignedTo.contains(currentUserId)
        }

        userBoards.forEachIndexed { index, board ->
            val button = Button(this).apply {
                text = board.name
                textSize = 12f
                isAllCaps = false
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
                ).apply {
                    if (index > 0) setMargins(8, 0, 0, 0)
                }

                setBackgroundColor(ContextCompat.getColor(this@StatisticsActivity, R.color.light_gray))
                setTextColor(ContextCompat.getColor(this@StatisticsActivity, R.color.dark_gray))

                setOnClickListener {
                    selectedOrgId = board.documentId
                    updateOrgSelector()
                    renderOrgAnalytics()
                }
            }
            orgSelectorContainer?.addView(button)
        }

        val statsIndex = mainContainer.indexOfChild(statsContainer)
        orgSelectorContainer?.let { container ->
            mainContainer.addView(container, statsIndex + 1)
        }

        // Postavi prvi board kao selektiran ako nijedan nije postavljen
        if (selectedOrgId.isEmpty() && userBoards.isNotEmpty()) {
            selectedOrgId = userBoards.first().documentId
        }

        updateOrgSelector()
    }

    private fun updateOrgSelector() {
        orgSelectorContainer?.let { container ->
            val userBoards = allBoards.filter { board ->
                board.assignedTo.contains(currentUserId)
            }

            for (i in 0 until container.childCount) {
                val button = container.getChildAt(i) as Button
                val boardId = userBoards[i].documentId

                if (boardId == selectedOrgId) {
                    button.setBackgroundColor(ContextCompat.getColor(this, R.color.green_600))
                    button.setTextColor(Color.WHITE)
                } else {
                    button.setBackgroundColor(ContextCompat.getColor(this, R.color.light_gray))
                    button.setTextColor(ContextCompat.getColor(this, R.color.dark_gray))
                }
            }
        }
    }

    private fun updateChartTitles(viewType: String) {
        when (viewType) {
            "korisnik" -> {
                barChartTitle.text = "Broj sati po organizaciji"
                pieChartTitle.text = "Broj sati po kategoriji rada"
            }
            "organizacija" -> {
                barChartTitle.text = "Broj sati po kategoriji rada"
                pieChartTitle.text = "Broj volontera po kategoriji rada"
            }
            "sustav" -> {
                barChartTitle.text = "Broj sati po kategoriji rada (sustav)"
                pieChartTitle.text = "Broj volontera po kategoriji rada (sustav)"
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun renderUserAnalytics() {
        if (selectedView != "korisnik") return

        val userAnalytics = generateUserAnalytics(currentUserId)
        if (userAnalytics == null) {
            showEmptyState()
            return
        }

        orgSelectorContainer?.let { container ->
            mainContainer.removeView(container)
            orgSelectorContainer = null
        }

        createStatsCards(listOf(
            StatCard("Ukupno sati", userAnalytics.totalHours.toString(), R.color.blue_600),
            StatCard("Organizacija", userAnalytics.organizations.size.toString(), R.color.green_600),
            StatCard("Kategorija rada", getUniqueCategoriesCount(userAnalytics).toString(), R.color.purple_600)
        ))

        setupBarChart(userAnalytics)
        setupPieChart(userAnalytics)
        updateChartTitles("korisnik")
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun renderOrgAnalytics() {
        if (selectedView != "organizacija") return

        if (orgSelectorContainer == null) {
            createOrgSelector()
        }

        if (selectedOrgId.isEmpty()) {
            val userBoards = allBoards.filter { board ->
                board.assignedTo.contains(currentUserId)
            }
            if (userBoards.isNotEmpty()) {
                selectedOrgId = userBoards.first().documentId
                updateOrgSelector()
            } else {
                createStatsCards(listOf(
                    StatCard("Ukupno sati", "0", R.color.green_600),
                    StatCard("Volontera", "0", R.color.blue_600),
                    StatCard("Kategorija", "0", R.color.purple_600)
                ))

                barChart.clear()
                pieChart.clear()
                updateChartTitles("organizacija")
                return
            }
        }

        val orgAnalytics = generateOrgAnalytics(selectedOrgId)
        if (orgAnalytics == null) {
            createStatsCards(listOf(
                StatCard("Ukupno sati", "0", R.color.green_600),
                StatCard("Volontera", "0", R.color.blue_600),
                StatCard("Kategorija", "0", R.color.purple_600)
            ))

            barChart.clear()
            pieChart.clear()
            updateChartTitles("organizacija")
            return
        }

        createStatsCards(listOf(
            StatCard("Ukupno sati", orgAnalytics.totalHours.toString(), R.color.green_600),
            StatCard("Volontera", orgAnalytics.totalVolunteers.toString(), R.color.blue_600),
            StatCard("Kategorija", orgAnalytics.categories.size.toString(), R.color.purple_600)
        ))

        setupOrgBarChart(orgAnalytics)
        setupOrgPieChart(orgAnalytics)
        updateChartTitles("organizacija")
    }

    private fun renderSystemAnalytics() {
        if (selectedView != "sustav") return

        orgSelectorContainer?.let { container ->
            mainContainer.removeView(container)
            orgSelectorContainer = null
        }

        val systemAnalytics = generateSystemAnalytics()

        createStatsCards(listOf(
            StatCard("Ukupno sati", systemAnalytics.totalHours.toString(), R.color.purple_600),
            StatCard("Volontera", systemAnalytics.totalVolunteers.toString(), R.color.blue_600),
            StatCard("Organizacija", systemAnalytics.totalOrganizations.toString(), R.color.green_600)
        ))

        setupSystemBarChart(systemAnalytics)
        setupSystemPieChart(systemAnalytics)
        updateChartTitles("sustav")
    }

    private fun getUniqueCategoriesCount(user: UserAnalytics): Int {
        val allCategories = mutableSetOf<String>()
        user.organizations.forEach { org ->
            allCategories.addAll(org.categories.keys)
        }
        return allCategories.size
    }

    private fun setupBarChart(user: UserAnalytics) {
        val entries = user.organizations.mapIndexed { index, org ->
            BarEntry(index.toFloat(), org.hours.toFloat())
        }

        val dataSet = BarDataSet(entries, "Sati rada")
        dataSet.color = ContextCompat.getColor(this, R.color.blue_600)
        dataSet.valueTextSize = 12f
        dataSet.valueFormatter = IntegerValueFormatter()

        val data = BarData(dataSet)
        barChart.data = data

        barChart.description.isEnabled = false
        barChart.legend.isEnabled = false
        barChart.animateY(1000)

        val xAxis = barChart.xAxis
        val shortNames = user.organizations.map {
            if (it.name.length > 10) it.name.take(8) + "..." else it.name
        }
        xAxis.valueFormatter = IndexAxisValueFormatter(shortNames)
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.granularity = 1f
        xAxis.setDrawGridLines(false)
        xAxis.textSize = 10f
        xAxis.labelRotationAngle = -45f
        xAxis.setLabelCount(user.organizations.size, false)

        barChart.setFitBars(true)
        barChart.setScaleEnabled(true)
        barChart.isDragEnabled = true

        barChart.invalidate()
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun setupPieChart(user: UserAnalytics) {
        val categoryHours = mutableMapOf<String, Int>()

        user.organizations.forEach { org ->
            org.categories.forEach { (category, hours) ->
                categoryHours[category] = categoryHours.getOrDefault(category, 0) + hours
            }
        }

        val entries = categoryHours.map { (category, hours) ->
            PieEntry(hours.toFloat(), category)
        }

        if (entries.isEmpty()) {
            pieChart.clear()
            pieChart.invalidate()
            return
        }

        val dataSet = PieDataSet(entries, "")
        dataSet.colors = entries.indices.map { index ->
            val hue = (index * 360f / entries.size) % 360f
            Color.HSVToColor(floatArrayOf(hue, 0.7f, 0.9f))
        }

        dataSet.valueFormatter = IntegerValueFormatter()

        val data = PieData(dataSet)
        data.setValueTextSize(14f)
        data.setValueTextColor(Color.WHITE)

        pieChart.data = data
        pieChart.description.isEnabled = false
        pieChart.setDrawEntryLabels(false)

        val legend = pieChart.legend
        legend.isEnabled = true
        legend.verticalAlignment = Legend.LegendVerticalAlignment.BOTTOM
        legend.horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
        legend.orientation = Legend.LegendOrientation.HORIZONTAL
        legend.setDrawInside(false)
        legend.textSize = 12f
        legend.textColor = Color.BLACK
        legend.xEntrySpace = 7f
        legend.yEntrySpace = 5f
        legend.formSize = 10f
        legend.form = Legend.LegendForm.CIRCLE
        legend.isWordWrapEnabled = true
        legend.maxSizePercent = 0.95f

        pieChart.animateY(1000)
        pieChart.invalidate()
    }

    private fun setupOrgBarChart(org: OrgAnalytics) {
        val entries = org.categories.entries.mapIndexed { index, (_, data) ->
            BarEntry(index.toFloat(), data.hours.toFloat())
        }

        val dataSet = BarDataSet(entries, "Sati rada")
        dataSet.color = ContextCompat.getColor(this, R.color.green_600)
        dataSet.valueTextSize = 12f
        dataSet.valueFormatter = IntegerValueFormatter()

        val barData = BarData(dataSet)
        barChart.data = barData

        val xAxis = barChart.xAxis
        val shortCategories = org.categories.keys.map {
            if (it.length > 15) it.take(12) + "..." else it
        }
        xAxis.valueFormatter = IndexAxisValueFormatter(shortCategories)
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.granularity = 1f
        xAxis.setDrawGridLines(false)
        xAxis.textSize = 9f
        xAxis.labelRotationAngle = -45f
        xAxis.setLabelCount(org.categories.size, false)

        barChart.description.isEnabled = false
        barChart.legend.isEnabled = false
        barChart.animateY(1000)
        barChart.setFitBars(true)
        barChart.setScaleEnabled(true)
        barChart.isDragEnabled = true
        barChart.invalidate()
    }

    private fun setupOrgPieChart(org: OrgAnalytics) {
        val entries = org.categories.filter { it.value.volunteers > 0 }.map { (category, data) ->
            PieEntry(data.volunteers.toFloat(), category)
        }

        if (entries.isEmpty()) {
            pieChart.clear()
            pieChart.invalidate()
            return
        }

        val dataSet = PieDataSet(entries, "")
        dataSet.colors = entries.indices.map { index ->
            val hue = (index * 360f / entries.size) % 360f
            Color.HSVToColor(floatArrayOf(hue, 0.7f, 0.9f))
        }
        dataSet.valueFormatter = IntegerValueFormatter()

        val data = PieData(dataSet)
        data.setValueTextSize(14f)
        data.setValueTextColor(Color.WHITE)

        pieChart.data = data
        pieChart.description.isEnabled = false
        pieChart.setDrawEntryLabels(false)

        val legend = pieChart.legend
        legend.isEnabled = true
        legend.verticalAlignment = Legend.LegendVerticalAlignment.BOTTOM
        legend.horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
        legend.orientation = Legend.LegendOrientation.HORIZONTAL
        legend.setDrawInside(false)
        legend.textSize = 12f
        legend.textColor = Color.BLACK
        legend.xEntrySpace = 7f
        legend.yEntrySpace = 5f
        legend.formSize = 10f
        legend.form = Legend.LegendForm.CIRCLE
        legend.isWordWrapEnabled = true
        legend.maxSizePercent = 0.95f

        pieChart.animateY(1000)
        pieChart.invalidate()
    }

    private fun setupSystemBarChart(systemAnalytics: SystemAnalytics) {
        val entries = systemAnalytics.categories.entries.mapIndexed { index, (_, data) ->
            BarEntry(index.toFloat(), data.hours.toFloat())
        }

        val dataSet = BarDataSet(entries, "Sati rada")
        dataSet.color = ContextCompat.getColor(this, R.color.purple_600)
        dataSet.valueTextSize = 12f
        dataSet.valueFormatter = IntegerValueFormatter()

        val barData = BarData(dataSet)
        barChart.data = barData

        val xAxis = barChart.xAxis
        val shortCategories = systemAnalytics.categories.keys.map {
            if (it.length > 15) it.take(12) + "..." else it
        }
        xAxis.valueFormatter = IndexAxisValueFormatter(shortCategories)
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.granularity = 1f
        xAxis.setDrawGridLines(false)
        xAxis.textSize = 9f
        xAxis.labelRotationAngle = -45f
        xAxis.setLabelCount(systemAnalytics.categories.size, false)

        barChart.description.isEnabled = false
        barChart.legend.isEnabled = false
        barChart.animateY(1000)
        barChart.setFitBars(true)
        barChart.setScaleEnabled(true)
        barChart.isDragEnabled = true
        barChart.invalidate()
    }

    private fun setupSystemPieChart(systemAnalytics: SystemAnalytics) {
        val entries = systemAnalytics.categories.map { (category, data) ->
            PieEntry(data.volunteers.toFloat(), category)
        }


        val dataSet = PieDataSet(entries, "")
        dataSet.colors = entries.indices.map { index ->
            val hue = (index * 360f / entries.size) % 360f
            Color.HSVToColor(floatArrayOf(hue, 0.7f, 0.9f))
        }
        dataSet.valueFormatter = IntegerValueFormatter()

        val data = PieData(dataSet)
        data.setValueTextSize(14f)
        data.setValueTextColor(Color.WHITE)

        pieChart.data = data
        pieChart.description.isEnabled = false
        pieChart.setDrawEntryLabels(false)

        val legend = pieChart.legend
        legend.isEnabled = true
        legend.verticalAlignment = Legend.LegendVerticalAlignment.BOTTOM
        legend.horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
        legend.orientation = Legend.LegendOrientation.HORIZONTAL
        legend.setDrawInside(false)
        legend.textSize = 12f
        legend.textColor = Color.BLACK
        legend.xEntrySpace = 7f
        legend.yEntrySpace = 5f
        legend.formSize = 10f
        legend.form = Legend.LegendForm.CIRCLE
        legend.isWordWrapEnabled = true
        legend.maxSizePercent = 0.95f

        pieChart.animateY(1000)
        pieChart.invalidate()
    }

    private fun createStatsCards(stats: List<StatCard>) {
        statsContainer.removeAllViews()

        stats.forEach { stat ->
            val cardView = createStatCard(stat)
            val params = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
            params.setMargins(8, 0, 8, 0)
            cardView.layoutParams = params
            statsContainer.addView(cardView)
        }
    }

    private fun createStatCard(stat: StatCard): CardView {
        val cardView = CardView(this).apply {
            radius = 12f
            cardElevation = 4f
            setCardBackgroundColor(Color.WHITE)
            useCompatPadding = true
        }

        val linearLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 24, 24, 24)
        }

        val valueText = TextView(this).apply {
            text = stat.value
            textSize = 24f
            setTypeface(null, Typeface.BOLD)
            setTextColor(ContextCompat.getColor(context, stat.colorRes))
        }

        val labelText = TextView(this).apply {
            text = stat.label
            textSize = 14f
            setTextColor(Color.parseColor("#6B7280"))
        }

        linearLayout.addView(valueText)
        linearLayout.addView(labelText)
        cardView.addView(linearLayout)

        return cardView
    }
}