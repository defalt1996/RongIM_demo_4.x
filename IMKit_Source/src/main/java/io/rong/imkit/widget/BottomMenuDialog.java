package io.rong.imkit.widget;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;

import io.rong.imkit.R;

public class BottomMenuDialog extends Dialog implements View.OnClickListener {

    private View.OnClickListener confirmListener;
    private View.OnClickListener middleListener;
    private View.OnClickListener cancelListener;

    private String confirmText;
    private String middleText;
    private String cancelText;

    BottomMenuDialog(Context context) {
        super(context, R.style.dialogFullscreen);
    }

    public BottomMenuDialog(Context context, int theme) {
        super(context, theme);
    }

    public BottomMenuDialog(Context context, String confirmText, String middleText) {
        super(context, R.style.dialogFullscreen);
        this.confirmText = confirmText;
        this.middleText = middleText;
    }

    public BottomMenuDialog(Context context, String confirmText, String middleText, String cancelText) {
        super(context, R.style.dialogFullscreen);
        this.confirmText = confirmText;
        this.middleText = middleText;
        this.cancelText = cancelText;
    }

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.rc_dialog_bottom);
        Window window = getWindow();
        WindowManager.LayoutParams layoutParams;
        if (window != null) {
            layoutParams = window.getAttributes();
            layoutParams.flags = WindowManager.LayoutParams.FLAG_DIM_BEHIND;
            layoutParams.dimAmount = 0.5f;
            window.setAttributes(layoutParams);
            window.setBackgroundDrawableResource(android.R.color.transparent);
        }

        Button step = findViewById(R.id.bt_by_step);
        Button combine = findViewById(R.id.bt_combine);
        Button cancel = findViewById(R.id.bt_cancel);

        if (!TextUtils.isEmpty(confirmText)) {
            step.setText(confirmText);
        }
        if (!TextUtils.isEmpty(middleText)) {
            combine.setText(middleText);
        }
        if (!TextUtils.isEmpty(cancelText)) {
            cancel.setText(cancelText);
        }

        cancel.setOnClickListener(this);
        step.setOnClickListener(this);
        combine.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        int i = v.getId();
        View.OnClickListener listener = null;
        if (i == R.id.bt_by_step) {
            listener = confirmListener;
        } else if (i == R.id.bt_combine) {
            listener = middleListener;
        } else if (i == R.id.bt_cancel) {
            listener = cancelListener;
        }
        if (listener != null) {
            listener.onClick(v);
        }

    }

    public View.OnClickListener getConfirmListener() {
        return confirmListener;
    }

    void setConfirmListener(View.OnClickListener confirmListener) {
        this.confirmListener = confirmListener;
    }

    public View.OnClickListener getCancelListener() {
        return cancelListener;
    }

    void setCancelListener(View.OnClickListener cancelListener) {
        this.cancelListener = cancelListener;
    }

    public View.OnClickListener getMiddleListener() {
        return middleListener;
    }

    void setMiddleListener(View.OnClickListener middleListener) {
        this.middleListener = middleListener;
    }
}
