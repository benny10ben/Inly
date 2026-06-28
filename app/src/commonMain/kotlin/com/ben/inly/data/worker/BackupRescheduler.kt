package com.ben.inly.data.worker

interface BackupRescheduler {
    fun rescheduleNow(frequency: String, time: String, day: String)
    fun cancel()
}