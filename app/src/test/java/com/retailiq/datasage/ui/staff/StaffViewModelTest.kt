package com.retailiq.datasage.ui.staff

import com.retailiq.datasage.data.model.StaffSessionDto
import com.retailiq.datasage.data.repository.StaffRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class StaffViewModelTest {

    private lateinit var repository: StaffRepository
    private lateinit var viewModel: StaffViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = mock(StaffRepository::class.java)
        viewModel = StaffViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial session state is NoSession`() {
        assertTrue(viewModel.sessionState.value is StaffSessionState.NoSession)
    }

    @Test
    fun `startSession transitions state to Active on success`() = runTest {
        val session = StaffSessionDto("s1", "ACTIVE", Instant.now().toString(), null, 0, 0.0)
        `when`(repository.startSession()).thenReturn(Result.success(session))

        viewModel.startSession()
        advanceUntilIdle()

        val state = viewModel.sessionState.value
        assertTrue(state is StaffSessionState.Active)
        assertEquals("s1", (state as StaffSessionState.Active).session.sessionId)
    }

    @Test
    fun `startSession transitions state to Error on failure`() = runTest {
        `when`(repository.startSession()).thenReturn(Result.failure(Exception("Failed")))

        viewModel.startSession()
        advanceUntilIdle()

        val state = viewModel.sessionState.value
        assertTrue(state is StaffSessionState.Error)
        assertEquals("Failed", (state as StaffSessionState.Error).message)
    }

    @Test
    fun `endSession transitions to Ended on success`() = runTest {
        val session = StaffSessionDto("s1", "ENDED", Instant.now().toString(), Instant.now().toString(), 5, 500.0)
        `when`(repository.endSession()).thenReturn(Result.success(session))

        viewModel.endSession()
        advanceUntilIdle()

        val state = viewModel.sessionState.value
        assertTrue(state is StaffSessionState.Ended)
        assertEquals("s1", (state as StaffSessionState.Ended).session.sessionId)
    }

    @Test
    fun `resetSessionState returns to NoSession`() {
        viewModel.resetSessionState()
        assertTrue(viewModel.sessionState.value is StaffSessionState.NoSession)
    }
}
