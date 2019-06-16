package com.chx.decoder.activity;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.chx.decoder.R;
import com.chx.decoder.comparator.ComparatorFactory;
import com.chx.decoder.decoder.SwiftDecoder;
import com.chx.decoder.decoder.result.DecoderResult;
import com.chx.decoder.decoder.result.Point;

import java.util.Collections;
import java.util.List;

public abstract class DecodeActivity extends BaseActivity {

    protected FrameLayout mResultContainer, mImageResultContainer;
    protected ScrollView mTextResultContainer;
    protected LinearLayout mOperationLayout;
    protected Spinner mSpinner;
    protected ComparatorFactory.Type mType = ComparatorFactory.Type.LINE;
    protected final int VIEW_SIZE = 80;
    protected final int VIEW_MARGIN = 20;
    protected final int TEXT_SIZE = 16;
    private boolean isMove;

    private static final int COUNT_TOUCH_IGNORE = 5;
    private int count;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(getLayoutResource());
        initView();
    }

    @Override
    protected void onStop() {
        super.onStop();
        SwiftDecoder.getInstance().release();
    }

    public abstract int getLayoutResource();

    public void initView() {
        mResultContainer = (FrameLayout) findViewById(R.id.result_container);
        mResultContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                v.setVisibility(View.GONE);
                mOperationLayout.setVisibility(View.VISIBLE);
                onResultClick();
            }
        });
        mImageResultContainer = (FrameLayout) findViewById(R.id.result_container_image);
        mTextResultContainer = (ScrollView) findViewById(R.id.result_container_text);
        mTextResultContainer.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                view.getParent().requestDisallowInterceptTouchEvent(true);
                switch (event.getAction()){
                    case MotionEvent.ACTION_DOWN:
                        isMove = false;
                        count = 0;
                        break;
                    case MotionEvent.ACTION_UP:
                        if (!isMove) {
                            mResultContainer.setVisibility(View.GONE);
                            mOperationLayout.setVisibility(View.VISIBLE);
                            onResultClick();
                        }
                        break;
                    case MotionEvent.ACTION_MOVE:
                        count++;
                        if (count > COUNT_TOUCH_IGNORE) {
                            isMove = true;
                        }
                        break;
                }
                return false;
            }
        });

        mOperationLayout = (LinearLayout) findViewById(R.id.layout_operation);
        findViewById(R.id.btn_decode).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onDecodeClick();
            }
        });
        mSpinner = (Spinner) findViewById(R.id.sort_spinner);
        mSpinner.setAdapter(ArrayAdapter.createFromResource(
                this, R.array.sort_mode, android.R.layout.simple_list_item_1));
        mSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                switch (position) {
                    case 0:
                        mType = ComparatorFactory.Type.LINE;
                        break;
                    case 1:
                        mType = ComparatorFactory.Type.ROW;
                        break;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    public abstract void onDecodeClick();

    public abstract void onResultClick();

    public void decodeBitmap(Bitmap bitmap) {
        if (SwiftDecoder.getInstance().decode(bitmap) == 0) {
            Toast.makeText(getApplicationContext(), "Decoding failed", Toast.LENGTH_LONG).show();
        } else {
            List<DecoderResult> results = SwiftDecoder.getInstance().getResults();
            if (results == null || results.size() == 0) {
                return;
            }
            beforeShowResults();
            onShowResults(results);
        }
    }

    public void beforeShowResults() {
        mOperationLayout.setVisibility(View.INVISIBLE);
        mResultContainer.setVisibility(View.VISIBLE);
    }

    public void onShowResults(List<DecoderResult> results) {
        sortResults(results);
        showResultsByText(results);
        showResultsByImage(results);
    }

    public void sortResults(List<DecoderResult> results) {
        Collections.sort(results, ComparatorFactory.getComparator(mType));
    }

    public void showResultsByText(List<DecoderResult> results) {
        mTextResultContainer.removeAllViews();
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        TextView v = new TextView(this);
        v.setPadding(10, 10, 10, 10);
        v.setTextColor(Color.WHITE);
        v.setTextSize(16);
//        StringBuilder sb = new StringBuilder("total:" + results.size() + "\n");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < results.size(); i++) {
            DecoderResult result = results.get(i);
            sb.append((i + 1) + ":" + result.getResult() + "\n");
            if (i < results.size() - 1) {
                sb.append("\n");
            }
        }
        v.setText(sb.toString());
        mTextResultContainer.addView(v, layoutParams);
    }

    public void showResultsByImage(List<DecoderResult> results) {
        mImageResultContainer.removeAllViews();
        int size = results.size();
        for (int i = 0; i < size; i++) {
            DecoderResult result = results.get(i);
            TextView tv = new TextView(this);
            tv.setTextColor(getResources().getColor(R.color.mark_text));
            tv.setTextSize(TEXT_SIZE);
            tv.setBackground(getResources().getDrawable(R.drawable.mark_view_style));
            tv.setText(String.format("%d/%d", i + 1, size));
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(VIEW_SIZE, VIEW_SIZE);
            fillParams(result, params);
            mImageResultContainer.addView(tv, params);
        }
    }

    public void fillParams(DecoderResult result, FrameLayout.LayoutParams params) {
        Point point = getViewPointByBitmapPoint(result.getBounds().getMarkPoint());
        int marginLeft = point.getX() - VIEW_MARGIN;
        if (marginLeft < 0) marginLeft = 0;
        int marginTop = point.getY() - VIEW_MARGIN;
        if (marginTop < 0) marginTop = 0;
        params.setMargins(marginLeft, marginTop, 0, 0);
    }

    public abstract Point getViewPointByBitmapPoint(Point point);
}
