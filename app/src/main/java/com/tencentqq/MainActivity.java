package com.tencentqq;

import android.app.Activity;
import android.os.Bundle;
import android.view.Window;

import com.tencentqq.layout.DragLayout;
import com.tencentqq.utils.Utils;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);
        DragLayout mDragLayout = (DragLayout) findViewById(R.id.dl);
        mDragLayout.setOnStatusDragListener(new DragLayout.OnStatusDragListener() {
            @Override
            public void onDragging(float percent) {
                //Utils.showToast(getApplicationContext(), "onDragging");
            }

            @Override
            public void onClose() {
                Utils.showToast(getApplicationContext(), "onClose");
            }

            @Override
            public void onOpen() {
                Utils.showToast(getApplicationContext(), "onOpen");
            }
        });
    }
}
