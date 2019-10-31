package jp.ac.agu.wil;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {
    TextView DebugMessage;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        DebugMessage=findViewById(R.id.DebugMessage);
    }

    public void BtnClick(View view) {
        if (view.getId()==R.id.start_button) {
            DebugMessage.setText("You push a Start button");
        }else if(view.getId()==R.id.stop_button){
            DebugMessage.setText("You push a Stop button");
        }
    }
}
