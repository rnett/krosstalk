package com.rnett.krosstalk.error

public enum class KrosstalkResultStatus(public val statusCode: Int) {
    SUCCESS(200), USER_ERROR(400), KROSSTALK_ERROR(500);
}

public data class KrosstalkResult(val data: ByteArray, val status: KrosstalkResultStatus) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as KrosstalkResult

        if (!data.contentEquals(other.data)) return false
        if (status != other.status) return false

        return true
    }

    override fun hashCode(): Int {
        var result = data.contentHashCode()
        result = 31 * result + status.hashCode()
        return result
    }
}