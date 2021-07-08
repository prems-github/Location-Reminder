package com.udacity.project4.locationreminders.data

import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result

//Use FakeDataSource that acts as a test double to the LocalDataSource
class FakeDataSource(var remindersList: MutableList<ReminderDTO>? = mutableListOf()) :
    ReminderDataSource {

//    TODO: Create a fake data source to act as a double to the real data source

    private var shouldReturnError = false

    fun setShouldReturnError(value: Boolean) {
        shouldReturnError = value
    }

    override suspend fun getReminders(): Result<List<ReminderDTO>> {

        if (shouldReturnError) {
            return Result.Error("Reminders not found")
        }

        remindersList?.let {
            return Result.Success(ArrayList(remindersList))
        }
        return Result.Error("Reminders not found")
    }

    override suspend fun saveReminder(reminder: ReminderDTO) {
        remindersList?.add(reminder)
    }

    override suspend fun getReminder(id: String): Result<ReminderDTO> {
        if (shouldReturnError) {
            return Result.Error("Reminder not found")
        }

        val reminder = remindersList?.find { it.id == id }
        return if (reminder != null)
            Result.Success(reminder)
        else Result.Error("Reminder not found")
    }

    override suspend fun deleteAllReminders() {
        remindersList?.clear()
    }


}