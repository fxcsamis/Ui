package com.arkitak.lite.wake

import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy

/**
 * AlwaysOnHotwordDetector is a hidden (@SystemApi) class: it is NOT in the public android.jar, so it cannot be named at
 * compile time (the build said "Unresolved reference 'AlwaysOnHotwordDetector'"). So this file never mentions it.
 * The callback is created at run time: if the platform's Callback is an interface we build a Proxy for it; if it is an
 * abstract class (as on some phones) a Proxy is impossible and [create] throws a clear message that the Wake screen shows.
 */
class DspCallback(private val owner: DspHotwordEngine) : InvocationHandler {

    companion object {
        private const val CALLBACK = "android.service.voice.AlwaysOnHotwordDetector\$Callback"

        /** Returns a callback object for the platform, or throws IllegalStateException with the reason. */
        fun create(owner: DspHotwordEngine): Any {
            val cls = Class.forName(CALLBACK)
            if (!cls.isInterface) {
                throw IllegalStateException("AlwaysOnHotwordDetector.Callback is an abstract class on this Android version and a hidden API: apps cannot subclass it")
            }
            return Proxy.newProxyInstance(cls.classLoader, arrayOf(cls), DspCallback(owner))
        }
    }

    override fun invoke(proxy: Any, method: Method, args: Array<out Any?>?): Any? {
        DiagLog.safe("dsp callback ${method.name}") {
            when (method.name) {
                "onAvailabilityChanged" -> owner.cbAvailability((args?.getOrNull(0) as? Int) ?: -1)
                "onDetected" -> owner.cbDetected()
                "onError" -> owner.cbError("onError")
                else -> owner.cbInfo(method.name)
            }
        }
        return when (method.name) {
            "hashCode" -> System.identityHashCode(proxy)
            "equals" -> proxy === args?.getOrNull(0)
            "toString" -> "DspCallback"
            else -> if (method.returnType == java.lang.Boolean.TYPE) false else null
        }
    }
}
