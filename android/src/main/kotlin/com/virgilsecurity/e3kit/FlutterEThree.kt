package com.virgilsecurity.e3kit

import android.app.Activity
import android.os.AsyncTask
import android.os.Looper
import android.util.Log
import com.virgilsecurity.android.common.exception.EThreeException
import com.virgilsecurity.android.common.model.ratchet.RatchetChannel
import com.virgilsecurity.android.common.model.FindUsersResult
import com.virgilsecurity.android.ethree.interaction.EThree
import com.virgilsecurity.common.callback.OnCompleteListener
import com.virgilsecurity.common.callback.OnResultListener
import com.virgilsecurity.sdk.cards.Card
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel

class FlutterEThree {
    val instance: EThree
    val channel: MethodChannel
    val activity: Activity?
    val ratchetChannels = HashMap<String, RatchetChannel>()

    constructor(instance: EThree, channel: MethodChannel, activity: Activity?) {
        this.instance = instance
        this.channel = channel
        this.activity = activity
    }

    fun invoke(call: MethodCall, result: MethodChannel.Result) {
        fun <T: Any>getArgument(argument: String, optional: Boolean = false): T {
            val arg = (call.arguments as? HashMap<String, Any>)?.getValue(argument) as? T

            if(!optional && arg == null) {
                val errorCode = "argument_not_found"
                val errorMessage = "Could not find argument `$argument`"
                result.error(errorCode, errorMessage, null)
                throw Error("$errorCode: $errorMessage")
            }

            return arg!!
        }

        fun <T: Any>getOptionalArgument(argument: String): T? {
            return try {
                getArgument(argument, true)
            } catch(e: Throwable) {
                null
            }
        }


        try {
            when (call.method) {
                "getIdentity" -> getIdentity(result)
                "hasLocalPrivateKey" -> hasLocalPrivateKey(result)
                "register" -> register(result)
                "rotatePrivateKey" -> rotatePrivateKey(result)
                "cleanUp" -> cleanUp(result)
                "findUsers" -> findUsers(
                        getArgument("identities"),
                        result
                )
                "encrypt" -> encrypt(
                        getArgument("text"),
                        getOptionalArgument("users"),
                        result
                )
                "decrypt" -> decrypt(
                        getArgument("text"),
                        getOptionalArgument("user"),
                        result
                )
                "backupPrivateKey" -> backupPrivateKey(
                    getArgument("password"),
                    result
                )
                "resetPrivateKeyBackup" -> resetPrivateKeyBackup(result)
                "changePassword" -> changePassword(
                    getArgument("oldPassword"),
                    getArgument("newPassword"),
                    result
                )
                "restorePrivateKey" -> restorePrivateKey(
                        getArgument("password"),
                        result
                )
                "unregister" -> unregister(result)
                "createRatchetChannel" -> createRatchetChannel(
                    getArgument("identity"),
                    result
                )
                "joinRatchetChannel" -> joinRatchetChannel(
                    getArgument("identity"),
                    result
                )
                "hasRatchetChannel" -> hasRatchetChannel(
                    getArgument("identity"),
                    result
                )
                "getRatchetChannel" -> getRatchetChannel(
                    getArgument("identity"),
                    result
                )
                "deleteRatchetChannel" -> deleteRatchetChannel(
                    getArgument("identity"),
                    result
                )
                "ratchetEncrypt" -> ratchetEncrypt(
                    getArgument("identity"),
                    getArgument("message"),
                    result
                )
                "ratchetDecrypt" -> ratchetDecrypt(
                    getArgument("identity"),
                    getArgument("message"),
                    result
                )
                "ratchetDecryptMultiple" -> ratchetDecryptMultiple(
                    getArgument("identity"),
                    getArgument("messages"),
                    result
                )
                else -> activity?.runOnUiThread {
                    result.error(
                            "method_not_recognized",
                            "Method is not recognized",
                            "Method name: '${call.method}'"
                    )
                }
            }
        } catch(e: Throwable) {
            result.error(e.toFlutterError())
        }
    }

    private fun getIdentity(result: MethodChannel.Result) {
        activity?.runOnUiThread {
            result.success(instance.identity)
        }
    }

    private fun hasLocalPrivateKey(result: MethodChannel.Result) {
        activity?.runOnUiThread {
            result.success(instance.hasLocalPrivateKey())
        }
    }

    private fun register(result: MethodChannel.Result) {
        instance.register().addCallback(object: OnCompleteListener {
            override fun onSuccess() {
                activity?.runOnUiThread {
                    result.success(true)
                }
            }
            override fun onError(throwable: Throwable) {
                activity?.runOnUiThread {
                    result.error(throwable.toFlutterError())
                }
            }
        })
    }

    private fun rotatePrivateKey(result: MethodChannel.Result) {
        instance.rotatePrivateKey().addCallback(object: OnCompleteListener {
            override fun onSuccess() {
                activity?.runOnUiThread {
                    result.success(true)
                }
            }
            override fun onError(throwable: Throwable) {
                activity?.runOnUiThread {
                    result.error(throwable.toFlutterError())
                }
            }
        })
    }

