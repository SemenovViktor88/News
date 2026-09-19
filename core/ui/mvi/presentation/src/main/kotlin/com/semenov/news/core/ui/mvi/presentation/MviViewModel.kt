package com.semenov.news.core.ui.mvi.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.semenov.news.core.domain.AppLogger
import com.semenov.news.core.ui.mvi.domain.model.UiEffect
import com.semenov.news.core.ui.mvi.domain.model.UiIntent
import com.semenov.news.core.ui.mvi.domain.model.UiPartial
import com.semenov.news.core.ui.mvi.domain.model.UiState
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.cancellation.CancellationException

abstract class MviViewModel<State : UiState, Partial : UiPartial, Intent : UiIntent, Effect : UiEffect>(
    initialState: State,
    protected val logger: AppLogger,
) : ViewModel() {
    private val initialized = AtomicBoolean(false)
    private val _state = MutableStateFlow(initialState)
    val state: StateFlow<State> by lazy(mode = LazyThreadSafetyMode.NONE) {
        initialize()
        _state.asStateFlow()
    }

    protected open val scope: CoroutineScope
        get() = viewModelScope

    private val _effect = MutableSharedFlow<Effect>(extraBufferCapacity = 8)
    val effect: SharedFlow<Effect> = _effect.asSharedFlow()

    protected val tag: String get() = this::class.simpleName ?: "BaseMviHost"

    protected val handler: CoroutineExceptionHandler =
        CoroutineExceptionHandler { _, throwable ->
            if (throwable is CancellationException) return@CoroutineExceptionHandler
            logger.log(tag, "Unhandled error", throwable)
            launch {
                handleError(throwable)
            }
        }

    fun initialize() {
        if (!initialized.compareAndSet(false, true)) return
        logger.log(tag, "Initialize")
        launch {
            runCatching { processInit() }
                .onFailure { throwable ->
                    if (throwable is CancellationException) throw throwable
                    logError(throwable, source = "initialize function")
                    handleError(throwable)
                }
        }
    }

    fun logError(
        throwable: Throwable,
        source: String,
        additional: String = "",
    ) {
        if (throwable is CancellationException) return
        logger.log(tag, "An error thrown in $tag in $source\n$additional", throwable)
    }

    // ── Public API ────────────────────────────────────────────────────────────

    fun processIntent(intent: Intent) {
        launch {
            logger.log(tag, "Intent: $intent")
            runCatching { handleIntent(intent) }
                .onFailure { throwable ->
                    if (throwable is CancellationException) throw throwable
                    logger.log(tag, "Error handling intent: $intent", throwable)
                    handleError(throwable, intent)
                }
        }
    }

    // ── Protected API ─────────────────────────────────────────────────────────

    protected abstract suspend fun handleIntent(intent: Intent)

    /**
     * Reduce current [State] with [Partial] via [reduceWithPartial].
     * Logs state changes automatically.
     */
    protected fun sendPartial(partial: Partial) {
        reduceState { old ->
            logger.log(tag, "Partial: $partial")
            reduceWithPartial(partial, old)
        }
    }

    /**
     * Define how [Partial] transforms [State].
     */
    protected abstract fun reduceWithPartial(
        partial: Partial,
        old: State,
    ): State

    /**
     * Method to process initial load.
     */
    protected abstract suspend fun processInit()

    protected fun emitEffect(effect: Effect) {
        launch { _effect.emit(effect) }
    }

    protected fun launch(
        context: CoroutineContext = EmptyCoroutineContext,
        block: suspend CoroutineScope.() -> Unit,
    ) = scope.launch(handler + context, block = block)

    // ── Private ───────────────────────────────────────────────────────────────

    private fun reduceState(block: (State) -> State) {
        _state.update { old ->
            val new = block(old)
            if (old != new) logger.log(tag, "State: $old → $new")
            new
        }
    }

    /**
     * Method to encapsulate flow binding.
     */
    protected fun <T> Flow<T>.bind(action: suspend (value: T) -> Unit) =
        onEach(action)
            .catch { throwable ->
                if (throwable is CancellationException) throw throwable
                logError(throwable, "bind function")
                handleError(throwable)
            }.launchIn(scope)

    protected open suspend fun handleError(
        error: Throwable,
        intent: Intent? = null,
    ) {
        if (!_state.value.hasContent) return
        logger.log(tag, message = error.message.orEmpty(), error)
    }
}
