package com.udacity.project4.locationreminders.savereminder

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.core.Is.`is`
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class SaveReminderViewModelTest {


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

        val reminder = ReminderDataItem(
            title = "New Market",
            description = "Buy Groceries",
            location = "Skull Mountain",
            latitude = -34.0,
            longitude = 151.0
        )

        val result=saveReminderViewModel.validateEnteredData(reminder)
        assertThat(result,`is`(true))

    }

    //Validate the entered data, returns true if its valid else return false in case of null or empty
    @Test
    fun validateEnteredData_withInvalidData_returnsFalse() {

        val reminder = ReminderDataItem(
            title = "",
            description = "Buy Groceries",
            location = "Skull Mountain",
            latitude = -34.0,
            longitude = 151.0
        )

        val result=saveReminderViewModel.validateEnteredData(reminder)
        assertThat(result,`is`(false))

    }


}