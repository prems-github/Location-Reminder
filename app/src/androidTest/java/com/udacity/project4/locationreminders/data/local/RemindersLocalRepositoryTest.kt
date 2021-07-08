package com.udacity.project4.locationreminders.data.local

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.matcher.ViewMatchers.assertThat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.`is`
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
//Medium Test to test the repository
@MediumTest
class RemindersLocalRepositoryTest {


    // Executes each task synchronously using Architecture Components.
    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    private lateinit var localDataSource: RemindersLocalRepository
    private lateinit var database: RemindersDatabase

    @Before
    fun setup() {

        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            RemindersDatabase::class.java
        ).allowMainThreadQueries().build()

        localDataSource = RemindersLocalRepository(database.reminderDao(), Dispatchers.Main)
    }

    @After
    fun cleanUp() {
        database.close()
    }

    @Test
    fun saveReminder_retrieveReminderById() = runBlocking {
        // GIVEN - save a reminder.
        val reminder = ReminderDTO(
            title = "New Market", description = "Buy Groceries", location = "Skull Mountain",
            latitude = -34.0, longitude = 151.0
        )
        localDataSource.saveReminder(reminder)

        // WHEN - Get the reminder by id from the database.
        val result = localDataSource.getReminder(reminder.id)

        // THEN - The loaded data contains the expected values.
        assertThat(result is Result.Success, `is`(true))
        result as Result.Success
        assertThat(result.data.id, `is`(reminder.id))
        assertThat(result.data.title, `is`(reminder.title))
        assertThat(result.data.description, `is`(reminder.description))
        assertThat(result.data.location, `is`(reminder.location))
    }

    @Test
    fun deleteAllReminders_tryRetrivingReminder_returnsError() = runBlocking{
        // GIVEN - save a reminder and delete all reminders.
        val reminder = ReminderDTO(
            title = "New Market", description = "Buy Groceries", location = "Skull Mountain",
            latitude = -34.0, longitude = 151.0
        )
        localDataSource.saveReminder(reminder)
        localDataSource.deleteAllReminders()

        // retrive reminder
       val result= localDataSource.getReminder(reminder.id)

        assertThat(result is Result.Error,  `is`(true))
        result as Result.Error
        assertThat(result.message,`is`("Reminder not found!"))
    }

}