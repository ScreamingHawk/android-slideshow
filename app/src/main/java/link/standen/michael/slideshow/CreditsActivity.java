package link.standen.michael.slideshow;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.widget.TextView;

public class CreditsActivity extends AppCompatActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_credits);
		setTitle(R.string.title_activity_credits);

		// Linkify
		((TextView)findViewById(R.id.credits_creator)).setMovementMethod(LinkMovementMethod.getInstance());
		((TextView)findViewById(R.id.credits_content1)).setMovementMethod(LinkMovementMethod.getInstance());
		((TextView)findViewById(R.id.credits_content2)).setMovementMethod(LinkMovementMethod.getInstance());
		((TextView)findViewById(R.id.credits_content3)).setMovementMethod(LinkMovementMethod.getInstance());
		((TextView)findViewById(R.id.credits_content4)).setMovementMethod(LinkMovementMethod.getInstance());
		((TextView)findViewById(R.id.credits_content5)).setMovementMethod(LinkMovementMethod.getInstance());
	}
}
