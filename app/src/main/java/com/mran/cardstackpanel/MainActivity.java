package com.mran.cardstackpanel;

import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {
    Button mButton;
    CardStackPanelView mCardStackPanelView;
    int color[] = {R.color.color0, R.color.color1, R.color.color2, R.color.color3, R.color.color4, R.color.color0, R.color.color1, R.color.color2, R.color.color3, R.color.color4, R.color.color0, R.color.color1, R.color.color2, R.color.color3, R.color.color4};
    int i = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mCardStackPanelView = findViewById(R.id.card_stack_view);
        mCardStackPanelView.setType(CardStackPanelView.V_BOTTOM_TYPE);

        mCardStackPanelView.setVisibleViewCount(5);
        mCardStackPanelView.setItemInterval(30);

        mButton = findViewById(R.id.button);
        mButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final TextView textView = new TextView(getApplicationContext());
                textView.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                textView.setTextSize(30);
                textView.setBackgroundResource(color[i]);
                textView.setText(String.valueOf(i++));
                textView.setGravity(Gravity.CENTER);

//                textView.setOnClickListener(new View.OnClickListener() {
//                    @Override
//                    public void onClick(View v) {
//                        Log.d("TAG", "onClick: " + textView.getText());
//
//                    }
//                });
                mCardStackPanelView.addView(textView);
            }
        });
        mCardStackPanelView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("TAG", "onClick: " + v);


            }
        });
//        mCardStackPanelView.setVisibleViewCount(3);

    }
}
