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
import com.volonterapp.R

class IntegerValueFormatter : ValueFormatter() {
    override fun getFormattedValue(value: Float): String {
        return value.toInt().toString()
    }
}

class StatisticsActivity : AppCompatActivity() {

    private lateinit var barChart: BarChart
    private lateinit var pieChart: PieChart
    private lateinit var statsContainer: LinearLayout
    private lateinit var mainContainer: LinearLayout
    private lateinit var barChartTitle: TextView
    private lateinit var pieChartTitle: TextView
    private var orgSelectorContainer: LinearLayout? = null

    private var selectedView = "korisnik"
    private var selectedOrgId = "org1"

    // Data classes
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

    // Simulirani podaci
    private val userAnalytics = mapOf(
        "user1" to UserAnalytics(
            name = "Ana Marić",
            totalHours = 85,
            organizations = listOf(
                OrganizationData("Zeleni Zagreb", 32, mapOf(
                    "Čišćenje i očuvanje okoliša" to 25,
                    "Fizički rad i pomoć u zajednici" to 7
                )),
                OrganizationData("Pomoć starijima", 28, mapOf(
                    "Druženje i pomoć starijima" to 20,
                    "Fizički rad i pomoć u zajednici" to 8
                )),
                OrganizationData("Kreativni centar", 25, mapOf(
                    "Umjetnički i kreativni rad" to 18,
                    "Rad s djecom i mladima" to 7
                ))
            )
        )
    )

    private val orgAnalytics = mapOf(
        "org1" to OrgAnalytics(
            name = "Zeleni Zagreb",
            totalHours = 147,
            totalVolunteers = 15,
            categories = mapOf(
                "Čišćenje i očuvanje okoliša" to CategoryData(98, 12),
                "Fizički rad i pomoć u zajednici" to CategoryData(35, 8),
                "Ostalo" to CategoryData(14, 3)
            )
        ),
        "org2" to OrgAnalytics(
            name = "Pomoć starijima",
            totalHours = 89,
            totalVolunteers = 8,
            categories = mapOf(
                "Druženje i pomoć starijima" to CategoryData(65, 7),
                "Fizički rad i pomoć u zajednici" to CategoryData(24, 5)
            )
        ),
        "org3" to OrgAnalytics(
            name = "Kreativni centar",
            totalHours = 112,
            totalVolunteers = 12,
            categories = mapOf(
                "Umjetnički i kreativni rad" to CategoryData(68, 9),
                "Rad s djecom i mladima" to CategoryData(44, 8)
            )
        )
    )

    private val systemAnalytics = SystemAnalytics(
        totalHours = 348,
        totalVolunteers = 23,
        totalOrganizations = 3,
        categories = mapOf(
            "Čišćenje i očuvanje okoliša" to SystemCategoryData(98, 12, 1),
            "Fizički rad i pomoć u zajednici" to SystemCategoryData(59, 13, 2),
            "Umjetnički i kreativni rad" to SystemCategoryData(68, 9, 1),
            "Druženje i pomoć starijima" to SystemCategoryData(65, 7, 1),
            "Rad s djecom i mladima" to SystemCategoryData(44, 8, 1),
            "Ostalo" to SystemCategoryData(14, 3, 1)
        )
    )

