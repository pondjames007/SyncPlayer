package com.example.otherFunction;

import android.widget.CompoundButton;
import android.widget.RadioButton;

public class RadioGroup1 implements RadioButton.OnCheckedChangeListener {
	private CompoundButton checkedButton = null;

	public void addRadioButton(RadioButton rb) {
		rb.setOnCheckedChangeListener(this);
		if (checkedButton == null) {
			checkedButton = rb;
		}
	}

	public void onCheckedChanged(CompoundButton buttonView,boolean isChecked) {
		if (isChecked) {
			checkedButton.setChecked(false);
			checkedButton = buttonView;
		}
	}

	public CompoundButton getCheckedRadioButton() {
		return checkedButton;
	}
}