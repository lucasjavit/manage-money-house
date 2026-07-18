package com.managehouse.ingest.net

import com.squareup.moshi.Moshi
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

object ApiFactory {
    fun create(baseUrl: String): IngestApi {
        // Adapters gerados por codegen (KSP) a partir de @JsonClass(generateAdapter=true).
        val moshi = Moshi.Builder().build()
        return Retrofit.Builder()
            .baseUrl(if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/")
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(IngestApi::class.java)
    }
}