    @RequiresApi(Build.VERSION_CODES.N)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_statistics)

        initViews()
        createChartTitles()
        setupNavigation()
        renderUserAnalytics()
    }

    private fun initViews() {
        barChart = findViewById(R.id.barChart)
        pieChart = findViewById(R.id.pieChart)
        statsContainer = findViewById(R.id.statsContainer)
        mainContainer = statsContainer.parent as LinearLayout
    }

    private fun createChartTitles() {
        // Bar chart naslov
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

        // Pie chart naslov
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

        // Pronađite pozicije grafova u layoutu
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

        // Dodajte naslove prije grafova
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

        // Reset svih dugmića - postavi ih na neaktivno stanje
        listOf(btnUser, btnOrg, btnSystem).forEach { btn ->
            btn.setBackgroundColor(ContextCompat.getColor(this, R.color.light_gray))
            btn.setTextColor(ContextCompat.getColor(this, R.color.dark_gray))
        }

        // Označiti aktivno dugme
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

    private fun createOrgSelector() {
        // Uklonite postojeći selector ako postoji
        orgSelectorContainer?.let { container ->
            mainContainer.removeView(container)
        }

        // Kreirajte novi container
        orgSelectorContainer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, 16)
            }
        }

        // Dodajte gumbove za svaku organizaciju
        orgAnalytics.entries.forEachIndexed { index, (orgId, org) ->
            val button = Button(this).apply {
                text = org.name
                textSize = 12f
                isAllCaps = false
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
                ).apply {
                    if (index > 0) setMargins(8, 0, 0, 0)
                }

                // Postavi OutlinedButton stil
                setBackgroundResource(android.R.drawable.btn_default)

                setOnClickListener {
                    selectedOrgId = orgId
                    updateOrgSelector()
                    renderOrgAnalytics()
                }
            }
            orgSelectorContainer?.addView(button)
        }

        // Dodajte container u glavni layout (nakon statsContainer)
        val statsIndex = mainContainer.indexOfChild(statsContainer)
        orgSelectorContainer?.let { container ->
            mainContainer.addView(container, statsIndex + 1)
        }

        updateOrgSelector()
    }

    private fun updateOrgSelector() {
        orgSelectorContainer?.let { container ->
            for (i in 0 until container.childCount) {
                val button = container.getChildAt(i) as Button
                val orgId = orgAnalytics.keys.elementAt(i)

                if (orgId == selectedOrgId) {
                    // Aktivni gumb
                    button.setBackgroundColor(ContextCompat.getColor(this, R.color.green_600))
                    button.setTextColor(Color.WHITE)
                } else {
                    // Neaktivni gumb - sada vidljiv
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
        val user = userAnalytics["user1"] ?: return

        // Uklonite org selector ako postoji
        orgSelectorContainer?.let { container ->
            mainContainer.removeView(container)
            orgSelectorContainer = null
        }

        // Kreiraj statistike kartice
        createStatsCards(listOf(
            StatCard("Ukupno sati", user.totalHours.toString(), R.color.blue_600),
            StatCard("Organizacija", user.organizations.size.toString(), R.color.green_600),
            StatCard("Kategorija rada", getUniqueCategoriesCount(user).toString(), R.color.purple_600)
        ))

        // Postavite bar chart
        setupBarChart(user)

        // Postavite pie chart
        setupPieChart(user)

        updateChartTitles("korisnik")
    }

    private fun renderOrgAnalytics() {
        val org = orgAnalytics[selectedOrgId] ?: return

        createStatsCards(listOf(
            StatCard("Ukupno sati", org.totalHours.toString(), R.color.green_600),
            StatCard("Volontera", org.totalVolunteers.toString(), R.color.blue_600),
            StatCard("Kategorija", org.categories.size.toString(), R.color.purple_600)
        ))

        // Dodajte org selector ako ne postoji
        if (orgSelectorContainer == null) {
            createOrgSelector()
        }

        setupOrgBarChart(org)
        setupOrgPieChart(org)
        updateChartTitles("organizacija")
    }

    private fun renderSystemAnalytics() {
        // Uklonite org selector ako postoji
        orgSelectorContainer?.let { container ->
            mainContainer.removeView(container)
            orgSelectorContainer = null
        }

        createStatsCards(listOf(
            StatCard("Ukupno sati", systemAnalytics.totalHours.toString(), R.color.purple_600),
            StatCard("Volontera", systemAnalytics.totalVolunteers.toString(), R.color.blue_600),
            StatCard("Organizacija", systemAnalytics.totalOrganizations.toString(), R.color.green_600)
        ))

        setupSystemBarChart()
        setupSystemPieChart()
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

        // Konfiguriraj chart
        barChart.description.isEnabled = false
        barChart.legend.isEnabled = false
        barChart.animateY(1000)

        // X-axis labeli - skrati nazive organizacija
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

        val dataSet = PieDataSet(entries, "")
        dataSet.colors = listOf(
            Color.parseColor("#22c55e"),
            Color.parseColor("#3b82f6"),
            Color.parseColor("#f59e0b"),
            Color.parseColor("#ef4444"),
            Color.parseColor("#8b5cf6"),
            Color.parseColor("#6b7280")
        )

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
        val entries = org.categories.map { (category, data) ->
            PieEntry(data.volunteers.toFloat(), category)
        }

        val dataSet = PieDataSet(entries, "")
        dataSet.colors = ColorTemplate.MATERIAL_COLORS.toList()
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

    private fun setupSystemBarChart() {
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

    private fun setupSystemPieChart() {
        val entries = systemAnalytics.categories.map { (category, data) ->
            PieEntry(data.volunteers.toFloat(), category)
        }

        val dataSet = PieDataSet(entries, "")
        dataSet.colors = listOf(
            Color.parseColor("#22c55e"),
            Color.parseColor("#3b82f6"),
            Color.parseColor("#f59e0b"),
            Color.parseColor("#ef4444"),
            Color.parseColor("#8b5cf6"),
            Color.parseColor("#6b7280")
        )
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