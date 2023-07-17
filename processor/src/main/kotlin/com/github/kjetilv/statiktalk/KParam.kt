package com.github.kjetilv.statiktalk

@Suppress("unused")
data class KParam(val name: String, val type: String, val optional: Boolean = false) {

    val jsonType: String
        get() = when (type) {
            "String", "kotlin.String", "java.lang.String" -> "textValue"
            "long", "Long", "kotlin.Long", "java.lang.Long" -> "longValue"
            "int", "Int", "kotlin.Int", "Integer", "java.lang.Integer" -> "intValue"
            "double", "Double", "kotlin.Double", "java.lang.Double" -> "doubleValue"
            "float", "Float", "kotlin.Float", "java.lang.Float" -> "floatValue"
            "BigDecimal", "java.math.BigDecimal" -> "decimalValue"
            "BigInteger", "java.math.BigInteger" -> "bigIntegerValue"
            "boolean", "kotlin.Boolean", "Boolean", "java.lang.Boolean" -> "booleanValue"
            else ->
                throw IllegalStateException("Unsupported type: $type")
        }
}