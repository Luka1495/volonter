package com.volonterapp.activities

import android.app.Activity
import android.app.DatePickerDialog
import android.graphics.Color
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.GridLayoutManager
import com.volonterapp.R
import com.volonterapp.adapters.CardMemberListItemsAdapter
import com.volonterapp.dialogs.LabelColorListDialog
import com.volonterapp.dialogs.MembersListDialog
import com.volonterapp.firebase.FirestoreClass
import com.volonterapp.model.*
import com.volonterapp.utils.Constants
import kotlinx.android.synthetic.main.activity_card_details.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class CardDetailsActivity : BaseActivity() {

    private lateinit var mBoardDetails: Board
    private var mTaskListPosition: Int = -1
    private var mCardPosition: Int = -1
    private var mSelectedColor: String = ""
    private var mTypeOfWork: String = ""
    private lateinit var mMembersDetailList: ArrayList<User>
    private var mSelectedDueDateMilliSeconds: Long = 0
    private var mSelectedWorkHours: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_card_details)

        getIntentData()

        setupActionBar()

        et_name_card_details.setText(mBoardDetails.taskList[mTaskListPosition].cards[mCardPosition].name)
        et_name_card_details.setSelection(et_name_card_details.text.toString().length)

        mSelectedColor = mBoardDetails.taskList[mTaskListPosition].cards[mCardPosition].labelColor
        if (mSelectedColor.isNotEmpty()) {
            setColor()
        }

        tv_select_label_color.setOnClickListener {
            labelColorsListDialog()
        }

        setupSelectedMembersList()

        tv_select_members.setOnClickListener {
            membersListDialog()
        }
        mTypeOfWork = mBoardDetails.taskList[mTaskListPosition].cards[mCardPosition].typeOfWork
        if (mTypeOfWork.isNotEmpty()) {
            tv_select_typeofwork.text = mTypeOfWork
        } else {
            tv_select_typeofwork.text = "Odaberi kategoriju rada"
        }

        tv_select_typeofwork.setOnClickListener {
            showTypeOfWorkDialog()
        }

        mSelectedDueDateMilliSeconds =
            mBoardDetails.taskList[mTaskListPosition].cards[mCardPosition].dueDate
        if (mSelectedDueDateMilliSeconds > 0) {
            val simpleDateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH)
            val selectedDate = simpleDateFormat.format(Date(mSelectedDueDateMilliSeconds))
            tv_select_due_date.text = selectedDate
        }

        tv_select_due_date.setOnClickListener {

            showDatePicker()
        }

        mSelectedWorkHours = mBoardDetails.taskList[mTaskListPosition].cards[mCardPosition].workHours
        if (mSelectedWorkHours > 0) {
            tv_select_work_hours.text = "$mSelectedWorkHours sati"
        } else {
            tv_select_work_hours.text = "Odaberi broj sati"
        }

        tv_select_work_hours.setOnClickListener {
            showWorkHoursInputDialog()
        }

        btn_update_card_details.setOnClickListener {
            if (et_name_card_details.text.toString().isNotEmpty()) {
                updateCardDetails()
            } else {
                Toast.makeText(this@CardDetailsActivity, "Enter card name.", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_delete_card, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_delete_card -> {
                alertDialogForDeleteCard(mBoardDetails.taskList[mTaskListPosition].cards[mCardPosition].name)
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }


    private fun setupActionBar() {

        setSupportActionBar(toolbar_card_details_activity)

        val actionBar = supportActionBar
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true)
            actionBar.setHomeAsUpIndicator(R.drawable.ic_white_color_back_24dp)
            actionBar.title = mBoardDetails.taskList[mTaskListPosition].cards[mCardPosition].name
        }

        toolbar_card_details_activity.setNavigationOnClickListener { onBackPressed() }
    }

    private fun getIntentData() {

        if (intent.hasExtra(Constants.TASK_LIST_ITEM_POSITION)) {
            mTaskListPosition = intent.getIntExtra(Constants.TASK_LIST_ITEM_POSITION, -1)
        }
        if (intent.hasExtra(Constants.CARD_LIST_ITEM_POSITION)) {
            mCardPosition = intent.getIntExtra(Constants.CARD_LIST_ITEM_POSITION, -1)
        }
        if (intent.hasExtra(Constants.BOARD_DETAIL)) {
            mBoardDetails = intent.getParcelableExtra<Board>(Constants.BOARD_DETAIL) ?: Board()
        }

        if (intent.hasExtra(Constants.BOARD_MEMBERS_LIST)) {
            mMembersDetailList = intent.getParcelableArrayListExtra(Constants.BOARD_MEMBERS_LIST)!!
        }
    }


    fun addUpdateTaskListSuccess() {

        hideProgressDialog()

        setResult(Activity.RESULT_OK)
        finish()
    }


    private fun updateCardDetails() {

        val card = Card(
            et_name_card_details.text.toString(),
            mBoardDetails.taskList[mTaskListPosition].cards[mCardPosition].createdBy,
            mBoardDetails.taskList[mTaskListPosition].cards[mCardPosition].assignedTo,
            mSelectedColor,
            mSelectedDueDateMilliSeconds,
            mSelectedWorkHours,
            mTypeOfWork
        )

        val taskList: ArrayList<Task> = mBoardDetails.taskList
        taskList.removeAt(taskList.size - 1)

        mBoardDetails.taskList[mTaskListPosition].cards[mCardPosition] = card

        showProgressDialog(resources.getString(R.string.please_wait))
        FirestoreClass().addUpdateTaskList(this@CardDetailsActivity, mBoardDetails)
    }


    private fun alertDialogForDeleteCard(cardName: String) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(resources.getString(R.string.alert))
        builder.setMessage(
            resources.getString(
                R.string.confirmation_message_to_delete_card,
                cardName
            )
        )
        builder.setIcon(android.R.drawable.ic_dialog_alert)

        builder.setPositiveButton(resources.getString(R.string.yes)) { dialogInterface, which ->
            dialogInterface.dismiss()
            deleteCard()
        }
        builder.setNegativeButton(resources.getString(R.string.no)) { dialogInterface, which ->
            dialogInterface.dismiss()
        }
        val alertDialog: AlertDialog = builder.create()
        alertDialog.setCancelable(false)
        alertDialog.show()
    }


    private fun deleteCard() {

        val cardsList: ArrayList<Card> = mBoardDetails.taskList[mTaskListPosition].cards
        cardsList.removeAt(mCardPosition)

        val taskList: ArrayList<Task> = mBoardDetails.taskList
        taskList.removeAt(taskList.size - 1)

        taskList[mTaskListPosition].cards = cardsList

        showProgressDialog(resources.getString(R.string.please_wait))
        FirestoreClass().addUpdateTaskList(this@CardDetailsActivity, mBoardDetails)
    }


    private fun setColor() {
        tv_select_label_color.text = ""
        tv_select_label_color.setBackgroundColor(Color.parseColor(mSelectedColor))
    }


    private fun colorsList(): ArrayList<String> {

        val colorsList: ArrayList<String> = ArrayList()
        colorsList.add("#43C86F")
        colorsList.add("#0C90F1")
        colorsList.add("#F72400")
        colorsList.add("#7A8089")
        colorsList.add("#D57C1D")
        colorsList.add("#770000")
        colorsList.add("#0022F8")

        return colorsList
    }


    private fun labelColorsListDialog() {

        val colorsList: ArrayList<String> = colorsList()

        val listDialog = object : LabelColorListDialog(
            this@CardDetailsActivity,
            colorsList,
            resources.getString(R.string.str_select_label_color),
            mSelectedColor
        ) {
            override fun onItemSelected(color: String) {
                mSelectedColor = color
                setColor()
            }
        }
        listDialog.show()
    }


    private fun membersListDialog() {

        val cardAssignedMembersList =
            mBoardDetails.taskList[mTaskListPosition].cards[mCardPosition].assignedTo

        if (cardAssignedMembersList.size > 0) {
            for (i in mMembersDetailList.indices) {
                for (j in cardAssignedMembersList) {
                    if (mMembersDetailList[i].id == j) {
                        mMembersDetailList[i].selected = true
                    }
                }
            }
        } else {
            for (i in mMembersDetailList.indices) {
                mMembersDetailList[i].selected = false
            }
        }

        val listDialog = object : MembersListDialog(
            this@CardDetailsActivity,
            mMembersDetailList,
            resources.getString(R.string.str_select_member)
        ) {
            override fun onItemSelected(user: User, action: String) {

                if (action == Constants.SELECT) {
                    if (!mBoardDetails.taskList[mTaskListPosition].cards[mCardPosition].assignedTo.contains(
                            user.id
                        )
                    ) {
                        mBoardDetails.taskList[mTaskListPosition].cards[mCardPosition].assignedTo.add(
                            user.id
                        )
                    }
                } else {
                    mBoardDetails.taskList[mTaskListPosition].cards[mCardPosition].assignedTo.remove(
                        user.id
                    )

                    for (i in mMembersDetailList.indices) {
                        if (mMembersDetailList[i].id == user.id) {
                            mMembersDetailList[i].selected = false
                        }
                    }
                }

                setupSelectedMembersList()
            }
        }
        listDialog.show()
    }


    private fun setupSelectedMembersList() {

        val cardAssignedMembersList =
            mBoardDetails.taskList[mTaskListPosition].cards[mCardPosition].assignedTo

        val selectedMembersList: ArrayList<SelectedMembers> = ArrayList()

        for (i in mMembersDetailList.indices) {
            for (j in cardAssignedMembersList) {
                if (mMembersDetailList[i].id == j) {
                    val selectedMember = SelectedMembers(
                        mMembersDetailList[i].id,
                        mMembersDetailList[i].image
                    )

                    selectedMembersList.add(selectedMember)
                }
            }
        }

        if (selectedMembersList.size > 0) {

            selectedMembersList.add(SelectedMembers("", ""))

            tv_select_members.visibility = View.GONE
            rv_selected_members_list.visibility = View.VISIBLE

            rv_selected_members_list.layoutManager = GridLayoutManager(this@CardDetailsActivity, 6)
            val adapter =
                CardMemberListItemsAdapter(this@CardDetailsActivity, selectedMembersList, true)
            rv_selected_members_list.adapter = adapter
            adapter.setOnClickListener(object :
                CardMemberListItemsAdapter.OnClickListener {
                override fun onClick() {
                    membersListDialog()
                }
            })
        } else {
            tv_select_members.visibility = View.VISIBLE
            rv_selected_members_list.visibility = View.GONE
        }
    }


    private fun showDatePicker() {
        Calendar.getInstance().let { calendar ->
            val currentYear = calendar.get(Calendar.YEAR)
            val currentMonth = calendar.get(Calendar.MONTH)
            val currentDay = calendar.get(Calendar.DAY_OF_MONTH)

            DatePickerDialog(
                this,
                { _, selectedYear, selectedMonth, selectedDay ->
                    handleDateSelection(selectedDay, selectedMonth, selectedYear)
                },
                currentYear,
                currentMonth,
                currentDay
            ).apply {
                show()
            }
        }
    }

    private fun handleDateSelection(day: Int, month: Int, year: Int) {
        val formattedDay = String.format("%02d", day)
        val formattedMonth = String.format("%02d", month + 1)
        val dateString = "$formattedDay/$formattedMonth/$year"

        tv_select_due_date.text = dateString

        SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH).parse(dateString)?.let { parsedDate ->
            mSelectedDueDateMilliSeconds = parsedDate.time
        }
    }

    private fun showWorkHoursInputDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Unesite broj sati")

        val input = android.widget.EditText(this)
        input.inputType = android.text.InputType.TYPE_CLASS_NUMBER
        input.hint = "Broj sati"

        if (mSelectedWorkHours > 0) {
            input.setText(mSelectedWorkHours.toString())
            input.setSelection(input.text.length)
        }

        builder.setView(input)

        builder.setPositiveButton("OK") { dialog, _ ->
            val inputText = input.text.toString()
            if (inputText.isNotEmpty()) {
                val hours = inputText.toIntOrNull()
                if (hours != null && hours > -1 && hours <= 8) {
                    mSelectedWorkHours = hours
                    if (hours == 1){
                        tv_select_work_hours.text = "$mSelectedWorkHours sat"
                    }
                    else if (hours > 1 && hours < 5){
                        tv_select_work_hours.text = "$mSelectedWorkHours sata"
                    }
                    else{
                        tv_select_work_hours.text = "$mSelectedWorkHours sati"
                    }
                    dialog.dismiss()
                } else {
                    Toast.makeText(this, "Molimo unesite valjani broj sati (0-8)", Toast.LENGTH_SHORT).show()
                }
            } else {
                mSelectedWorkHours = 0
                tv_select_work_hours.text = "Odaberi broj sati"
                dialog.dismiss()
            }
        }

        builder.setNegativeButton("Otkazano") { dialog, _ ->
            dialog.cancel()
        }

        val dialog = builder.create()
        dialog.show()
    }
    private fun showTypeOfWorkDialog() {
        val categories = arrayOf(
            "Čišćenje i očuvanje okoliša",
            "Fizički rad i pomoć u zajednici",
            "Umjetnički i kreativni rad",
            "Druženje i pomoć starijima",
            "Rad s djecom i mladima",
            "Ostalo"
        )

        val builder = AlertDialog.Builder(this)
        builder.setTitle("Odaberite kategoriju rada")

        var checkedItem = -1
        if (mTypeOfWork.isNotEmpty()) {
            checkedItem = categories.indexOf(mTypeOfWork)
        }

        builder.setSingleChoiceItems(categories, checkedItem) { dialog, which ->
            mTypeOfWork = categories[which]
            tv_select_typeofwork.text = mTypeOfWork
            dialog.dismiss()
        }

        builder.setNegativeButton("Otkazano") { dialog, _ ->
            dialog.cancel()
        }

        builder.setNeutralButton("Ukloni") { dialog, _ ->
            mTypeOfWork = ""
            tv_select_typeofwork.text = "Odaberi kategoriju rada"
            dialog.dismiss()
        }

        val dialog = builder.create()
        dialog.show()
    }
}