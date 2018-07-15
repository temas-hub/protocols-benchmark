package benchmark.protocol.temas.com.streamclient

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import com.temas.protocols.benchmark.transport.Client
import com.temas.protocols.benchmark.transport.Client.Companion.defaultPort
import com.temas.protocols.benchmark.udp.UdpClient

class StreamClientActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stream_client)

        UdpClient("192.168.1.125", defaultPort).init()
    }
}
