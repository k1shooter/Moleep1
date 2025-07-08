package com.example.moleep1.ui.added.event

open class Event<out T>(private val content: T) {

    var hasBeenHandled = false
        private set // 외부에서는 값을 변경할 수 없도록 설정

    /**
     * 이미 처리된 이벤트라면 null을, 아니라면 content를 반환합니다.
     */
    fun getContentIfNotHandled(): T? {
        return if (hasBeenHandled) {
            null
        } else {
            hasBeenHandled = true
            content
        }
    }

    /**
     * 처리 여부와 관계없이 content를 반환합니다.
     */
    fun peekContent(): T = content
}