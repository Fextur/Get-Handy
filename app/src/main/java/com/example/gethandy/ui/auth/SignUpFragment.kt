package com.example.gethandy.ui

import android.os.Bundle
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.gethandy.R
import com.example.gethandy.databinding.FragmentSignUpBinding
import com.example.gethandy.utils.SnackbarType
import com.example.gethandy.utils.UserManager
import com.example.gethandy.utils.showSnackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SignUpFragment : Fragment() {

    private var _binding: FragmentSignUpBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth
    private val db = FirebaseFirestore.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSignUpBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()

        binding.btnSignup.setOnClickListener {
            signUpUser()
        }
    }

    private fun signUpUser() {
        val fullName = binding.etName.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val phone = binding.etPhone.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()

        if (!validateInput(fullName, email, phone, password)) return

        binding.btnSignup.isEnabled = false
        binding.btnSignup.text = getString(R.string.signing_up)

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val userId = task.result?.user?.uid ?: return@addOnCompleteListener
                    UserManager.saveUserId(requireContext(), userId)

                    saveUserToFirestore(userId, fullName, email, phone)
                } else {
                    showError(task.exception?.message ?: "Sign up failed")
                }
            }
    }

    private fun validateInput(fullName: String, email: String, phone: String, password: String): Boolean {
        if (fullName.isEmpty()) {
            binding.etName.error = getString(R.string.full_name_required)
            return false
        }
        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.etEmail.error = getString(R.string.valid_email_required)
            return false
        }
        if (phone.length < 9 || !phone.matches(Regex("^\\+?[0-9]{7,15}\$"))) {
            binding.etPhone.error = getString(R.string.valid_phone_required)
            return false
        }
        if (password.length < 6) {
            binding.etPassword.error = getString(R.string.password_length_error)
            return false
        }
        return true
    }

    private fun saveUserToFirestore(userId: String, fullName: String, email: String, phone: String) {
        val user = hashMapOf(
            "fullName" to fullName,
            "email" to email,
            "phone" to phone,
            "profilePicUrl" to ""
        )

        db.collection("users").document(userId)
            .set(user)
            .addOnSuccessListener {
                showSnackbar(binding.root, getString(R.string.account_created_successfully), SnackbarType.SUCCESS)

                findNavController().navigate(R.id.action_signup_to_home)
            }
            .addOnFailureListener { e ->
                showError(getString(R.string.error_saving_data) + ": ${e.message}")
            }
    }

    private fun showError(message: String) {
        showSnackbar(binding.root, message, SnackbarType.ERROR)
        binding.btnSignup.isEnabled = true
        binding.btnSignup.text = getString(R.string.sign_up)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
