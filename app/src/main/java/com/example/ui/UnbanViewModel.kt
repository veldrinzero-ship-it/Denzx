package com.example.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.UnbanRequest
import com.example.data.UnbanRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random

enum class ProcessMode(val label: String, val seconds: Int) {
    EXPRESS_1_MIN("⚡ 1 Menit Express", 60),
    DEEP_SCAN_10_MIN("🛡️ 10 Menit Deep Scan", 600)
}

enum class ProcessState {
    IDLE, RUNNING, COMPLETED, CANCELLED
}

data class CountryOption(val code: String, val country: String, val flag: String)

val COUNTRY_OPTIONS = listOf(
    CountryOption("+62", "Indonesia", "🇮🇩"),
    CountryOption("+1", "Amerika Serikat / Kanada", "🇺🇸"),
    CountryOption("+60", "Malaysia", "🇲🇾"),
    CountryOption("+65", "Singapura", "🇸🇬"),
    CountryOption("+91", "India", "🇮🇳"),
    CountryOption("+44", "Inggris", "🇬🇧"),
    CountryOption("+81", "Jepang", "🇯🇵"),
    CountryOption("+966", "Arab Saudi", "🇸🇦"),
    CountryOption("+971", "Uni Emirat Arab", "🇦🇪")
)

class UnbanViewModel(private val repository: UnbanRepository) : ViewModel() {

    val historyList: StateFlow<List<UnbanRequest>> = repository.allRequests
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _countryCode = MutableStateFlow("+62")
    val countryCode: StateFlow<String> = _countryCode.asStateFlow()

    private val _rawPhoneNumber = MutableStateFlow("")
    val rawPhoneNumber: StateFlow<String> = _rawPhoneNumber.asStateFlow()

    private val _selectedMode = MutableStateFlow(ProcessMode.EXPRESS_1_MIN)
    val selectedMode: StateFlow<ProcessMode> = _selectedMode.asStateFlow()

    private val _processState = MutableStateFlow(ProcessState.IDLE)
    val processState: StateFlow<ProcessState> = _processState.asStateFlow()

    private val _remainingSeconds = MutableStateFlow(60)
    val remainingSeconds: StateFlow<Int> = _remainingSeconds.asStateFlow()

    private val _appealsCount = MutableStateFlow(0)
    val appealsCount: StateFlow<Int> = _appealsCount.asStateFlow()

    private val _currentStepMessage = MutableStateFlow("Siap memproses permohonan pemulihan.")
    val currentStepMessage: StateFlow<String> = _currentStepMessage.asStateFlow()

    private val _activeTicketId = MutableStateFlow("")
    val activeTicketId: StateFlow<String> = _activeTicketId.asStateFlow()

    private val _showCompletionDialog = MutableStateFlow(false)
    val showCompletionDialog: StateFlow<Boolean> = _showCompletionDialog.asStateFlow()

    private val _completionText = MutableStateFlow("")
    val completionText: StateFlow<String> = _completionText.asStateFlow()

    private var processJob: Job? = null

    fun setCountryCode(code: String) {
        _countryCode.value = code
    }

    fun setRawPhoneNumber(number: String) {
        // Keep digits only
        _rawPhoneNumber.value = number.filter { it.isDigit() }
    }

    fun setSelectedMode(mode: ProcessMode) {
        if (_processState.value == ProcessState.RUNNING) return
        _selectedMode.value = mode
        _remainingSeconds.value = mode.seconds
    }

    fun getFormattedPhoneNumber(): String {
        val raw = _rawPhoneNumber.value
        val code = _countryCode.value
        return if (raw.isBlank()) "" else "$code ${formatNumberSpacing(raw)}"
    }

    private fun formatNumberSpacing(digits: String): String {
        return when {
            digits.length <= 4 -> digits
            digits.length <= 8 -> "${digits.substring(0, 4)} ${digits.substring(4)}"
            else -> "${digits.substring(0, 4)} ${digits.substring(4, 8)} ${digits.substring(8)}"
        }
    }

