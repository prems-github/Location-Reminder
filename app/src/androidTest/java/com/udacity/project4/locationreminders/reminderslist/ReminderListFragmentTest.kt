package com.udacity.project4.locationreminders.reminderslist

import android.app.Application
import android.os.Bundle
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.udacity.project4.R
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.local.FakeAndroidTestDataSource
import com.udacity.project4.util.DataBindingIdlingResource
import com.udacity.project4.util.monitorFragment
import com.udacity.project4.utils.EspressoIdlingResource
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify

@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
//UI Testing
@MediumTest
class ReminderListFragmentTest {


    private lateinit var appContext: Application
    private val remindersLocalRepository = FakeAndroidTestDataSource()

    // An idling resource that waits for Data Binding to have no pending bindings.
    private val dataBindingIdlingResource = DataBindingIdlingResource()

    @Before
    fun init() {
        stopKoin()//stop the original app koin
        appContext = ApplicationProvider.getApplicationContext()
        val myModule = module {
            viewModel {

                RemindersListViewModel(
                    appContext,
                    remindersLocalRepository
                )
            }
        }
        //declare a new koin module
        startKoin {
            modules(listOf(myModule))
        }
    }

    @After
    fun tearDown() {
        stopKoin()
    }
    @Before
    fun registerIdlingResource(){
        IdlingRegistry.getInstance().register(EspressoIdlingResource.countingIdlingResource)
        IdlingRegistry.getInstance().register(dataBindingIdlingResource)
    }

    @After
    fun unregisterIdlingResource(){
        IdlingRegistry.getInstance().unregister(EspressoIdlingResource.countingIdlingResource)
        IdlingRegistry.getInstance().unregister(dataBindingIdlingResource)
    }

    //retrieves reminders from local data source and displays in UI

    @Test
    fun loadReminders_displayDataOnUI(){
        //given list of reminders in local repository
        remindersLocalRepository.addReminders(
            ReminderDTO(title = "New Market", description = "Buy Groceries",location = "Skull Mountain",
                latitude = -34.0,longitude = 151.0),
            ReminderDTO(title = "Old Market", description = "Buy Stationaries",location = "Bull Mountain",
                latitude = -34.12,longitude = 151.85)
        )
        //launch a fragment which loads data from repository
        val scenario = launchFragmentInContainer<ReminderListFragment>(Bundle(), R.style.AppTheme)
        dataBindingIdlingResource.monitorFragment(scenario)

        //check it displays the reminder
       onView(withText("New Market")).check(matches(isDisplayed()))
        Thread.sleep(2000)

    }

    //if there is no data in repository display no data text

    @Test
    fun withNoReminders_displayNoDataText(){

        val scenario = launchFragmentInContainer<ReminderListFragment>(Bundle(), R.style.AppTheme)
        dataBindingIdlingResource.monitorFragment(scenario)

        onView(withId(R.id.noDataTextView)).check(matches(isDisplayed()))
        Thread.sleep(2000)
    }

    //navigation on fab click
    @Test
    fun navigation_clickOnFabButton_navigateToSaveReminder(){

        //from home screen
        val scenario = launchFragmentInContainer<ReminderListFragment>(Bundle(), R.style.AppTheme)
        dataBindingIdlingResource.monitorFragment(scenario)
        val navController=mock(NavController::class.java)
        scenario.onFragment {
            Navigation.setViewNavController(it.view!!,navController)
        }

        //when click on fab button
        onView(withId(R.id.addReminderFAB)).perform(click())


        //verify that we navigate to SaveReminderFragment
        verify(navController).navigate(
            ReminderListFragmentDirections.toSaveReminder()
        )

    }

}