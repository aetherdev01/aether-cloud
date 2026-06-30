package com.aether.cloud.ui.auth

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.aether.cloud.MainActivity
import com.aether.cloud.data.repository.AuthRepository
import com.aether.cloud.databinding.ActivityPhoneLoginBinding
import com.aether.cloud.util.Resource
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthProvider
import kotlinx.coroutines.launch
import androidx.lifecycle.lifecycleScope

class PhoneLoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPhoneLoginBinding
    private lateinit var authRepository: AuthRepository

    private var fullPhoneNumber: String = ""
    private var resendToken: PhoneAuthProvider.ForceResendingToken? = null
    private var resendTimer: CountDownTimer? = null

    private val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

        // Called when Android auto-detects the SMS (no manual code entry needed)
        override fun onVerificationCompleted(credential: PhoneAuthCredential) {
            setLoading(false)
            signInWithCredential(credential)
        }

        override fun onVerificationFailed(e: FirebaseException) {
            setLoading(false)
            val message = when (e) {
                is FirebaseTooManyRequestsException -> "Terlalu banyak percobaan. Coba lagi nanti."
                else -> e.message ?: "Verifikasi nomor gagal"
            }
            Toast.makeText(this@PhoneLoginActivity, message, Toast.LENGTH_LONG).show()
        }

        override fun onCodeSent(
            verificationId: String,
            token: PhoneAuthProvider.ForceResendingToken
        ) {
            setLoading(false)
            resendToken = token
            this@PhoneLoginActivity.verificationId = verificationId
            showOtpStep()
            startResendTimer()
            Toast.makeText(this@PhoneLoginActivity, "Kode OTP terkirim", Toast.LENGTH_SHORT).show()
        }
    }

    private var verificationId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPhoneLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        authRepository = AuthRepository(this)

        binding.toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        binding.btnSendOtp.setOnClickListener { onSendOtpClicked() }
        binding.btnVerifyOtp.setOnClickListener { onVerifyOtpClicked() }
        binding.btnResendOtp.setOnClickListener { onResendClicked() }
        binding.btnChangeNumber.setOnClickListener { showPhoneStep() }
    }

    private fun onSendOtpClicked() {
        val countryCode = binding.etCountryCode.text.toString().trim()
        val localNumber = binding.etPhoneNumber.text.toString().trim()

        if (countryCode.isEmpty() || !countryCode.startsWith("+")) {
            binding.etCountryCode.error = "e.g. +62"
            return
        }
        if (localNumber.isEmpty() || localNumber.length < 8) {
            binding.etPhoneNumber.error = "Enter a valid phone number"
            return
        }

        // Strip leading zero, e.g. user types 081234... with +62 selected
        val normalizedLocal = localNumber.trimStart('0')
        fullPhoneNumber = countryCode + normalizedLocal

        setLoading(true)
        authRepository.sendOtp(
            phoneNumber = fullPhoneNumber,
            activity = this,
            resendToken = null,
            callbacks = callbacks
        )
    }

    private fun onResendClicked() {
        if (fullPhoneNumber.isEmpty()) return
        setLoading(true)
        authRepository.sendOtp(
            phoneNumber = fullPhoneNumber,
            activity = this,
            resendToken = resendToken,
            callbacks = callbacks
        )
    }

    private fun onVerifyOtpClicked() {
        val code = binding.etOtpCode.text.toString().trim()
        val vid = verificationId

        if (code.length < 6) {
            binding.etOtpCode.error = "Enter the 6-digit code"
            return
        }
        if (vid == null) {
            Toast.makeText(this, "Verification session expired, please resend code", Toast.LENGTH_SHORT).show()
            return
        }

        setLoading(true)
        val credential = PhoneAuthProvider.getCredential(vid, code)
        signInWithCredential(credential)
    }

    private fun signInWithCredential(credential: PhoneAuthCredential) {
        setLoading(true)
        lifecycleScope.launch {
            when (val result = authRepository.signInWithPhoneCredential(credential)) {
                is Resource.Success -> {
                    checkProfileAndProceed()
                }
                is Resource.Error -> {
                    setLoading(false)
                    Toast.makeText(
                        this@PhoneLoginActivity,
                        result.message ?: "Kode salah atau sudah kedaluwarsa",
                        Toast.LENGTH_LONG
                    ).show()
                }
                else -> {}
            }
        }
    }

    private fun checkProfileAndProceed() {
        lifecycleScope.launch {
            val currentUser = authRepository.getCurrentUser()
            if (currentUser == null) {
                setLoading(false)
                return@launch
            }
            when (val result = authRepository.checkUserProfile(currentUser.uid)) {
                is Resource.Success -> {
                    setLoading(false)
                    val target = if (result.data == true) MainActivity::class.java else ProfileSetupActivity::class.java
                    startActivity(Intent(this@PhoneLoginActivity, target))
                    finish()
                }
                is Resource.Error -> {
                    setLoading(false)
                    Toast.makeText(
                        this@PhoneLoginActivity,
                        "Failed to check profile: ${result.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
                else -> {}
            }
        }
    }

    private fun showOtpStep() {
        binding.stepPhone.visibility = View.GONE
        binding.stepOtp.visibility = View.VISIBLE
        binding.tvPhoneDisplay.text = fullPhoneNumber
        binding.etOtpCode.requestFocus()
    }

    private fun showPhoneStep() {
        resendTimer?.cancel()
        binding.stepOtp.visibility = View.GONE
        binding.stepPhone.visibility = View.VISIBLE
        binding.etOtpCode.text?.clear()
    }

    private fun startResendTimer() {
        resendTimer?.cancel()
        binding.btnResendOtp.isEnabled = false
        resendTimer = object : CountDownTimer(30_000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                binding.btnResendOtp.text = "Resend code (${millisUntilFinished / 1000}s)"
            }

            override fun onFinish() {
                binding.btnResendOtp.isEnabled = true
                binding.btnResendOtp.text = getString(com.aether.cloud.R.string.resend_code)
            }
        }.start()
    }

    private fun setLoading(loading: Boolean) {
        binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        binding.btnSendOtp.isEnabled = !loading
        binding.btnVerifyOtp.isEnabled = !loading
    }

    override fun onDestroy() {
        super.onDestroy()
        resendTimer?.cancel()
    }
}
