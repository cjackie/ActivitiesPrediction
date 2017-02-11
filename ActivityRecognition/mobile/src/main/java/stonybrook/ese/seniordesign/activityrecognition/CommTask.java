package stonybrook.ese.seniordesign.activityrecognition;

import android.os.AsyncTask;
import android.widget.TextView;

import org.w3c.dom.Text;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * Created by kbumsik on 2/10/2017.
 */

public class CommTask extends AsyncTask<byte[], String, Long> {
    private String hostName;
    private int port;
    private TextView statusText;
    private TextView recvText;

    public CommTask(String hostName, int port, TextView statusText, TextView recvText)
    {
        this.hostName = hostName;
        this.port = port;
        this.statusText = statusText;
        this.recvText = recvText;
    }

    @Override
    protected void onPreExecute()
    {
        statusText.setText("Sending...");
        recvText.setText("");
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
    protected void onProgressUpdate(String... recvMsg)
    {
        String string = recvText.getText().toString() + recvMsg[0];
        recvText.setText(string);
    }

    @Override
    protected void onCancelled()
    {
        statusText.setText("Transmission halted");
        super.onCancelled();
    }

    @Override
    protected void onPostExecute(Long result)
    {
        statusText.setText("Transmission completed: " + result + " bytes");
        super.onPostExecute(result);
    }
}
