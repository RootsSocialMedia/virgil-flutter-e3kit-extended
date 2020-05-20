package com.virgilsecurity.e3kit

import com.virgilsecurity.android.common.exception.EThreeException
import com.virgilsecurity.android.common.exception.EThreeRatchetException
import com.virgilsecurity.crypto.ratchet.RatchetException
// import com.virgilsecurity.android.common.exception.PrivateKeyNotFoundException
import io.flutter.plugin.common.MethodChannel

typealias FlutterError = Triple<String?, String?, Any?>

fun MethodChannel.Result.error(error: FlutterError) {
    this.error(error.first, error.second, error.third)
}

fun Throwable.defaultFlutterError(): FlutterError {
    return FlutterError(
            "unknown_error",
            message,
            null
    )
}

fun Throwable.toFlutterError(): FlutterError {
    if(this is EThreeException) {
        return this.toFlutterError()
    }

     if(this is EThreeRatchetException) {
         return this.toFlutterError()
     }

    return this.defaultFlutterError()
}

fun EThreeException.toFlutterError(): FlutterError {
    if(this.message == "User is already registered") {
        return FlutterError(
                "user_is_already_registered",
                message,
                null
        )
    }

    if(this.message == "Private key already exists in local key storage.") {
        return FlutterError(
                "private_key_exists",
                message,
                null
        )
    }

    return this.defaultFlutterError()
}

fun EThreeRatchetException.toFlutterError(): FlutterError {
    if(this.message == "70204: Trying to encrypt empty array.") {
        return FlutterError(
                "trying_to_encrypt_empty_array",
                message,
                null
        )
    }
    if(this.message == "70204: Trying to decrypt empty array.") {
        return FlutterError(
                "trying_to_decrypt_empty_array",
                message,
                null
        )
    }
    if(this.message == "70204: Channel with provided user and name already exists.") {
        return FlutterError(
                "channel_already_exists",
                message,
                null
        )
    }
    if(this.message == "70204: Channel with self is forbidden. Use regular encryption for this purpose.") {
        return FlutterError(
                "channel_with_self_is_forbidden",
                message,
                null
        )
    }
    if(this.message == "70204: enableRatchet parameter is set to false.") {
        return FlutterError(
                "ratchet_not_enabled",
                message,
                null
        )
    }
    if(this.message == "70204: Provided user has been never initialized with ratchet enabled.") {
        return FlutterError(
                "ratchet_not_enabled_for_provided_user",
                message,
                null
        )
    }
    if(this.message == "70204: There is no invitation from provided user.") {
        return FlutterError(
                "no_invitation_from_provided_user",
                message,
                null
        )
    }
    if(this.message == "70204: There is no self card in local storage.") {
        return FlutterError(
                "no_self_card_in_local_storage",
                message,
                null
        )
    }
    return this.defaultFlutterError()
}