    private fun cleanUp(result: MethodChannel.Result) {
        AsyncTask.execute {
            try {
                instance.cleanup()
            } catch(e: Throwable) {
                activity?.runOnUiThread {
                    result.error(e.toFlutterError())
                }

                return@execute
            }

            activity?.runOnUiThread {
                result.success(true)
            }
        }
    }

    private fun findUsers(identities: List<String>, result: MethodChannel.Result) {
        instance.findUsers(identities, true).addCallback(object: OnResultListener<FindUsersResult> {
            override fun onSuccess(res: FindUsersResult) {
                val mapped = res.mapValues {
                    it.value.rawCard.exportAsBase64String()!!
                }

                activity?.runOnUiThread {
                    result.success(mapped)
                }
            }
            override fun onError(throwable: Throwable) {
                activity?.runOnUiThread {
                    result.error(throwable.toFlutterError())
                }
            }
        })
    }

    private fun encrypt(
            text: String,
            users: HashMap<String, String>? = null,
            result: MethodChannel.Result
    ) {
        val mapped = users?.mapValues {
            instance.cardManager.importCardAsString(it.value)!!

        }

        val findUsersResult = if (mapped != null) FindUsersResult(mapped) else null

        val res = instance.encrypt(text, findUsersResult)

        result.success(res)
    }

    private fun decrypt(
            text: String,
            user: String? = null,
            result: MethodChannel.Result
    ) {
        val imported = if (user != null) instance.cardManager.importCardAsString(user) else null
        val res = instance.authDecrypt(text, imported)

        result.success(res)
    }

    private fun backupPrivateKey(password: String, result: MethodChannel.Result) {
        instance.backupPrivateKey(password).addCallback(object: OnCompleteListener {
            override fun onSuccess() {
                activity?.runOnUiThread {
                    result.success(true)
                }
            }
            override fun onError(throwable: Throwable) {
                activity?.runOnUiThread {
                    result.error(throwable.toFlutterError())
                }
            }
        })
    }

    private fun resetPrivateKeyBackup(result: MethodChannel.Result) {
        instance.resetPrivateKeyBackup().addCallback(object: OnCompleteListener {
            override fun onSuccess() {
                activity?.runOnUiThread {
                    result.success(true)
                }
            }
            override fun onError(throwable: Throwable) {
                activity?.runOnUiThread {
                    result.error(throwable.toFlutterError())
                }
            }
        })
    }

    private fun changePassword(oldPassword: String, newPassword: String, result: MethodChannel.Result) {
        instance.changePassword(oldPassword, newPassword).addCallback(object: OnCompleteListener {
            override fun onSuccess() {
                activity?.runOnUiThread {
                    result.success(true)
                }
            }
            override fun onError(throwable: Throwable) {
                activity?.runOnUiThread {
                    result.error(throwable.toFlutterError())
                }
            }
        })
    }

    private fun restorePrivateKey(password: String, result: MethodChannel.Result) {
        instance.restorePrivateKey(password).addCallback(object: OnCompleteListener {
            override fun onSuccess() {
                activity?.runOnUiThread {
                    result.success(true)
                }
            }
            override fun onError(throwable: Throwable) {
                activity?.runOnUiThread {
                    result.error(throwable.toFlutterError())
                }
            }
        })
    }

    private fun unregister(result: MethodChannel.Result) {
        instance.unregister().addCallback(object: OnCompleteListener {
            override fun onSuccess() {
                activity?.runOnUiThread {
                    result.success(true)
                }
            }
            override fun onError(throwable: Throwable) {
                activity?.runOnUiThread {
                    result.error(throwable.toFlutterError())
                }
            }
        })
    }

    private fun createRatchetChannel(identity: String, result: MethodChannel.Result) {

        instance.findUsers(listOf(identity), true).addCallback(object: OnResultListener<FindUsersResult> {
            override fun onSuccess(res: FindUsersResult) {

                val card : Card? = res.get(identity)

                instance.createRatchetChannel(card!!).addCallback(object: OnResultListener<RatchetChannel> {
                    override fun onSuccess(ratchetRes: RatchetChannel) {
                        activity?.runOnUiThread {
//                            ratchetChannels[identity] = ratchetRes
                            result.success(true)
                        }
                    }
                    override fun onError(throwable: Throwable) {
                        activity?.runOnUiThread {
                            result.error(throwable.toFlutterError())
                        }
                    }
                })
            }
            override fun onError(throwable: Throwable) {
                activity?.runOnUiThread {
                    result.error(throwable.toFlutterError())
                }
            }
        })

    }

    private fun joinRatchetChannel(identity: String, result: MethodChannel.Result) {

        instance.findUsers(listOf(identity), true).addCallback(object: OnResultListener<FindUsersResult> {
            override fun onSuccess(res: FindUsersResult) {
                val card : Card? = res.get(identity)

                instance.joinRatchetChannel(card!!).addCallback(object: OnResultListener<RatchetChannel> {
                    override fun onSuccess(ratchetRes: RatchetChannel) {
                        activity?.runOnUiThread {
//                            ratchetChannels.put(key = identity, value = ratchetRes)
                            result.success(true)
                        }
                    }
                    override fun onError(throwable: Throwable) {
                        activity?.runOnUiThread {
                            result.error(throwable.toFlutterError())
                        }
                    }
                })
            }
            override fun onError(throwable: Throwable) {
                activity?.runOnUiThread {
                    result.error(throwable.toFlutterError())
                }
            }
        })

    }

