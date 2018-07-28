package io.github.pr0methean.betterrandom.seed

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.Proxy
import java.net.URL
import java.security.cert.Certificate
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLPeerUnverifiedException
import org.slf4j.LoggerFactory

/**
 * Used for testing [RandomDotOrgSeedGenerator].
 */
class FakeHttpsUrlConnection(url: URL, val proxy: Proxy?, responseBody: ByteArray) : HttpsURLConnection(url) {
    @Volatile
    var isDisconnected = false
        private set
    private val os = ByteArrayOutputStream()
    private val `is`: ByteArrayInputStream?

    val requestBody: ByteArray
        get() = os.toByteArray()

    init {
        `is` = ByteArrayInputStream(responseBody)
    }

    @Throws(IOException::class)
    override fun getInputStream(): InputStream? {
        if (isDisconnected) {
            throw IllegalStateException("Already disconnected")
        }
        return `is`
    }

    @Throws(IOException::class)
    override fun getOutputStream(): OutputStream {
        return os
    }

    override fun getCipherSuite(): String? {
        return null
    }

    override fun getLocalCertificates(): Array<Certificate> {
        return CERTIFICATES
    }

    @Throws(SSLPeerUnverifiedException::class)
    override fun getServerCertificates(): Array<Certificate> {
        return CERTIFICATES
    }

    override fun disconnect() {
        isDisconnected = true
    }

    override fun usingProxy(): Boolean {
        return proxy != null && proxy != Proxy.NO_PROXY
    }

    @Throws(IOException::class)
    override fun connect() {
        connected = true
    }

    protected override fun finalize() {
        try {
            `is`?.close()
            os.close()
        } catch (e: IOException) {
            LoggerFactory.getLogger(FakeHttpsUrlConnection::class.java).error("Failed to close streams", e)
        }

    }

    companion object {

        private val CERTIFICATES = arrayOfNulls<Certificate>(0)
    }
}
