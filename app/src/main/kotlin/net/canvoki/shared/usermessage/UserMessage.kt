package net.canvoki.shared.usermessage

sealed interface UserMessage {
    val message: String

    data class Info(
        override val message: String,
    ) : UserMessage

    data class Suggestion(
        override val message: String,
        val actionLabel: String,
        val action: () -> Unit,
    ) : UserMessage

    fun post() {
        UserMessageBus.post(this)
    }
}
