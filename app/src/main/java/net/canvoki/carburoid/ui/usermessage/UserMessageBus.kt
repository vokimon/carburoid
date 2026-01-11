package net.canvoki.carburoid.ui.usermessage

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object UserMessageBus {
    private val _messages = MutableSharedFlow<UserMessage>(extraBufferCapacity = 10)
    val messages: SharedFlow<UserMessage> = _messages.asSharedFlow()

    fun post(message: UserMessage) {
        _messages.tryEmit(message)
    }
}
