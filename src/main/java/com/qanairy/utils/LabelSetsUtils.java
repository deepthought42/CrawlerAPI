package com.qanairy.utils;

import org.springframework.stereotype.Service;

import com.qanairy.models.enums.FormType;

@Service
public class LabelSetsUtils {

	public static FormType[] getFormTypeOptions() {
		return FormType.values();
	}
}
