package io.github.limvot.chatatat

import org.matrix.androidsdk.rest.callback.ApiCallback;
import org.matrix.androidsdk.rest.model.MatrixError;

class CallbackWrapper<T>(val f: (T) -> Unit): ApiCallback<T> {
    override fun onSuccess(info: T) {
        f(info)
    }
    override fun onNetworkError(e: Exception) { }
    override fun onMatrixError(e: MatrixError) { }
    override fun onUnexpectedError(e: Exception) { }
}
