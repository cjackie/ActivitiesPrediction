package stonybrook.ese.seniordesign.activityrecognition;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

public class MainEntry extends AppCompatActivity implements View.OnClickListener {
    final static String TAG = "AR_MainEntry";

    private Button collectBtn;
    private Button predictBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.entry);
        collectBtn = (Button) findViewById(R.id.collectBtn);
        predictBtn = (Button) findViewById(R.id.predictBtn);

        collectBtn.setOnClickListener(this);
        predictBtn.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == collectBtn.getId()) {
            startActivity(new Intent(this, CollectActivity.class));
        } else if (id == predictBtn.getId()) {
            
        } else {
            Log.d(TAG, "onClick: unexpected");
        }
    }
}
