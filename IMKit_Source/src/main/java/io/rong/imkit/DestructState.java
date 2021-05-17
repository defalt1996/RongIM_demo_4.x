package io.rong.imkit;

import android.os.Build;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import java.util.List;

import io.rong.imkit.emoticon.EmoticonTabAdapter;
import io.rong.imkit.plugin.IPluginModule;
import io.rong.imkit.plugin.ImagePlugin;
import io.rong.imkit.dialog.ImageVideoDialogFragment;

import static android.view.View.GONE;

/**
 * Created by Android Studio.
 * User: lvhongzhen
 * Date: 2019-08-15
 * Time: 11:21
 */
public class DestructState implements IRongExtensionState {
    private ImageVideoDialogFragment imageVideoDialog;

    DestructState() {
    }

    @Override
    public void changeView(RongExtension pExtension) {
        pExtension.refreshQuickView();
        ImageView voiceToggle = pExtension.getVoiceToggle();
        if (voiceToggle != null)
            voiceToggle.setImageResource(R.drawable.rc_destruct_voice_toggle_selector);
        ImageView pluginToggle = pExtension.getPluginToggle();
        if (pluginToggle != null)
            pluginToggle.setImageResource(R.drawable.rc_destruct_plugin_toggle_selector);
        ImageView emoticonToggle = pExtension.getEmoticonToggle();
        if (emoticonToggle != null)
            emoticonToggle.setImageResource(R.drawable.rc_destruct_emotion_toggle_selector);
        EditText editText = pExtension.getEditText();
        if (editText != null) {
            editText.setBackgroundResource(R.drawable.rc_destruct_edit_text_background_selector);
        }
        Button voiceInputToggle = pExtension.getVoiceInputToggle();
        if (voiceInputToggle != null) {
            voiceInputToggle.setTextColor(pExtension.getContext().getResources().getColor(R.color.rc_destruct_voice_color));
        }
    }

    @Override
    public void onClick(final RongExtension pExtension, View v) {
        int id = v.getId();
        if (id == R.id.rc_plugin_toggle) {
            pExtension.exitBurnMode();
        } else if (id == R.id.rc_emoticon_toggle) {
            if (imageVideoDialog == null) {
                List<IPluginModule> pluginModules = pExtension.getPluginModules();
                boolean hasSight = false;
                boolean hasImage = false;
                IPluginModule imagePlugin = null;
                IPluginModule sightPlugin = null;
                for (IPluginModule plugin : pluginModules) {
                    if (plugin instanceof ImagePlugin) {
                        hasImage = true;
                        imagePlugin = plugin;
                    } else if (plugin.getClass().getName().equals("io.rong.sight.SightPlugin")) {
                        hasSight = true;
                        sightPlugin = plugin;
                    }
                }
                imageVideoDialog = new ImageVideoDialogFragment();
                imageVideoDialog.setHasImage(hasImage);
                imageVideoDialog.setHasSight(hasSight);
                final IPluginModule finalSightPlugin = sightPlugin;
                final IPluginModule finalImagePlugin = imagePlugin;
                imageVideoDialog.setImageVideoDialogListener(new ImageVideoDialogFragment.ImageVideoDialogListener() {
                    @Override
                    public void onSightClick(View v) {
                        if (finalSightPlugin != null)
                            finalSightPlugin.onClick(pExtension.getFragment(), pExtension);
                    }

                    @Override
                    public void onImageClick(View v) {
                        if (finalImagePlugin != null)
                            finalImagePlugin.onClick(pExtension.getFragment(), pExtension);
                    }
                });
            }
            imageVideoDialog.show(pExtension.getFragment().getFragmentManager());
        } else if (id == R.id.rc_voice_toggle) {
            pExtension.clickVoice(pExtension.isRobotFirst(), pExtension, v, R.drawable.rc_destruct_emotion_toggle_selector);
        }

    }


    @Override
    public boolean onEditTextTouch(RongExtension pExtension, View v, MotionEvent event) {
        if (MotionEvent.ACTION_DOWN == event.getAction()) {
            EditText editText = pExtension.getEditText();
            if (pExtension.getExtensionClickListener() != null)
                pExtension.getExtensionClickListener().onEditTextClick(editText);
            if (Build.BRAND.toLowerCase().contains("meizu")) {
                editText.requestFocus();
                pExtension.getEmoticonToggle().setSelected(false);
                pExtension.setKeyBoardActive(true);
            } else {
                pExtension.showInputKeyBoard();
            }
            pExtension.getContainerLayout().setSelected(true);
            pExtension.hidePluginBoard();
            pExtension.hideEmoticonBoard();
            pExtension.hidePhrasesBoard();
        }
        return false;
    }

    @Override
    public void hideEmoticonBoard(ImageView pEmoticonToggle, EmoticonTabAdapter pEmotionTabAdapter) {
        pEmotionTabAdapter.setVisibility(GONE);
    }


}
