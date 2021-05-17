package io.rong.imkit.widget;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.media.MediaScannerConnection;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.PopupWindow;
import android.widget.Toast;

import java.io.File;

import io.rong.common.FileUtils;
import io.rong.imkit.R;
import io.rong.imkit.utilities.KitStorageUtils;

public class PicturePopupWindow extends PopupWindow {

    public PicturePopupWindow(final Context context, final File imageFile) {
        super(context);
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        final View menuView = inflater.inflate(R.layout.rc_pic_popup_window, null);
        menuView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });

        Button btn_save_pic = menuView.findViewById(R.id.rc_content);
        btn_save_pic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String saveImagePath = KitStorageUtils.getImageSavePath(v.getContext());

                if (imageFile != null && imageFile.exists()) {
                    String name = System.currentTimeMillis() + ".jpg";
                    FileUtils.copyFile(imageFile, saveImagePath + File.separator, name);
                    MediaScannerConnection.scanFile(context.getApplicationContext(), new String[]{saveImagePath + File.separator + name}, null, null);
                    Toast.makeText(context, String.format(context.getString(R.string.rc_save_picture_at), saveImagePath + File.separator + name), Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(context, context.getString(R.string.rc_src_file_not_found), Toast.LENGTH_SHORT).show();
                }
                dismiss();
            }
        });
        Button btn_cancel = menuView.findViewById(R.id.rc_btn_cancel);
        btn_cancel.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                dismiss();
            }
        });

        this.setContentView(menuView);
        this.setWidth(LayoutParams.MATCH_PARENT);
        this.setHeight(LayoutParams.WRAP_CONTENT);
        this.setFocusable(true);
        ColorDrawable dw = new ColorDrawable(0xb0000000);
        this.setBackgroundDrawable(dw);
    }
}