    private fun hasRatchetChannel(identity: String, result: MethodChannel.Result) {
        try{
            val ratchet : RatchetChannel? = ratchetChannels[identity]
            if(ratchet != null){
                result.success(true)
            }else {
                result.success(false)
            }
        }catch(throwable: Throwable) {
            result.error(throwable.toFlutterError())
        }
    }

    private fun getRatchetChannel(identity: String, result: MethodChannel.Result) {
        instance.findUsers(listOf(identity), true).addCallback(object: OnResultListener<FindUsersResult> {
            override fun onSuccess(res: FindUsersResult) {

                try{
                    val card : Card? = res.get(identity)

                    val ratchetChannel: RatchetChannel? = instance.getRatchetChannel(card!!)
                    if(ratchetChannel != null){
//                        ratchetChannels.put(key = identity, value = ratchetChannel!!)
                        result.success(true)
                    }else{
                        result.success(false)
                    }
                }catch (e: Throwable){
                    result.error(e.toFlutterError())
                }

            }
            override fun onError(throwable: Throwable) {
                activity?.runOnUiThread {
                    result.error(throwable.toFlutterError())
                }
            }
        })
    }

    private fun deleteRatchetChannel(identity: String, result: MethodChannel.Result) {
        instance.findUsers(listOf(identity), true).addCallback(object: OnResultListener<FindUsersResult> {
            override fun onSuccess(res: FindUsersResult) {
                val card : Card? = res.get(identity)

                instance.deleteRatchetChannel(card!!).addCallback(object: OnCompleteListener {
                    override fun onSuccess() {
//                        ratchetChannels.remove(identity)
                        activity?.runOnUiThread {
                            result.success(true)
                        }
                    }
                    override fun onError(throwable: Throwable) {
                        activity?.runOnUiThread {
                            result.error(throwable.toFlutterError())
                        }
                    }
                })
            }
            override fun onError(throwable: Throwable) {
                activity?.runOnUiThread {
                    result.error(throwable.toFlutterError())
                }
            }
        })
    }

    // the app will crash if not yet get the ratchetChannel!
    private fun ratchetEncrypt(identity: String, message: String, result: MethodChannel.Result) {
        try{
            instance.findUsers(listOf(identity), true).addCallback(object: OnResultListener<FindUsersResult> {
                override fun onSuccess(res: FindUsersResult) {
                    try{
                        val card : Card? = res.get(identity)
                        val ratchetChannel: RatchetChannel? = instance.getRatchetChannel(card!!)
                        val encryptedMessage : String = ratchetChannel!!.encrypt(message)
                        activity?.runOnUiThread {
                            result.success(encryptedMessage)
                        }
                    }catch (e: Throwable){
                        activity?.runOnUiThread {
                            result.error(e.toFlutterError())
                        }
                    }
                }
                override fun onError(throwable: Throwable) {
                    activity?.runOnUiThread {
                        result.error(throwable.toFlutterError())
                    }
                }
            })
        }catch(throwable: Throwable){
            result.error(throwable.toFlutterError())
        }
    }

    // the app will crash if not yet get the ratchetChannel!
    private fun ratchetDecrypt(identity: String, message: String, result: MethodChannel.Result) {
        try{
            instance.findUsers(listOf(identity), true).addCallback(object: OnResultListener<FindUsersResult> {
                override fun onSuccess(res: FindUsersResult) {
                    try{
                        val card : Card? = res.get(identity)
                        val ratchetChannel: RatchetChannel? = instance.getRatchetChannel(card!!)
                        val decryptedMessage : String = ratchetChannel!!.decrypt(message)
                        activity?.runOnUiThread {
                            result.success(decryptedMessage)
                        }
                    }catch (e: Throwable){
                        activity?.runOnUiThread {
                            result.error(e.toFlutterError())
                        }
                    }
                }
                override fun onError(throwable: Throwable) {
                    activity?.runOnUiThread {
                        result.error(throwable.toFlutterError())
                    }
                }
            })
        }catch(throwable: Throwable){
            result.error(throwable.toFlutterError())
        }
    }

    private fun ratchetDecryptMultiple(identity: String, messages: List<String>, result: MethodChannel.Result) {
        try{
            val ratchetChannel : RatchetChannel? = ratchetChannels[identity]
            val encryptedMessages: RatchetChannel.MultipleString = RatchetChannel.MultipleString(messages)
            val decryptedMessages: RatchetChannel.MultipleString = ratchetChannel!!.decryptMultiple(encryptedMessages)
            val decryptedStrings : List<String> = decryptedMessages.multipleText
            result.success(decryptedStrings)
        }catch(throwable: Throwable){
            result.error(throwable.toFlutterError())
        }
    }

}
