package com.crawlerApi.utils;

import org.springframework.stereotype.Service;

import com.crawlerApi.models.enums.FormType;

@Service
public class LabelSetsUtils {

	public static FormType[] getFormTypeOptions() {
		return FormType.values();
	}
}
