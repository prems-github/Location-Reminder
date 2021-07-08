package com.udacity.project4.locationreminders.data.local

import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result

//Use FakeAndroidTestDataSource that acts as a test double to the LocalDataSource
class FakeAndroidTestDataSource(var remindersList: MutableList<ReminderDTO>? = mutableListOf()) :
    ReminderDataSource {

//    TODO: Create a fake data source to act as a double to the real data source


    override suspend fun getReminders(): Result<List<ReminderDTO>> {

        remindersList?.let {
            return Result.Success(ArrayList(remindersList))
        }
        return Result.Error("Reminders not found")
    }

    override suspend fun saveReminder(reminder: ReminderDTO) {
        remindersList?.add(reminder)
    }

    override suspend fun getReminder(id: String): Result<ReminderDTO> {

        val reminder = remindersList?.find { it.id == id }
        return if (reminder != null)
            Result.Success(reminder)
        else Result.Error("Reminder not found")
    }

    override suspend fun deleteAllReminders() {
        remindersList?.clear()
    }

    fun addReminders(vararg reminders: ReminderDTO) {
        for (reminder in reminders) {
            remindersList?.add(reminder)
        }
    }

}