package org.sakaiproject.s2u.copyright.job.utils;

import java.util.List;

import lombok.extern.slf4j.Slf4j;

import org.quartz.JobExecutionContext;
import org.quartz.SchedulerException;

@Slf4j
public class JobUtils {

    public static boolean isJobRunning(JobExecutionContext jobExecutionContext) {
        try {
            List<JobExecutionContext> jobs = jobExecutionContext.getScheduler().getCurrentlyExecutingJobs();
            for (JobExecutionContext job : jobs) {
                if (job.getJobDetail().getKey().getName().equals(jobExecutionContext.getJobDetail().getKey().getName()) && !job.getJobInstance().equals(jobExecutionContext.getJobInstance())) {
                    log.error("Aborting execution: There's another running instance of the job {}.", job.getJobDetail());
                    return true;
                }
            }
        } catch(SchedulerException ex) {
            log.warn("WARNING: " + ex.getMessage());
        }
        return false;
    }
}
