package stonybrook.ese.seniordesign.activityrecognition;

import android.os.AsyncTask;
import android.util.Log;
import android.widget.TextView;

import org.w3c.dom.Text;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * Created by kbumsik on 2/10/2017.
 */

public class CommTask extends AsyncTask<byte[], String, Long> {

    public final String TAG = "CommTask";
    private String hostName;
    private int port;

    public CommTask(String hostName, int port)
    {
        this.hostName = hostName;
        this.port = port;
    }

    @Override
    protected void onPreExecute()
    {
        super.onPreExecute();
    }

    @Override
    protected Long doInBackground(byte[]... bytes) {
        long result = 0;
        byte[] input = new byte[1024];
        Socket sock = null;
        try {
            // Connect
            InetAddress inet = InetAddress.getByName(hostName);
            sock = new Socket(inet, port);

            // get stream
            OutputStream outputStream = sock.getOutputStream();
            InputStream inputStream = sock.getInputStream();

            // First receive
            do {
                inputStream.read(input);
                publishProgress(new String(input, "UTF-8"));
            } while (inputStream.available() > 0);

            // Send
            for (int i = 0; i < bytes.length; i++) {
                outputStream.write(bytes[i]);
                result += bytes[i].length;

                // receive
                do {
                    inputStream.read(input);
                    publishProgress(new String(input, "UTF-8"));
                } while (inputStream.available() > 0);
            }
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (sock != null) {
                try {
                    sock.close();
                } catch (IOException e) {
                    e.printStackTrace();    // Auto-generated.
                }
            }
        }
        return result;
    }

    @Override
    protected void onCancelled()
    {
        Log.d(TAG, "Transmission halted");
        super.onCancelled();
    }

    @Override
    protected void onPostExecute(Long result)
    {
        Log.d(TAG, "Transmission completed");
        super.onPostExecute(result);
    }

    // data that is ready to be transmitted
    static public byte[] format(String accelData, String gyroData) {
        // TODO format: label ,pos,time,Ax,Ay,Az,Gx,Gy,Gz,Mx,My,Mz
        return "?,NA,71848364590374.000000,1,1,1,1,1,1,1,1,1\n".getBytes(StandardCharsets.US_ASCII);
    }
}
