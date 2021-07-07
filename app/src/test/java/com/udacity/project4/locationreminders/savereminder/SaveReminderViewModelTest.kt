package com.udacity.project4.locationreminders.savereminder

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.getOrAwaitValue
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.core.Is.`is`
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class SaveReminderViewModelTest {

    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    //TODO: provide testing to the SaveReminderView and its live data objects
    private lateinit var saveReminderViewModel: SaveReminderViewModel
    private lateinit var fakeDataSource: FakeDataSource


    @Before
    fun setupViewModel() {
        stopKoin()
        fakeDataSource = FakeDataSource()
        saveReminderViewModel = SaveReminderViewModel(
            ApplicationProvider.getApplicationContext(),
            fakeDataSource
        )
    }

    //Validate the entered data, returns true if its valid else return false in case of null or empty
    @Test
    fun validateEnteredData_withValidData_returnstrue() {

        //given a new reminder with valid data
        val reminder = ReminderDataItem(
            title = "New Market",
            description = "Buy Groceries",
            location = "Skull Mountain",
            latitude = -34.0,
            longitude = 151.0
        )

        //when validating
        val result = saveReminderViewModel.validateEnteredData(reminder)

        //returns true
        assertThat(result, `is`(true))

    }

    //Validate the entered data, returns true if its valid else return false in case of null or empty
    @Test
    fun validateEnteredData_withInvalidData_returnsFalse() {

        //given a new reminder with invalid data
        val reminder = ReminderDataItem(
            title = "",
            description = "Buy Groceries",
            location = "Skull Mountain",
            latitude = -34.0,
            longitude = 151.0
        )
        //when validating
        val result = saveReminderViewModel.validateEnteredData(reminder)

        //returns false
        assertThat(result, `is`(false))

    }

    //save new reminder and confirms with a toast

    @Test
    fun saveReminder_newReminderData_confirmWithToast(){

        //given a new reminder
        val reminder = ReminderDataItem(
            title = "New Market",
            description = "Buy Groceries",
            location = "Skull Mountain",
            latitude = -34.0,
            longitude = 151.0)

        //saving the reminder
        saveReminderViewModel.saveReminder(reminder)

        //then confirms with a toast
        assertThat(saveReminderViewModel.showToast.getOrAwaitValue(),`is`("Reminder Saved!"))

    }


}