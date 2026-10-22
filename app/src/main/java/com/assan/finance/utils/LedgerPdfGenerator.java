package com.assan.finance.utils;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.os.Environment;

import com.assan.finance.model.assan.BorrowerSummaryData;
import com.assan.finance.model.assan.RecoveryScheduleRecord;
import com.assan.finance.sharedPreferences.SharedManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class LedgerPdfGenerator {

    public static File generatePdf(Context context, BorrowerSummaryData summary, List<RecoveryScheduleRecord> records) {
        PdfDocument document = new PdfDocument();
        // A4 landscape: 842 x 595 points
        int pageHeight = 595;
        int pageWidth = 842;
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create();
        PdfDocument.Page page = document.startPage(pageInfo);
        Canvas canvas = page.getCanvas();

        SharedManager sharedManager = SharedManager.getInstance(context);
        String branchName = sharedManager.getString("FIELD_UNIT_NAME", "N/A");
        String userId = sharedManager.getString("USER_NAME", "N/A");

        String refundAmt = "N/A";
        String refundDate = "N/A";
        String refundCheque = "N/A";

        Paint titlePaint = new Paint();
        titlePaint.setTypeface(Typeface.create(Typeface.SERIF, Typeface.NORMAL));
        titlePaint.setTextSize(20f);
        canvas.drawText("Borrower's Ledger", 15, 35, titlePaint);

        Paint linePaint = new Paint();
        linePaint.setColor(Color.BLACK);
        linePaint.setStrokeWidth(0.5f);

        canvas.drawLine(15, 45, pageWidth - 15, 45, linePaint);

        Paint leftPaint = new Paint();
        leftPaint.setTypeface(Typeface.MONOSPACE);
        leftPaint.setTextSize(9f);
        leftPaint.setTextAlign(Paint.Align.LEFT);

        Paint rightPaint = new Paint();
        rightPaint.setTypeface(Typeface.MONOSPACE);
        rightPaint.setTextSize(9f);
        rightPaint.setTextAlign(Paint.Align.RIGHT);

        Paint centerPaint = new Paint();
        centerPaint.setTypeface(Typeface.MONOSPACE);
        centerPaint.setTextSize(9f);
        centerPaint.setTextAlign(Paint.Align.CENTER);

        int startY = 60;
        int lineSpacing = 15;

        if (summary != null) {
            // Row 1
            rightPaint.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));
            canvas.drawText("Sanction No:", 110, startY, rightPaint);
            rightPaint.setTypeface(Typeface.MONOSPACE);
            canvas.drawText("Cheque No:", 350, startY, rightPaint);
            canvas.drawText("Refund Detail", 650, startY, centerPaint);
            
            canvas.drawText(safeString(summary.getSanctionNumber()), 115, startY, leftPaint);
            canvas.drawText(safeString(summary.getChequeNumber()), 355, startY, leftPaint);

            // Row 2
            rightPaint.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));
            canvas.drawText("Name:", 110, startY + lineSpacing, rightPaint);
            rightPaint.setTypeface(Typeface.MONOSPACE);
            canvas.drawText("Cheque Date:", 350, startY + lineSpacing, rightPaint);
            canvas.drawText("Amount:", 610, startY + lineSpacing, rightPaint);
            
            canvas.drawText(safeString(summary.getBorrowerName()), 115, startY + lineSpacing, leftPaint);
            canvas.drawText(safeDate(summary.getChequeDate()), 355, startY + lineSpacing, leftPaint);
            canvas.drawText(refundAmt + "  Date: " + refundDate, 615, startY + lineSpacing, leftPaint);

            // Row 3
            rightPaint.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));
            canvas.drawText("Parentage:", 110, startY + lineSpacing * 2, rightPaint);
            rightPaint.setTypeface(Typeface.MONOSPACE);
            canvas.drawText("Date of Disb:", 350, startY + lineSpacing * 2, rightPaint);
            canvas.drawText("Cheque No:", 610, startY + lineSpacing * 2, rightPaint);
            
            canvas.drawText(safeString(summary.getFatherName()), 115, startY + lineSpacing * 2, leftPaint);
            canvas.drawText(safeDate(summary.getDisbursementDate()), 355, startY + lineSpacing * 2, leftPaint);
            canvas.drawText(refundCheque, 615, startY + lineSpacing * 2, leftPaint);
            
            canvas.drawLine(520, startY + lineSpacing * 2 + 5, 827, startY + lineSpacing * 2 + 5, linePaint);

            // Row 4
            rightPaint.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));
            canvas.drawText("CNIC:", 110, startY + lineSpacing * 4, rightPaint);
            rightPaint.setTypeface(Typeface.MONOSPACE);
            canvas.drawText("Borrower No:", 350, startY + lineSpacing * 4, rightPaint);
            canvas.drawText("Financing Amount:", 610, startY + lineSpacing * 4, rightPaint);
            
            canvas.drawText(safeString(summary.getCnic()), 115, startY + lineSpacing * 4, leftPaint);
            canvas.drawText(safeString(summary.getBorrowerNo()), 355, startY + lineSpacing * 4, leftPaint);
            canvas.drawText(formatAmt(summary.getLoanAmount()), 615, startY + lineSpacing * 4, leftPaint);

            // Row 5
            rightPaint.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));
            canvas.drawText("CO Name:", 110, startY + lineSpacing * 5, rightPaint);
            rightPaint.setTypeface(Typeface.MONOSPACE);
            canvas.drawText("Type of Credit:", 350, startY + lineSpacing * 5, rightPaint);
            canvas.drawText("Priod of Credit:", 610, startY + lineSpacing * 5, rightPaint);
            
            canvas.drawText(safeString(summary.getCoName()), 115, startY + lineSpacing * 5, leftPaint);
            canvas.drawText(safeString(summary.getProductName()), 355, startY + lineSpacing * 5, leftPaint);
            canvas.drawText(summary.getLoanDuration() + "         Rate: " + summary.getInterestRate() + " %", 615, startY + lineSpacing * 5, leftPaint);

            // Row 6
            rightPaint.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));
            canvas.drawText("Type:", 110, startY + lineSpacing * 6, rightPaint);
            rightPaint.setTypeface(Typeface.MONOSPACE);
            canvas.drawText("Write Off Date:", 350, startY + lineSpacing * 6, rightPaint);
            canvas.drawText("Mode of Payment:", 610, startY + lineSpacing * 6, rightPaint);
            
            canvas.drawText(safeString(summary.getSex()), 115, startY + lineSpacing * 6, leftPaint);
            canvas.drawText("N/A", 355, startY + lineSpacing * 6, leftPaint);
            canvas.drawText(safeString(summary.getFrequency()), 615, startY + lineSpacing * 6, leftPaint);

            // Row 7
            rightPaint.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));
            canvas.drawText("Branch/Office:", 110, startY + lineSpacing * 7, rightPaint);
            rightPaint.setTypeface(Typeface.MONOSPACE);
            canvas.drawText("Write-Off Amount:", 350, startY + lineSpacing * 7, rightPaint);
            canvas.drawText("Installments:", 610, startY + lineSpacing * 7, rightPaint);
            
            canvas.drawText(branchName, 115, startY + lineSpacing * 7, leftPaint);
            canvas.drawText(formatAmt(summary.getWriteoffAmount()), 355, startY + lineSpacing * 7, leftPaint);
            canvas.drawText(String.valueOf(summary.getTotalInstallments()), 615, startY + lineSpacing * 7, leftPaint);

            // Row 8
            rightPaint.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));
            canvas.drawText("Project Name:", 110, startY + lineSpacing * 8, rightPaint);
            rightPaint.setTypeface(Typeface.MONOSPACE);
            canvas.drawText("Client Account No:", 350, startY + lineSpacing * 8, rightPaint);
            canvas.drawText("Service Charge:", 610, startY + lineSpacing * 8, rightPaint);
            
            canvas.drawText(safeString(summary.getProjectName()), 115, startY + lineSpacing * 8, leftPaint);
            canvas.drawText(safeString(summary.getClientAccount()), 355, startY + lineSpacing * 8, leftPaint);
            canvas.drawText(formatAmt(summary.getServiceCharge()) + "  Cell No: " + safeString(summary.getCell()), 615, startY + lineSpacing * 8, leftPaint);
        }

        canvas.drawLine(15, 200, pageWidth - 15, 200, linePaint);

        // Table
        int tableTop = 205;
        int y = tableTop;

        Paint thPaint = new Paint(centerPaint);
        thPaint.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));

        canvas.drawText("Install", 35, y, thPaint);
        canvas.drawText("Dues", 202, y, thPaint);
        canvas.drawText("Recovery", 512, y, thPaint);
        canvas.drawText("Over", 707, y, thPaint);
        canvas.drawText("Balance", 783, y, thPaint);

        y += 5;
        canvas.drawLine(55, y, 675, y, linePaint);
        y += 12;

        canvas.drawText("No", 35, y, thPaint);
        canvas.drawText("Due Date", 90, y, thPaint);
        canvas.drawText("PA", 170, y, rightPaint);
        canvas.drawText("SC", 225, y, rightPaint);
        canvas.drawText("Pen", 280, y, rightPaint);
        canvas.drawText("Total", 345, y, rightPaint);

        canvas.drawText("Date", 385, y, thPaint);
        canvas.drawText("Receipt No", 465, y, thPaint);
        canvas.drawText("PA", 520, y, rightPaint);
        canvas.drawText("SC", 570, y, rightPaint);
        canvas.drawText("Excess", 615, y, rightPaint);
        canvas.drawText("Total", 670, y, rightPaint);

        canvas.drawText("Due", 707, y, thPaint);
        canvas.drawText("PA", 783, y, thPaint);

        y += 5;
        canvas.drawLine(15, y, pageWidth - 15, y, linePaint);
        y += 15;

        int currentPageTableTop = tableTop - 12;
        int vL1 = 55, vL2 = 350, vL3 = 675, vL4 = 740;

        double totDuePa = 0, totDueSc = 0, totDuePen = 0, totDueTotal = 0;
        double totRecPa = 0, totRecSc = 0, totRecExcess = 0, totRecTotal = 0;

        if (records != null) {
            for (RecoveryScheduleRecord r : records) {
                if (y > pageHeight - 50) {
                    drawVerticalLines(canvas, linePaint, 15, pageWidth - 15, vL1, vL2, vL3, vL4, currentPageTableTop, y - 10);
                    drawFooter(canvas, leftPaint, rightPaint, centerPaint, userId, pageInfo.getPageNumber());
                    document.finishPage(page);
                    pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageInfo.getPageNumber() + 1).create();
                    page = document.startPage(pageInfo);
                    canvas = page.getCanvas();
                    
                    currentPageTableTop = 40;
                    y = currentPageTableTop;
                    canvas.drawLine(15, y, pageWidth - 15, y, linePaint);
                    y += 15;
                }

                canvas.drawText(String.valueOf(r.getInstallmentNo()), 35, y, centerPaint);
                canvas.drawText(safeDate(r.getDueDate()), 60, y, leftPaint);
                canvas.drawText(formatAmt(r.getPrincipalDue()), 170, y, rightPaint);
                canvas.drawText(formatAmt(r.getServiceChargeDue()), 225, y, rightPaint);
                canvas.drawText(formatAmt(r.getPenaltyDue()), 280, y, rightPaint);
                canvas.drawText(formatAmt(r.getTotalDue()), 345, y, rightPaint);
                
                canvas.drawText(safeDate(r.getReceiptDate()), 355, y, leftPaint);
                canvas.drawText(safeString(r.getReceiptNo()), 425, y, leftPaint);
                canvas.drawText(formatAmt(r.getRecoveredPrincipal()), 520, y, rightPaint);
                canvas.drawText(formatAmt(r.getRecoveredServiceCharge()), 570, y, rightPaint);
                canvas.drawText(formatAmt(r.getExcessRecovery()), 615, y, rightPaint);
                // Total recovered = Recovered PA + Recovered SC + Excess
                double recAmount = r.getRecoveredPrincipal() + r.getRecoveredServiceCharge() + r.getExcessRecovery();
                canvas.drawText(formatAmt(recAmount), 670, y, rightPaint);
                
                canvas.drawText(formatAmt(r.getOutstandingPrincipal() + r.getOutstandingServiceCharge()), 735, y, rightPaint);
                canvas.drawText(formatAmt(r.getOutstandingPrincipal()), 822, y, rightPaint);
                
                y += 15;

                totDuePa += r.getPrincipalDue();
                totDueSc += r.getServiceChargeDue();
                totDuePen += r.getPenaltyDue();
                totDueTotal += r.getTotalDue();
                totRecPa += r.getRecoveredPrincipal();
                totRecSc += r.getRecoveredServiceCharge();
                totRecExcess += r.getExcessRecovery();
                totRecTotal += (r.getRecoveredPrincipal() + r.getRecoveredServiceCharge() + r.getExcessRecovery());
            }
        }

        y += 40; // Add significant vertical space ("bohot nichy")
        canvas.drawLine(15, y, pageWidth - 15, y, linePaint);
        y += 15;
        // Move "Total:" label under the Recovery section (x=425)
        canvas.drawText("Total:", 425, y, leftPaint);
        canvas.drawText(formatAmt(totDuePa), 170, y, rightPaint);
        canvas.drawText(formatAmt(totDueSc), 225, y, rightPaint);
        canvas.drawText(formatAmt(totDuePen), 280, y, rightPaint);
        canvas.drawText(formatAmt(totDueTotal), 345, y, rightPaint);
        canvas.drawText(formatAmt(totRecPa), 520, y, rightPaint);
        canvas.drawText(formatAmt(totRecSc), 570, y, rightPaint);
        canvas.drawText(formatAmt(totRecExcess), 615, y, rightPaint);
        canvas.drawText(formatAmt(totRecTotal), 670, y, rightPaint);
        
        y += 5;
        canvas.drawLine(15, y, pageWidth - 15, y, linePaint);
        
        drawVerticalLines(canvas, linePaint, 15, pageWidth - 15, vL1, vL2, vL3, vL4, currentPageTableTop, y);
        drawFooter(canvas, leftPaint, rightPaint, centerPaint, userId, pageInfo.getPageNumber());

        document.finishPage(page);

        // Save
        try {
            File docsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
            if (docsDir != null && !docsDir.exists()) {
                docsDir.mkdirs();
            }
            
            String borrowerNo = "Unknown";
            if (summary != null && summary.getBorrowerNo() != null) {
                borrowerNo = summary.getBorrowerNo();
            }
            borrowerNo = borrowerNo.replaceAll("[^a-zA-Z0-9-]", "_");

            String timestamp = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault()).format(new java.util.Date());
            File file = new File(docsDir, "Ledger_" + borrowerNo + "_" + timestamp + ".pdf");
            
            document.writeTo(new java.io.FileOutputStream(file));
            document.close();
            return file;
        } catch (IOException e) {
            e.printStackTrace();
            document.close();
            return null;
        }
    }

    private static void drawVerticalLines(Canvas canvas, Paint paint, int bL, int bR, int v1, int v2, int v3, int v4, int topY, int bottomY) {
        canvas.drawLine(bL, topY, bL, bottomY, paint);
        canvas.drawLine(v1, topY, v1, bottomY, paint);
        canvas.drawLine(v2, topY, v2, bottomY, paint);
        canvas.drawLine(v3, topY, v3, bottomY, paint);
        canvas.drawLine(v4, topY, v4, bottomY, paint);
        canvas.drawLine(bR, topY, bR, bottomY, paint);
    }

    private static void drawFooter(Canvas canvas, Paint leftP, Paint rightP, Paint centerP, String userId, int pageNum) {
        int y = 575;
        leftP.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));
        canvas.drawText("NRSP-MEDP", 15, y - 10, leftP);
        leftP.setTypeface(Typeface.MONOSPACE);
        canvas.drawLine(15, y - 5, 827, y - 5, new Paint()); // thin line

        String time = new java.text.SimpleDateFormat("dd-MMM-yy HH:mm:ss", java.util.Locale.US).format(new java.util.Date()).toUpperCase();
        canvas.drawText(time, 15, y + 10, leftP);
        
        canvas.drawText("User ID: " + userId, 200, y + 10, leftP);
        canvas.drawText("Form: LoanReportsForm", 400, y + 10, leftP);
        canvas.drawText("Page " + pageNum + " of 1", 827, y + 10, rightP);
    }

    private static String safeString(String val) {
        return (val == null || val.equalsIgnoreCase("null")) ? "" : val;
    }

    private static String safeDate(String val) {
        if (val == null || val.isEmpty() || val.equalsIgnoreCase("null")) return "";
        String dateStr = val.contains("T") ? val.split("T")[0] : val;
        try {
            java.text.SimpleDateFormat inputFormat;
            if (dateStr.contains("-") && dateStr.split("-")[0].length() == 4) {
                inputFormat = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US);
            } else if (dateStr.contains("-") && dateStr.split("-")[2].length() == 4) {
                inputFormat = new java.text.SimpleDateFormat("dd-MM-yyyy", java.util.Locale.US);
            } else if (dateStr.contains("/")) {
                inputFormat = new java.text.SimpleDateFormat("MM/dd/yyyy", java.util.Locale.US);
            } else {
                return dateStr.toUpperCase();
            }
            java.util.Date date = inputFormat.parse(dateStr);
            java.text.SimpleDateFormat outputFormat = new java.text.SimpleDateFormat("dd-MMM-yy", java.util.Locale.US);
            return outputFormat.format(date).toUpperCase();
        } catch (Exception e) {
            return dateStr.toUpperCase();
        }
    }

    private static String formatAmt(double val) {
        if (val == 0) return "0";
        return String.format(Locale.US, "%,.0f", val);
    }
}
