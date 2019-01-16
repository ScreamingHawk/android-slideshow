package link.standen.michael.slideshow.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;

import link.standen.michael.slideshow.R;

public class ControlsDialog extends DialogFragment {

	public static final String TAG = ControlsDialog.class.getName();

	@NonNull
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		AlertDialog.Builder bob = new AlertDialog.Builder(getActivity());
		LayoutInflater inflater = getActivity().getLayoutInflater();

		bob.setView(inflater.inflate(R.layout.dialog_controls, null))
				.setPositiveButton(R.string.changelog_ok_button, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						ControlsDialog.this.getDialog().cancel();
					}
				});

		return bob.create();
	}
}
