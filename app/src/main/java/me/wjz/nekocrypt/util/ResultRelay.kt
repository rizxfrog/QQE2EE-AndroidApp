package me.wjz.nekocrypt.util

import android.net.Uri
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object ResultRelay {
    private val _flow = MutableSharedFlow<Uri>()
    val flow = _flow.asSharedFlow()

    suspend fun send(uri: Uri) {
        _flow.emit(uri)
    }
}