package com.qanairy.services;

import org.springframework.stereotype.Service;

import com.qanairy.models.enums.FormType;

@Service
public class LabelSetsService {

	public static FormType[] getFormTypeOptions() {
		// TODO Auto-generated method stub
		return FormType.values();
	}
}
