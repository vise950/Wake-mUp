package nicola.dev.com.alarmap.utils

import kotlin.reflect.KClass

fun Any?.log(obj: Any? = null) {
    val logger = when(obj){
        is KClass<*> -> obj.java.simpleName
        is Class<*> -> obj.simpleName
        is String -> obj
        null -> "Default"
        else -> obj.javaClass.simpleName
    }

    val message = when(this){
        is String? -> if(this == null) "NullString" else this
        is Int? -> if(this == null) "NullInt" else "Int: $this"
        is Float? -> if(this == null) "NullFloat" else "Float: $this"
        is Double? -> if(this == null) "NullDouble" else "Double: $this"
        else -> if(this == null) "NullValue" else "Value: $this"
    }
    android.util.Log.e(logger, message)
}

/*

Test else let

a?.let{ ( object not null -> run my code }
    .other{ object null -> other code }

 */
inline fun <T, R> T.other(block: (T) -> R): R = block(this)
