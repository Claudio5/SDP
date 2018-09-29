package ch.epfl.sweng.SDP;

import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;

public class HomeActivity extends AppCompatActivity {
    View view;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        view = this.getWindow().getDecorView();
        view.setBackgroundResource(R.color.colorGrey);

        TextView drawText = findViewById(R.id.drawButton);
        Typeface type = Typeface.createFromAsset(getAssets(),"fonts/Muroslant.otf");
        drawText.setTypeface(type);
        drawText.setPadding(0, -14, 0, 0);
    }
}