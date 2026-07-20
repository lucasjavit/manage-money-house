package com.managehouse.ingest.net

import com.squareup.moshi.JsonClass
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

@JsonClass(generateAdapter = true)
data class IngestRequest(
    val userId: Long,
    val externalId: String,
    val rawText: String,
    val timestamp: Long,
    val packageName: String,
    val destination: String,
    val expenseTypeId: Long?,
    // Preenchidos no registro manual; quando amount vem, o backend não chama a IA.
    val amount: Double? = null,
    val description: String? = null
)

@JsonClass(generateAdapter = true)
data class IngestResponse(
    val status: String,
    val id: Long?,
    val expenseId: Long?,
    val amount: Double?,
    val suggestedExpenseTypeId: Long?,
    val needsReview: Boolean
)

@JsonClass(generateAdapter = true)
data class ExpenseType(
    val id: Long,
    val name: String
)

interface IngestApi {
    @POST("/api/ingest")
    suspend fun ingest(
        @Header("X-Ingest-Token") token: String,
        @Body body: IngestRequest
    ): Response<IngestResponse>

    @GET("/api/expense-types")
    suspend fun expenseTypes(): Response<List<ExpenseType>>
}
