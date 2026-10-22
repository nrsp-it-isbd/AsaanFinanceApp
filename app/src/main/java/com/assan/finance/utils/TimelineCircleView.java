package com.assan.finance.utils;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import com.assan.finance.apis.ApiManager;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class TimelineCircleView extends View {
    Paint paint;
    private int currentStatus = 0;
    private int clientId;
    private StatusUpdateListener statusUpdateListener;

    public TimelineCircleView(Context context) {
        super(context);
        init();
        updateStatusFromAPI();
    }

    public TimelineCircleView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
        updateStatusFromAPI();
    }

    public TimelineCircleView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
        updateStatusFromAPI();
    }

    private void init() {
        paint = new Paint();
        paint.setAntiAlias(true);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int dotRadius = 24; // smaller, cleaner dot
        int gap = 160; // vertical gap between dots
        int leftMargin = 120; // left margin
        int topMargin = 150; // top margin

        int status = currentStatus; // 0 = In Progress, 1 = Approved, 2 = Rejected

        String[] statusTexts = {"In Progress", "Approved", "Rejected"};

        // Paint definitions
        Paint paintActive = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintActive.setColor(Color.parseColor("#1B5E20")); // Theme Green
        paintActive.setStyle(Paint.Style.FILL);

        Paint paintRejected = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintRejected.setColor(Color.parseColor("#EF4444")); // Theme Red
        paintRejected.setStyle(Paint.Style.FILL);

        Paint paintStrokeInactive = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintStrokeInactive.setColor(Color.parseColor("#CBD5E1"));
        paintStrokeInactive.setStyle(Paint.Style.STROKE);
        paintStrokeInactive.setStrokeWidth(6);

        Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        linePaint.setStrokeWidth(8);

        Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setTextSize(44);
        textPaint.setTypeface(android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD));

        // Draw Line between Circle 0 and Circle 1
        if (status == 1) {
            linePaint.setColor(Color.parseColor("#1B5E20"));
        } else {
            linePaint.setColor(Color.parseColor("#E2E8F0"));
        }
        canvas.drawLine(leftMargin, topMargin + dotRadius, leftMargin, topMargin + gap - dotRadius, linePaint);

        // Draw Line between Circle 1 and Circle 2
        if (status == 2) {
            linePaint.setColor(Color.parseColor("#EF4444"));
        } else {
            linePaint.setColor(Color.parseColor("#E2E8F0"));
        }
        canvas.drawLine(leftMargin, topMargin + gap + dotRadius, leftMargin, topMargin + 2 * gap - dotRadius, linePaint);

        // Draw Circle 0 (In Progress)
        canvas.drawCircle(leftMargin, topMargin, dotRadius, paintActive);
        textPaint.setColor(Color.parseColor("#1F2937"));
        canvas.drawText(statusTexts[0], leftMargin + dotRadius * 2.5f, topMargin + 14, textPaint);

        // Draw Circle 1 (Approved)
        if (status == 1) {
            canvas.drawCircle(leftMargin, topMargin + gap, dotRadius, paintActive);
            textPaint.setColor(Color.parseColor("#1B5E20"));
            canvas.drawText(statusTexts[1], leftMargin + dotRadius * 2.5f, topMargin + gap + 14, textPaint);
        } else {
            canvas.drawCircle(leftMargin, topMargin + gap, dotRadius, paintStrokeInactive);
            textPaint.setColor(Color.parseColor("#9CA3AF"));
            canvas.drawText(statusTexts[1], leftMargin + dotRadius * 2.5f, topMargin + gap + 14, textPaint);
        }

        // Draw Circle 2 (Rejected)
        if (status == 2) {
            canvas.drawCircle(leftMargin, topMargin + 2 * gap, dotRadius, paintRejected);
            textPaint.setColor(Color.parseColor("#EF4444"));
            canvas.drawText(statusTexts[2], leftMargin + dotRadius * 2.5f, topMargin + 2 * gap + 14, textPaint);
        } else {
            canvas.drawCircle(leftMargin, topMargin + 2 * gap, dotRadius, paintStrokeInactive);
            textPaint.setColor(Color.parseColor("#9CA3AF"));
            canvas.drawText(statusTexts[2], leftMargin + dotRadius * 2.5f, topMargin + 2 * gap + 14, textPaint);
        }
    }

    public void setStatusUpdateListener(StatusUpdateListener listener) {
        this.statusUpdateListener = listener;
    }

    public void updateStatusFromAPI() {
        fetchDataAndUpdateStatus();
    }

    public void setClientId(int clientId) {
        this.clientId = clientId;
        updateStatusFromAPI();
    }

    private void fetchDataAndUpdateStatus() {
        // Obsolete PHP API removed
        // OkHttpClient client = new OkHttpClient();
        // Request request = new Request.Builder()
        //         .url(ApiManager.BASE_URL + "get_loan_status_through_clientid.php?clientid=" + clientId)
        //         .build();

        // client.newCall(request).enqueue(new Callback() {
        //     @Override
        //     public void onFailure(Call call, IOException e) {
        //         // Handle failure
        //     }

        //     @Override
        //     public void onResponse(Call call, Response response) throws IOException {
        //         if (response.isSuccessful()) {
        //             String responseData = response.body().string();
        //             int dbStatus = parseStatusFromResponse(responseData);
        //             Log.e("MyLoanStatusTest::", "DB Status: " + dbStatus);
        //             
        //             currentStatus = dbStatus;
        //             new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> invalidate());
        //         }
        //     }
        // });
    }

    private int mapDbStatusToTimelineStatus(int dbStatus) {
        if (dbStatus == 0) {
            return 0; // In Progress
        } else if (dbStatus == 2 || dbStatus == 5) {
            return 1; // Approved
        } else if (dbStatus == 3 || dbStatus == 6) {
            return 2; // Rejected
        }
        return 0; // Default: In Progress
    }

    private int parseStatusFromResponse(String responseData) {
        try {
            JSONObject jsonObject = new JSONObject(responseData);
            JSONArray statusArray = jsonObject.getJSONArray("STATUS");
            if (statusArray.length() > 0) {
                JSONObject statusObject = statusArray.getJSONObject(0);
                String statusValue = statusObject.getString("STATUS");
                return Integer.parseInt(statusValue);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return 0; // Default value or handle the parsing error
    }

    public void setCurrentStatus(int status) {
        currentStatus = status;
        invalidate(); // Redraw view
    }


    public interface StatusUpdateListener {
        void onStatusUpdate(int status);
    }
}