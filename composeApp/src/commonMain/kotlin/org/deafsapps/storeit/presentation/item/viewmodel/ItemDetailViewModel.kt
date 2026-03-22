package org.deafsapps.storeit.presentation.item.viewmodel

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.update
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

private const val STOP_SHARE_SHORT_TIMEOUT_MILLIS = 500L

@Factory
class ItemDetailViewModel(
    @InjectedParam private val itemId: String,
    coroutineScope: CoroutineScope?,
    private val getItemByIdUseCase: GetItemByIdUseCaseType,
    private val addItemUseCase: AddItemUseCaseType,
    private val deleteItemUseCase: DeleteItemUseCaseType,
) : StoreItViewModel(coroutineScope = coroutineScope) {

    private val _uiState = MutableStateFlow(ItemDetailUiState.getDefault())
    val uiState: StateFlow<ItemDetailUiState> = _uiState.asStateFlow()

    private val _uiEvent = MutableSharedFlow<ItemDetailUiEvent?>()
    val uiEvent: SharedFlow<ItemDetailUiEvent?> = _uiEvent.asSharedFlow()
        .shareIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = STOP_SHARE_SHORT_TIMEOUT_MILLIS),
        )

    private var loadedItem: Item? = null

    init {
        loadItem()
    }

    fun onUpdateName(name: String) {
        _uiState.update { state -> state.copy(name = name, error = null) }
    }

    fun onUpdateDescription(description: String) {
        _uiState.update { state -> state.copy(description = description, error = null) }
    }

    fun onUpdateQuantity(quantity: Int?) {
        _uiState.update { state -> state.copy(quantity = quantity, error = null) }
    }

    fun onUpdateOwner(owner: String) {
        _uiState.update { state -> state.copy(owner = owner, error = null) }
    }

    fun onUpdateTagInput(tagInput: String) {
        _uiState.update { state -> state.copy(tagInput = tagInput, error = null) }
    }

    fun onAddTag() {
        val tag = _uiState.value.tagInput.trim()
        if (tag.isNotEmpty()) {
            _uiState.update { state ->
                state.copy(tags = state.tags + tag, tagInput = "", error = null)
            }
        }
    }

    fun onRemoveTag(tag: String) {
        _uiState.update { state -> state.copy(tags = state.tags - tag) }
    }

    fun onUpdatePhotoUri(uri: String?) {
        _uiState.update { state -> state.copy(photoUri = uri, error = null) }
    }

    fun onDeleteClick() {
        _uiState.update { state -> state.copy(showDeleteConfirm = true) }
    }

    fun onDismissDeleteConfirm() {
        _uiState.update { state -> state.copy(showDeleteConfirm = false) }
    }

    fun onConfirmDelete() {
        val id = loadedItem?.id ?: itemId
        viewModelScope.launch {
            _uiState.update { state -> state.copy(isSaving = true, error = null, showDeleteConfirm = false) }
            deleteItemUseCase(input = id).fold(
                ifErr = { error ->
                    _uiState.update { state ->
                        state.copy(isSaving = false, error = error.toErrorCause())
                    }
                    _uiEvent.emit(ItemDetailUiEvent.ShowError(message = error.toErrorCause()))
                },
                ifOk = {
                    _uiState.update { state -> state.copy(isSaving = false) }
                    _uiEvent.emit(ItemDetailUiEvent.NavigateBack)
                },
            )
        }
    }

    fun onSave() {
        val source = loadedItem ?: return
        val state = _uiState.value
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }
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
                    _uiState.update { s ->
                        s.copy(isSaving = false, error = error.toErrorCause())
                    }
                    _uiEvent.emit(ItemDetailUiEvent.ShowError(message = error.toErrorCause()))
                },
                ifOk = { saved ->
                    loadedItem = saved
                    _uiState.update { s ->
                        s.copy(isSaving = false)
                    }
                    _uiEvent.emit(ItemDetailUiEvent.NavigateBack)
                },
            )
        }
    }

    private fun loadItem() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            getItemByIdUseCase(input = itemId).fold(
                ifErr = { error ->
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            error = error.toErrorCause(),
                        )
                    }
                    _uiEvent.emit(ItemDetailUiEvent.ShowError(message = error.toErrorCause()))
                },
                ifOk = { item ->
                    loadedItem = item
                    _uiState.update { state ->
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
                },
            )
        }
    }
}

private fun DomainError.toErrorCause(): String = when (this) {
    is DomainError.ValidationError -> reason
    is DomainError.NotFound -> "Item details not found"
    is DomainError.Unknown -> "An unknown error occurred"
}
