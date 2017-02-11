package stonybrook.ese.seniordesign.activityrecognition;

import android.os.AsyncTask;
import android.renderscript.ScriptGroup;
import android.util.Log;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * Created by kbumsik on 2/10/2017.
 */

public class CommTask extends AsyncTask<byte[], Void, Long> {
    private String hostName;
    private int port;
    private TextView text;

    public CommTask(String hostName, int port, TextView text)
    {
        this.hostName = hostName;
        this.port = port;
        this.text = text;
    }

    @Override
    protected void onPreExecute()
    {
        text.setText("Sending...");
        super.onPreExecute();
    }

    @Override
    protected Long doInBackground(byte[]... bytes) {
        long result = 0;
        Socket sock = null;
        try {
            // Connect
            InetAddress inet = InetAddress.getByName(hostName);
            sock = new Socket(inet, port);

            // get stream
            OutputStream outputStream = sock.getOutputStream();
            InputStream inputStream = sock.getInputStream();

            // Send
            for (int i = 0; i < bytes.length; i++) {
                outputStream.write(bytes[i]);
                result += bytes[i].length;
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
        text.setText("Transmission halted");
        super.onCancelled();
    }

    @Override
    protected void onPostExecute(Long result)
    {
        text.setText("Transmission completed: " + result + " bytes");
        super.onPostExecute(result);
    }
}
