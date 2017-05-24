package edu.uiowa.cs.bpmonitor;

import android.app.Activity;
import android.content.Context;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class BPMonitorActivity extends AppCompatActivity implements TextViewUpdateCallback {

    private static final String TAG = "BPMonitorActivity";

    private Button connectButton, disconnectButton;
    private EditText addressEditText;
    private TextView displayTextView;
    private Context context;
    private BPMonitorBLE bpMonitorBLE;
    private Activity curAct;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bpmonitor);
        // the context and curAct are needed for passing to the BLE class for call backs and
        // bluetooth connectivity
        context = this;
        curAct = this;
        // initialize the UI components
        initUI();
    }

    /**
     * Initialize all the buttons and the UI components, add logic to the buttons
     * */
    private void initUI() {
        connectButton = (Button) findViewById(R.id.button);
        disconnectButton = (Button) findViewById(R.id.button2);
        displayTextView = (TextView) findViewById(R.id.displayTextView);
        displayTextView.setText("Sys.: NA, Dia.: NA, Pulse: NA");
        disconnectButton.setEnabled(false);
        addressEditText = (EditText) findViewById(R.id.addressEditText);

        // when connect is clicked, start the BLE connection and hand over control to BPMonitorBLE.java
        connectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "clicked connect");
                bpMonitorBLE = new BPMonitorBLE(addressEditText.getText().toString(), context, (TextViewUpdateCallback) curAct);
                displayTextView.setText("Working...");
                disconnectButton.setEnabled(true);
            }
        });

        // disconnect from device
        disconnectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bpMonitorBLE.closeConnection();
                disconnectButton.setEnabled(false);
            }
        });
    }

    @Override
    public void updateTextView(String toDisplay) {
        displayTextView.setText(toDisplay);
    }
}
