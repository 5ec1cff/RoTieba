package io.github.a13e300.ro_tieba.ui.login

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import io.github.a13e300.ro_tieba.App
import io.github.a13e300.ro_tieba.BaseFragment
import io.github.a13e300.ro_tieba.Logger
import io.github.a13e300.ro_tieba.databinding.FragmentLoginBinding
import kotlinx.coroutines.launch

private const val TIEBA_LOGIN_URL =
    "https://wappass.baidu.com/passport?login&u=https%3A%2F%2Ftieba.baidu.com%2Findex%2Ftbwise%2Fmine"

// https://github.com/HuanCheng65/TiebaLite/blob/34d6e12be5144bea0138b5dc7cafb7ad704ae3f3/app/src/main/java/com/huanchengfly/tieba/post/activities/LoginActivity.kt
// https://github.com/HuanCheng65/TiebaLite/blob/34d6e12be5144bea0138b5dc7cafb7ad704ae3f3/app/src/main/java/com/huanchengfly/tieba/post/fragments/WebViewFragment.java

class LoginFragment : BaseFragment() {
    private lateinit var binding: FragmentLoginBinding
    private val mClient = object : WebViewClient() {
        override fun onPageFinished(view: WebView?, url: String) {
            val cm = CookieManager.getInstance()
            val cookie = cm.getCookie(url)
            Logger.d("url=$url cookie=$cookie")
            if (url.startsWith("https://tieba.baidu.com/index/tbwise/") || url.startsWith("https://tiebac.baidu.com/index/tbwise/")) {
                var bduss: String? = null
                var sToken: String? = null
                var baiduId: String? = null
                cookie.split("; ").forEach {
                    val s = it.split("=", limit = 2)
                    val name = s.firstOrNull() ?: return@forEach
                    val value = s.getOrNull(1) ?: return@forEach
                    when (name) {
                        "BDUSS" -> bduss = value
                        "STOKEN" -> sToken = value
                        "BAIDUID" -> baiduId = value
                    }
                }
                if (bduss == null || sToken == null) return
                viewLifecycleOwner.lifecycleScope.launch {
                    runCatching {
                        App.instance.accountManager.addAccount(bduss!!, sToken, baiduId)
                    }.onFailure {
                        Logger.e("failed to login", it)
                        MaterialAlertDialogBuilder(requireContext())
                            .setTitle("登录失败")
                            .setMessage(it.message)
                            .show()
                    }.onSuccess {
                        setFragmentResult("login", bundleOf("success" to true))
                        navigateUp()
                    }
                }
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentLoginBinding.inflate(inflater, container, false)
        setupToolbar(binding.toolbar)
        binding.toolbar.title = "登录"
        binding.webview.apply {
            loadUrl(TIEBA_LOGIN_URL)
            webViewClient = mClient
            settings.apply {
                javaScriptEnabled = true
                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                domStorageEnabled = true
            }
            CookieManager.getInstance().setAcceptCookie(true)
        }
        return binding.root
    }
}
