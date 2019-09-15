package com.qanairy.quartz;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.PersistJobDataAfterExecution;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.qanairy.services.EngagementEmailService;


@PersistJobDataAfterExecution
@DisallowConcurrentExecution
@Component
public class EmailAfterFourtyEightHours implements Job {
 
    @Autowired
    private EngagementEmailService jobService;
 
    public void execute(JobExecutionContext context) throws JobExecutionException {
        jobService.send48HourInactivitySinceDiscoveryEmails();
    }
}
