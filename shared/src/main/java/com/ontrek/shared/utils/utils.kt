package com.ontrek.shared.utils
/**
 * Formatta un messaggio di errore HTTP per la visualizzazione a schermo.
 *
 * @param httpCode Codice di errore HTTP
 * @param errorMessage Messaggio di errore dal backend (opzionale)
 * @return Stringa formattata contenente informazioni sull'errore
 */
fun formatErrorMessage(
    httpCode: Int,
    errorMessage: String? = null,
    customMessages: Map<Int, String>? = null
): String {
    val errorDescription = customMessages?.get(httpCode) ?: when (httpCode) {
        400 -> "Request not valid"
        401 -> "Not authorized"
        403 -> "Access denied"
        404 -> "Resource not found"
        408 -> "Timeout of the request"
        500 -> "Error on the server"
        502 -> "Gateway not available"
        503 -> "Service unavailable"
        504 -> "Timeout of the gateway"
        else -> "Unknown error"
    }

    return if (errorMessage.isNullOrBlank()) {
        "$httpCode: $errorDescription"
    } else {
        "$httpCode: $errorDescription - $errorMessage"
    }
}