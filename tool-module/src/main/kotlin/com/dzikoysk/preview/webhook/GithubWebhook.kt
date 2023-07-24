package com.dzikoysk.preview.webhook

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class GithubWebhook(
    // Common properties
    val action: String? = null,
    val issue: Issue? = null,
    val sender: Sender? = null,
    val repository: Repository? = null,
    val organization: Organization? = null,
    val installation: Installation? = null,
    // Webhook specific properties
    val ref: String? = null,
    val ref_type: String? = null,
    val after: String? = null,
    val base_ref: String? = null,
    val before: String? = null,
    val created: Boolean? = null,
    val deleted: Boolean? = null,
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class Issue(
        val url: String,
        val number: Int
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class Sender(
        val login: String,
        val id: Int
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class Repository(
        val id: Int,
        val full_name: String,
        val owner: Owner,
    ) {

        @JsonIgnoreProperties(ignoreUnknown = true)
        data class Owner(
            val login: String,
            val id: Int
        )

    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class Organization(
        val login: String,
        val id: Int
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class Installation(
        val id: Int
    )

}