package org.deafsapps.storeit.presentation.item.viewmodel

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.runningFold
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.deafsapps.storeit.base.fold
import org.deafsapps.storeit.domain.model.DomainError
import org.deafsapps.storeit.domain.model.Item
import org.deafsapps.storeit.domain.usecase.AddItemUseCaseType
import org.deafsapps.storeit.domain.usecase.DeleteItemUseCaseType
import org.deafsapps.storeit.domain.usecase.GetItemByIdUseCaseType
import org.deafsapps.storeit.presentation.StoreItViewModel
import org.deafsapps.storeit.presentation.item.model.ItemDetailUiEvent
import org.deafsapps.storeit.presentation.item.model.ItemDetailUiState
import org.koin.core.annotation.Factory
import org.koin.core.annotation.InjectedParam

private const val STOP_SHARE_LONG_TIMEOUT_MILLIS = 5_000L
private const val STOP_SHARE_SHORT_TIMEOUT_MILLIS = 500L
private const val STATE_CHANGE_BUFFER_CAPACITY = 64

@Factory
class ItemDetailViewModel(
    @InjectedParam private val itemId: String,
    coroutineScope: CoroutineScope?,
    private val getItemByIdUseCase: GetItemByIdUseCaseType,
    private val addItemUseCase: AddItemUseCaseType,
    private val deleteItemUseCase: DeleteItemUseCaseType,
) : StoreItViewModel(coroutineScope = coroutineScope) {

    private var latestState = ItemDetailUiState.getDefault()

    private val loadRequests = MutableSharedFlow<ItemDetailLoadRequest>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    private val stateChanges = MutableSharedFlow<ItemDetailStateChange>(
        extraBufferCapacity = STATE_CHANGE_BUFFER_CAPACITY,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<ItemDetailUiState> = loadRequests
        .onStart { emit(value = ItemDetailLoadRequest.RefreshItem) }
        .flatMapLatest { request -> request.toStateChanges() }
        .let { loadChanges -> listOf(loadChanges, stateChanges).merge() }
        .runningFold(
            initial = ItemDetailUiState.getDefault(),
            operation = { state, change ->
                change.reduce(state = state).also { latestState = it }
            },
        )
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = STOP_SHARE_LONG_TIMEOUT_MILLIS),
            initialValue = ItemDetailUiState.getDefault(),
        )

    private val _uiEvent = MutableSharedFlow<ItemDetailUiEvent?>()
    val uiEvent: SharedFlow<ItemDetailUiEvent?> = _uiEvent.asSharedFlow()
        .shareIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = STOP_SHARE_SHORT_TIMEOUT_MILLIS),
        )

    private var loadedItem: Item? = null

    fun onUpdateName(name: String) {
        enqueueStateChange(change = ItemDetailStateChange.NameUpdated(name = name))
    }

    fun onUpdateDescription(description: String) {
        enqueueStateChange(change = ItemDetailStateChange.DescriptionUpdated(description = description))
    }

    fun onUpdateQuantity(quantity: Int?) {
        enqueueStateChange(change = ItemDetailStateChange.QuantityUpdated(quantity = quantity))
    }

    fun onUpdateOwner(owner: String) {
        enqueueStateChange(change = ItemDetailStateChange.OwnerUpdated(owner = owner))
    }

    fun onUpdateTagInput(tagInput: String) {
        enqueueStateChange(change = ItemDetailStateChange.TagInputUpdated(tagInput = tagInput))
    }

    fun onAddTag() {
        val tag = latestState.tagInput.trim()
        if (tag.isNotEmpty()) {
            enqueueStateChange(change = ItemDetailStateChange.TagAdded(tag = tag))
        }
    }

    fun onRemoveTag(tag: String) {
        enqueueStateChange(change = ItemDetailStateChange.TagRemoved(tag = tag))
    }

    fun onUpdatePhotoUri(uri: String?) {
        enqueueStateChange(change = ItemDetailStateChange.PhotoUriUpdated(uri = uri))
    }

    fun onDeleteSelected() {
        enqueueStateChange(change = ItemDetailStateChange.DeleteConfirmVisibilityChanged(isVisible = true))
    }

    fun onDismissDeleteConfirm() {
        enqueueStateChange(change = ItemDetailStateChange.DeleteConfirmVisibilityChanged(isVisible = false))
    }

    fun onConfirmDelete() {
        val id = loadedItem?.id ?: itemId
        viewModelScope.launch {
            emitStateChange(change = ItemDetailStateChange.DeleteStarted)
            deleteItemUseCase(input = id).fold(
                ifErr = { error ->
                    emitStateChange(change = ItemDetailStateChange.SaveFailed(error = error))
                    _uiEvent.emit(ItemDetailUiEvent.ShowError(message = error.toErrorCause()))
                },
                ifOk = {
                    emitStateChange(change = ItemDetailStateChange.SaveFinished)
                    _uiEvent.emit(ItemDetailUiEvent.NavigateBack)
                },
            )
        }
    }

    fun onSave() {
        val source = loadedItem ?: return
        val state = latestState
        viewModelScope.launch {
            emitStateChange(change = ItemDetailStateChange.SaveStarted)
            val item = Item(
                id = source.id,
                rackId = source.rackId,
                slotId = source.slotId,
                name = state.name.trim().ifBlank { "Item" },
                description = state.description.trim(),
                photoUri = state.photoUri,
                quantity = state.quantity,
                owner = state.owner.trim(),
                tags = state.tags,
                createdAt = source.createdAt,
                updatedAt = source.updatedAt,
            )
            addItemUseCase(item).fold(
                ifErr = { error ->
                    emitStateChange(change = ItemDetailStateChange.SaveFailed(error = error))
                    _uiEvent.emit(ItemDetailUiEvent.ShowError(message = error.toErrorCause()))
                },
                ifOk = { saved ->
                    loadedItem = saved
                    emitStateChange(change = ItemDetailStateChange.SaveFinished)
                    _uiEvent.emit(ItemDetailUiEvent.NavigateBack)
                },
            )
        }
    }

    private fun enqueueStateChange(change: ItemDetailStateChange) {
        latestState = change.reduce(state = latestState)
        stateChanges.tryEmit(value = change)
    }

    private suspend fun emitStateChange(change: ItemDetailStateChange) {
        latestState = change.reduce(state = latestState)
        stateChanges.emit(value = change)
    }

    fun refresh() {
        loadRequests.tryEmit(value = ItemDetailLoadRequest.RefreshItem)
    }

    private fun ItemDetailLoadRequest.toStateChanges(): Flow<ItemDetailStateChange> = flow {
        emit(value = ItemDetailStateChange.Loading)
        when (this@toStateChanges) {
            ItemDetailLoadRequest.RefreshItem ->
                getItemByIdUseCase(input = itemId).fold(
                    ifErr = { error ->
                        emit(value = ItemDetailStateChange.LoadFailed(error = error))
                        _uiEvent.emit(ItemDetailUiEvent.ShowError(message = error.toErrorCause()))
                    },
                    ifOk = { item ->
                        loadedItem = item
                        emit(value = ItemDetailStateChange.ItemLoaded(item = item))
                    },
                )
        }
    }

    private enum class ItemDetailLoadRequest {
        RefreshItem,
    }

    private sealed interface ItemDetailStateChange {
        fun reduce(state: ItemDetailUiState): ItemDetailUiState

        data object Loading : ItemDetailStateChange {
            override fun reduce(state: ItemDetailUiState): ItemDetailUiState =
                state.copy(isLoading = true, error = null)
        }

        data class ItemLoaded(
            private val item: Item,
        ) : ItemDetailStateChange {
            override fun reduce(state: ItemDetailUiState): ItemDetailUiState =
                state.copy(
                    isLoading = false,
                    error = null,
                    name = item.name,
                    description = item.description,
                    quantity = item.quantity,
                    owner = item.owner,
                    tags = item.tags,
                    tagInput = "",
                    photoUri = item.photoUri,
                )
        }

        data class LoadFailed(
            private val error: DomainError,
        ) : ItemDetailStateChange {
            override fun reduce(state: ItemDetailUiState): ItemDetailUiState =
                state.copy(
                    isLoading = false,
                    error = error.toErrorCause(),
                )
        }

        data class NameUpdated(private val name: String) : ItemDetailStateChange {
            override fun reduce(state: ItemDetailUiState): ItemDetailUiState =
                state.copy(name = name, error = null)
        }

        data class DescriptionUpdated(private val description: String) : ItemDetailStateChange {
            override fun reduce(state: ItemDetailUiState): ItemDetailUiState =
                state.copy(description = description, error = null)
        }

        data class QuantityUpdated(private val quantity: Int?) : ItemDetailStateChange {
            override fun reduce(state: ItemDetailUiState): ItemDetailUiState =
                state.copy(quantity = quantity, error = null)
        }

        data class OwnerUpdated(private val owner: String) : ItemDetailStateChange {
            override fun reduce(state: ItemDetailUiState): ItemDetailUiState =
                state.copy(owner = owner, error = null)
        }

        data class TagInputUpdated(private val tagInput: String) : ItemDetailStateChange {
            override fun reduce(state: ItemDetailUiState): ItemDetailUiState =
                state.copy(tagInput = tagInput, error = null)
        }

        data class TagAdded(private val tag: String) : ItemDetailStateChange {
            override fun reduce(state: ItemDetailUiState): ItemDetailUiState =
                state.copy(tags = state.tags + tag, tagInput = "", error = null)
        }

        data class TagRemoved(private val tag: String) : ItemDetailStateChange {
            override fun reduce(state: ItemDetailUiState): ItemDetailUiState =
                state.copy(tags = state.tags - tag)
        }

        data class PhotoUriUpdated(private val uri: String?) : ItemDetailStateChange {
            override fun reduce(state: ItemDetailUiState): ItemDetailUiState =
                state.copy(photoUri = uri, error = null)
        }

        data class DeleteConfirmVisibilityChanged(
            private val isVisible: Boolean,
        ) : ItemDetailStateChange {
            override fun reduce(state: ItemDetailUiState): ItemDetailUiState =
                state.copy(showDeleteConfirm = isVisible)
        }

        data object SaveStarted : ItemDetailStateChange {
            override fun reduce(state: ItemDetailUiState): ItemDetailUiState =
                state.copy(isSaving = true, error = null)
        }

        data object DeleteStarted : ItemDetailStateChange {
            override fun reduce(state: ItemDetailUiState): ItemDetailUiState =
                state.copy(isSaving = true, error = null, showDeleteConfirm = false)
        }

        data class SaveFailed(
            private val error: DomainError,
        ) : ItemDetailStateChange {
            override fun reduce(state: ItemDetailUiState): ItemDetailUiState =
                state.copy(isSaving = false, error = error.toErrorCause())
        }

        data object SaveFinished : ItemDetailStateChange {
            override fun reduce(state: ItemDetailUiState): ItemDetailUiState =
                state.copy(isSaving = false)
        }
    }
}

private fun DomainError.toErrorCause(): String = when (this) {
    is DomainError.AuthenticationFailed,
    is DomainError.ServiceUnavailable,
    is DomainError.ConfigurationError -> message
    is DomainError.ValidationError -> reason
    is DomainError.NotFound -> "Item details not found"
    is DomainError.Unknown -> message
}
