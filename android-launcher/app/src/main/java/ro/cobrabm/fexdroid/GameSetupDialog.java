package ro.cobrabm.fexdroid;

import android.app.Activity;
import android.app.AlertDialog;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/** Message and actions share a scrollable view: AlertDialog message + list hides the list. */
final class GameSetupDialog {
    interface Selection { void select(int index); }
    static AlertDialog show(Activity activity, Selection selection) {
        int pad=Math.round(20*activity.getResources().getDisplayMetrics().density);
        LinearLayout body=new LinearLayout(activity);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(pad,pad,pad,pad);
        TextView message=new TextView(activity);
        message.setText(TlouLanguage.t(activity,"Choose how to add your game. Installation and preparation run automatically."));
        message.setPadding(0,0,0,pad);
        body.addView(message);
        ScrollView scroll=new ScrollView(activity);scroll.addView(body);
        AlertDialog dialog=new AlertDialog.Builder(activity)
            .setTitle(TlouLanguage.t(activity,"Set up TLOU-AE"))
            .setView(scroll).setCancelable(false).create();
        String[] labels={"Install PKG","Select game folder","Not now"};
        for(int i=0;i<labels.length;i++) {
            final int index=i;
            Button action=new Button(activity);
            action.setText(TlouLanguage.t(activity,labels[i]));
            action.setAllCaps(false);
            action.setOnClickListener(v->{dialog.dismiss();selection.select(index);});
            body.addView(action,new LinearLayout.LayoutParams(-1,-2));
        }
        dialog.show();return dialog;
    }
}
