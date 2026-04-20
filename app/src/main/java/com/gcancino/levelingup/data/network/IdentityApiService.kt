package com.gcancino.levelingup.data.network

import com.gcancino.levelingup.data.network.dto.WeeklyReportRequestDto
import com.gcancino.levelingup.data.network.dto.WeeklyReportResponseDto
import retrofit2.http.Body
import retrofit2.http.POST

interface IdentityApiService {
    @POST("identity/weekly-report")
    suspend fun generateWeeklyReport(
        @Body request: WeeklyReportRequestDto
    ): WeeklyReportResponseDto
}