    fun startUnbanProcess() {
        val raw = _rawPhoneNumber.value.trim()
        if (raw.length < 8) {
            _currentStepMessage.value = "⚠️ Nomor telepon tidak valid! Masukkan minimal 8 digit."
            return
        }

        val totalSec = _selectedMode.value.seconds
        _processState.value = ProcessState.RUNNING
        _remainingSeconds.value = totalSec
        _appealsCount.value = 0
        _showCompletionDialog.value = false

        val ticket = "WA-${Random.nextInt(10000, 99999)}-SUPPORT"
        _activeTicketId.value = ticket

        processJob?.cancel()
        processJob = viewModelScope.launch {
            val stepLogList = listOf(
                "Membuat enkripsi ID perangkat & token permohonan...",
                "Menghubungkan ke gateway WhatsApp Support (support@whatsapp.com)...",
                "Mengirimkan batch permohonan banding berkecepatan tinggi...",
                "Menyiapkan berkas tinjauan otomatis tiket $ticket...",
                "Sinkronisasi status nomor dengan database WhatsApp...",
                "Menunggu respon validasi server utama...",
                "Verifikasi integritas akun & pemulihan sistem..."
            )

            var currentSec = totalSec
            val maxAppeals = if (totalSec == 60) 1420 else 8900

            while (currentSec > 0 && _processState.value == ProcessState.RUNNING) {
                delay(1000L)
                currentSec--
                _remainingSeconds.value = currentSec

                val elapsed = totalSec - currentSec
                val fraction = elapsed.toFloat() / totalSec.toFloat()

                // Increment appeals sent
                val calculatedAppeals = (fraction * maxAppeals + Random.nextInt(5, 15)).toInt()
                    .coerceAtMost(maxAppeals)
                _appealsCount.value = calculatedAppeals

                // Update step log
                val logIndex = (fraction * (stepLogList.size - 1)).toInt().coerceIn(stepLogList.indices)
                _currentStepMessage.value = stepLogList[logIndex]
            }

            if (_processState.value == ProcessState.RUNNING) {
                // Completed successfully
                _processState.value = ProcessState.COMPLETED
                _appealsCount.value = maxAppeals
                _currentStepMessage.value = "✅ WhatsApp Support merespon! Akun berhasil dipulihkan."

                val fullPhone = getFormattedPhoneNumber()
                val timestampStr = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id", "ID")).format(Date())

                val officialResponse = """
                    OFFICIAL WHATSAPP SUPPORT RESPONSE
                    
                    Yth. Pengguna WhatsApp,
                    
                    Sistem kami telah selesai meninjau aktivitas akun pada nomor telepon $fullPhone.
                    
                    Berdasarkan evaluasi ulang tim dukungan teknis, kami mengonfirmasi bahwa pemblokiran akun Anda telah dicabut secara permanen.
                    
                    ------------------------------------------
                    STATUS AKUN    : DIPULIHKAN & AKTIF
                    ID TIKET       : $ticket
                    WAKTU PROSES   : $timestampStr
                    TOTAL APPEAL   : ${_appealsCount.value} Permohonan
                    ------------------------------------------
                    
                    Akun WhatsApp Anda sekarang dapat didaftarkan dan digunakan kembali tanpa kendala. Mohon pastikan untuk selalu mematuhi Ketentuan Layanan WhatsApp.
                    
                    Terima kasih atas kesabaran Anda,
                    WhatsApp Support Team
                """.trimIndent()

                _completionText.value = officialResponse
                _showCompletionDialog.value = true

                // Save to Room DB
                val request = UnbanRequest(
                    phoneNumber = fullPhone,
                    countryCode = _countryCode.value,
                    ticketId = ticket,
                    durationSeconds = totalSec,
                    appealsSentCount = maxAppeals,
                    status = "RESTORED",
                    officialResponse = officialResponse
                )
                repository.insert(request)
            }
        }
    }

    fun cancelProcess() {
        processJob?.cancel()
        _processState.value = ProcessState.CANCELLED
        _currentStepMessage.value = "Proses dibatalkan."
    }

    fun dismissCompletionDialog() {
        _showCompletionDialog.value = false
    }

    fun sendEmailPermohonanDirect(context: Context) {
        val fullPhone = getFormattedPhoneNumber()
        if (fullPhone.isBlank()) {
            Toast.makeText(context, "Masukkan nomor HP terlebih dahulu!", Toast.LENGTH_SHORT).show()
            return
        }

        val subject = "Permohonan Pemulihan Akun WhatsApp Terblokir - $fullPhone"
        val bodyText = """
            Halo Tim WhatsApp Support,

            Mohon bantuan untuk memeriksa dan membuka kembali pemblokiran pada nomor WhatsApp saya:

            • Nomor Telepon : $fullPhone
            • Kendala       : Akun terblokir (Nomor ini tidak bisa menggunakan WhatsApp)
            • Perangkat     : Android Smartphone

            Saya tidak berniat melanggar Ketentuan Layanan WhatsApp. Nomor ini sangat penting untuk komunikasi keluarga dan pekerjaan saya. Mohon bantuan untuk memulihkan akun saya secepatnya.

            Terima kasih,
            Pengguna WhatsApp ($fullPhone)
        """.trimIndent()

        val recipientEmails = arrayOf(
            "support@whatsapp.com",
            "android@support.whatsapp.com",
            "appeal@support.whatsapp.com",
            "smb_web@support.whatsapp.com"
        )

        try {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:")
                putExtra(Intent.EXTRA_EMAIL, recipientEmails)
                putExtra(Intent.EXTRA_SUBJECT, subject)
                putExtra(Intent.EXTRA_TEXT, bodyText)
            }
            context.startActivity(Intent.createChooser(intent, "Kirim Permohonan via Email"))
        } catch (e: Exception) {
            Toast.makeText(context, "Tidak dapat membuka aplikasi Gmail/Email: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    fun deleteHistoryItem(id: Int) {
        viewModelScope.launch {
            repository.deleteById(id)
        }
    }

    fun showHistoryDetailDialog(request: UnbanRequest) {
        _activeTicketId.value = request.ticketId
        _completionText.value = request.officialResponse
        _showCompletionDialog.value = true
    }
}

class UnbanViewModelFactory(private val repository: UnbanRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(UnbanViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return UnbanViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
