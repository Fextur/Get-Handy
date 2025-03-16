package com.example.gethandy.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.gethandy.R
import com.example.gethandy.databinding.FragmentLoginBinding
import com.example.gethandy.utils.SnackbarType
import com.example.gethandy.utils.UserManager
import com.example.gethandy.utils.showSnackbar
import com.google.firebase.auth.FirebaseAuth

class LoginFragment : Fragment() {

    private lateinit var binding: FragmentLoginBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()

        binding.btnSignup.setOnClickListener {
            findNavController().navigate(R.id.action_login_to_signup)
        }

        binding.btnLogin.setOnClickListener {
            loginUser()
        }
    }

    private fun loginUser() {
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()

        if (!isValidEmail(email)) {
            binding.etEmail.error = getString(R.string.valid_email_required)
            return
        }

        if (password.length < 6) {
            binding.etPassword.error = getString(R.string.password_length_error)
            return
        }

        binding.btnLogin.isEnabled = false
        binding.btnLogin.text = getString(R.string.logging_in)

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val userId = auth.currentUser?.uid ?: return@addOnCompleteListener
                    UserManager.saveUserId(requireContext(), userId)

                    showSnackbar(binding.root, getString(R.string.login_successful), SnackbarType.SUCCESS)
                    findNavController().navigate(R.id.action_login_to_home)
                } else {
                    showSnackbar(binding.root, getString(R.string.invalid_credentials), SnackbarType.ERROR)
                    binding.btnLogin.isEnabled = true
                    binding.btnLogin.text = getString(R.string.login)
                }
            }
    }

    private fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }
}
