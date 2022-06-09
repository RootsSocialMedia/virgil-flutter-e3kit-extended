package com.virgilsecurity.e3kit

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.virgilsecurity.android.common.callback.OnGetTokenCallback
import com.virgilsecurity.android.ethree.interaction.EThree
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.plugin.common.PluginRegistry.Registrar
import kotlinx.coroutines.*
import java.util.concurrent.Semaphore

class E3kitPlugin: MethodCallHandler {
    companion object {
        @JvmStatic
        fun registerWith(registrar: Registrar) {
            val channel = MethodChannel(registrar.messenger(), "plugins.virgilsecurity.com/e3kit")
            channel.setMethodCallHandler(
                    E3kitPlugin(registrar.activity(), registrar.context(), channel)
            )
        }
        private class MethodResultWrapper(result: Result) : Result {
            private val methodResult : Result = result
            private val handler : Handler = Handler(Looper.getMainLooper())

            override fun success(result: Any?) {
                handler.post {
                    run {
                        methodResult.success(result)
                    }
                }
            }
            override fun error(errorCode: String, errorMessage: String?, errorDetails: Any?) {
                handler.post {
                    run {
                        methodResult.error(errorCode, errorMessage, errorDetails)
                    }
                }
            }
            override fun notImplemented() {
                handler.post {
                    run {
                        methodResult.notImplemented()
                    }
                }
            }
        }
    }

    val activity: Activity?
    val context: Context
    val channel: MethodChannel
    private val eThrees = HashMap<String, FlutterEThree>()

    constructor(activity: Activity?, context: Context, channel: MethodChannel) {
        this.activity = activity
        this.context = context
        this.channel = channel
    }

    override fun onMethodCall(call: MethodCall, rawResult: Result) {
        // Solution for java.lang.IllegalStateException: Reply already submitted
        val result : Result = MethodResultWrapper(rawResult)
        val arguments = call.arguments as? HashMap<*, *>
        val instanceId = arguments!!["_id"] as String?
                ?: return result.error(
                        "argument_not_found",
                        "Could not find argument `_id` of type String",
                        null
                )


        if (call.method == "init") {

            val identity = arguments["identity"] as? String
                    ?: return result.error(
                            "argument_not_found",
                            "Could not find argument `identity` of type String",
                            null
                    )

            // solution for java.lang.IllegalStateException: Cannot access database on the main thread since it may potentially lock the UI for a long period of time.
            val parentJob = Job()
            val coroutineScope = CoroutineScope(Dispatchers.Main + parentJob)
            val deferredEThree = coroutineScope.async(Dispatchers.IO) {

                return@async EThree(identity, object : OnGetTokenCallback {
                    override fun onGetToken(): String {
                        var token: String? = null
                        var error: String? = null
                        var notImplemented: Boolean? = null

                        val semaphore = Semaphore(0)

                        activity?.runOnUiThread {
                            channel.invokeMethod("tokenCallback", hashMapOf("_id" to instanceId), object : Result {
                                override fun success(p0: Any?) {
                                    token = p0 as? String
                                    semaphore.release()
                                }

                                override fun error(p0: String, p1: String?, p2: Any?) {
                                    error = "$p0: $p1"
                                    semaphore.release()
                                }

                                override fun notImplemented() {
                                    notImplemented = true
                                    semaphore.release()
                                }
                            })
                        }

                        // Because E3Kit for Android doesn't support async onGetToken
                        // And Flutter's Platform Channels are asynchronous
                        semaphore.acquire()

                        return token ?: throw Error(error)
                    }
                },
                context,
                enableRatchet = true)
            }
            try{
                coroutineScope.launch(Dispatchers.Main){
                    val eThree = deferredEThree.await()
                    eThrees[instanceId] = FlutterEThree(eThree, channel, activity)
                    result.success(true)
                }
            }catch( throwable : Throwable) {
                parentJob.cancel()
                result.error(throwable.toFlutterError())
            }

        } else {
            val eThree = eThrees[instanceId]

            if(eThree !is FlutterEThree) {
                return result.error(
                        "not_initialized",
                        "EThree instance is not initialized",
                        null
                )
            }

            eThree.invoke(call, result)
        }
    }
}
