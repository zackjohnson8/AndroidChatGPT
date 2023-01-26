package com.example.androidchatgpt

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.androidchatgpt.databinding.FragmentFirstBinding
import com.google.android.gms.net.CronetProviderInstaller
import org.chromium.net.CronetEngine
import org.chromium.net.CronetException
import org.chromium.net.UrlRequest
import org.chromium.net.UrlResponseInfo
import java.nio.ByteBuffer
import java.util.concurrent.Executor
import java.util.concurrent.Executors


private const val TAG = "MyUrlRequestCallback"

class MyUrlRequestCallback : UrlRequest.Callback() {
    private var responseHeaders: MutableMap<String, MutableList<String>>? = null
    private val myBuffer: ByteBuffer?
        get() {
            return ByteBuffer.allocateDirect(102400)
        }
    private val shouldFollow: Boolean
        get() {
            return true
        }

    override fun onRedirectReceived(request: UrlRequest?, info: UrlResponseInfo?, newLocationUrl: String?) {
        Log.i(TAG, "onRedirectReceived method called.")
        // You should call the request.followRedirect() method to continue
        // processing the request.
        if (shouldFollow) {
            request?.followRedirect()
        } else {
            request?.cancel()
        }

    }

    override fun onResponseStarted(request: UrlRequest?, info: UrlResponseInfo?) {
        Log.i(TAG, "onResponseStarted method called.")
        // You should call the request.read() method before the request can be
        // further processed. The following instruction provides a ByteBuffer object
        // with a capacity of 102400 bytes for the read() method. The same buffer
        // with data is passed to the onReadCompleted() method.
        request?.read(ByteBuffer.allocateDirect(102400))
        val httpStatusCode = info?.httpStatusCode
        if (httpStatusCode == 200) {
            // The request was fulfilled. Start reading the response.
            request?.read(myBuffer)
        } else if (httpStatusCode == 503) {
            // The service is unavailable. You should still check if the request
            // contains some data.
            request?.read(myBuffer)
        }
        responseHeaders = info?.allHeaders
    }

    override fun onReadCompleted(request: UrlRequest?, info: UrlResponseInfo?, byteBuffer: ByteBuffer?) {
        Log.i(TAG, "onReadCompleted method called.")
        // You should keep reading the request until there's no more data.
        byteBuffer?.clear()
        request?.read(myBuffer)
    }

    override fun onSucceeded(request: UrlRequest?, info: UrlResponseInfo?) {
        Log.i(TAG, "onSucceeded method called.")
    }

    override fun onFailed(request: UrlRequest?, info: UrlResponseInfo?, error: CronetException?) {
        Log.e(TAG, "The request failed.", error)
    }

    override fun onCanceled(request: UrlRequest?, info: UrlResponseInfo?) {
        // Free resources allocated to process this request.
        Log.i(TAG, "The request was canceled.")
    }
}



/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class FirstFragment : Fragment() {

    private var _binding: FragmentFirstBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentFirstBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        while (!CronetProviderInstaller.isInstalled()) {
            CronetProviderInstaller.installProvider(this.requireContext())
        }

        val context = this.requireContext()
        val myBuilder = CronetEngine.Builder(context)
        val myCronetEngine = myBuilder.build()

        val executor: Executor = Executors.newSingleThreadExecutor()

        val requestBuilder = myCronetEngine.newUrlRequestBuilder(
            "https://www.google.com",
            MyUrlRequestCallback(),
            executor
        )

        val request: UrlRequest = requestBuilder.build()
        request.start()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}