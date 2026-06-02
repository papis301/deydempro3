package com.pisco.deydempro3;

import android.graphics.Bitmap;

public class DriverDocument {

    private int id;

    private String type;

    private String rectoUrl;

    private String versoUrl;

    private String status;

    private String reason;
    private Bitmap rectoBitmap;
    private Bitmap versoBitmap;

    public DriverDocument(
            int id,
            String type,
            String rectoUrl,
            String versoUrl,
            String status,
            String reason
    ) {

        this.id = id;
        this.type = type;
        this.rectoUrl = rectoUrl;
        this.versoUrl = versoUrl;
        this.status = status;
        this.reason = reason;
    }

    public int getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public String getRectoUrl() {
        return rectoUrl;
    }

    public String getVersoUrl() {
        return versoUrl;
    }

    public String getStatus() {
        return status;
    }

    public String getReason() {
        return reason;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setRectoUrl(String rectoUrl) {
        this.rectoUrl = rectoUrl;
    }

    public void setVersoUrl(String versoUrl) {
        this.versoUrl = versoUrl;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public boolean isApproved() {
        return "approved".equalsIgnoreCase(status);
    }

    public boolean isPending() {
        return "pending".equalsIgnoreCase(status);
    }

    public boolean isRejected() {
        return "rejected".equalsIgnoreCase(status);
    }

    public Bitmap getRectoBitmap() {
        return rectoBitmap;
    }

    public void setRectoBitmap(Bitmap rectoBitmap) {
        this.rectoBitmap = rectoBitmap;
    }

    public Bitmap getVersoBitmap() {
        return versoBitmap;
    }

    public void setVersoBitmap(Bitmap versoBitmap) {
        this.versoBitmap = versoBitmap;
    }
}