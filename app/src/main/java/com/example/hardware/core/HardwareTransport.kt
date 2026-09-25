package com.example.hardware.core

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface HardwareTransport {
    val state: StateFlow<HardwareConnectionState>
    suspend fun connect(device: HardwareDevice): HardwareResult<Unit>
    fun disconnect()
    suspend fun write(bytes: ByteArray): HardwareResult<Unit>
    fun incomingBytes(): Flow<ByteArray>
}
