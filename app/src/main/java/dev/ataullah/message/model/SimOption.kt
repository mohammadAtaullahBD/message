package dev.ataullah.message.model

data class SimOption(
    val subscriptionId: Int,
    val displayName: String,
    val carrierName: String?
) {
    val label: String
        get() = buildString {
            append(displayName.ifBlank { "SIM ${subscriptionId}" })
            carrierName?.takeIf { it.isNotBlank() && it != displayName }?.let {
                append(" (")
                append(it)
                append(')')
            }
        }
}
