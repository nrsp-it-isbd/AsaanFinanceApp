package com.assan.finance.utils;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;

import androidx.appcompat.widget.AppCompatImageView;

public class GuidelinesImageView extends AppCompatImageView {

    private Paint paint;
    private Path path;

    public GuidelinesImageView(Context context) {
        super(context);
        init();
    }

    public GuidelinesImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public GuidelinesImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        paint = new Paint();
        paint.setColor(Color.WHITE);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(4);
        paint.setPathEffect(new DashPathEffect(new float[]{10, 20}, 0)); // Dash pattern

        path = new Path();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int width = getWidth();
        int height = getHeight();

        // Draw dashed lines as guidelines
        float centerX = width / 2f;
        float centerY = height / 2f;
        float dashLength = 50; // Length of each dash

        // Vertical dashed line
        path.reset();
        path.moveTo(centerX, 0);
        path.lineTo(centerX, height);
        canvas.drawPath(path, paint);

        // Horizontal dashed line
        path.reset();
        path.moveTo(0, centerY);
        path.lineTo(width, centerY);
        canvas.drawPath(path, paint);
    }
}
