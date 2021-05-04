package com.looksee.models.journeys;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.looksee.browsing.ActionFactory;
import com.looksee.browsing.Browser;
import com.looksee.services.StepService;

@Service
public class StepExecutor {
	private static Logger log = LoggerFactory.getLogger(StepExecutor.class);

	@Autowired
	private StepService step_service;
	
	public void execute(Browser browser, Step step) {
		assert browser != null;
		assert step != null;
		
		if(step instanceof ElementInteractionStep) {
			ElementInteractionStep interaction_step = (ElementInteractionStep)step;
			
			interaction_step.setElement(step_service.getElementState(interaction_step.getKey()));
			interaction_step.setAction(step_service.getAction(interaction_step.getKey()));

			WebElement interactive_elem = browser.getDriver().findElement(By.xpath(interaction_step.getElement().getXpath()));
			ActionFactory action_factory = new ActionFactory(browser.getDriver());
			action_factory.execAction(interactive_elem, interaction_step.getAction().getValue(), interaction_step.getAction().getName());
		}
		else if (step instanceof NavigationStep) {
			log.warn("navigation step url   :: "+((NavigationStep) step).getUrl());
			browser.navigateTo(((NavigationStep) step).getUrl());
		}
	}
}
