package org.sakaiproject.s2u.copyright.job;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import org.sakaiproject.s2u.copyright.job.utils.JobUtils;
import org.sakaiproject.s2u.copyright.logic.CopyrightCheckerService;
import org.sakaiproject.s2u.copyright.logic.SakaiProxy;
import org.sakaiproject.s2u.copyright.model.IntellectualPropertyFile;
import org.sakaiproject.s2u.copyright.model.IntellectualPropertyFileState;

@Slf4j
public class ContentEventProcessorJob implements Job {

    @Setter private SakaiProxy sakaiProxy;
    @Setter private CopyrightCheckerService copyrightCheckerService;

    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        //Abort if there is another execution of the job
        if(JobUtils.isJobRunning(jobExecutionContext)) return;

        //Control variables
        int total = 0;
        int success = 0;
        int failed = 0;
        long startExecutionTime = System.nanoTime();

        //Get the resource time to live in order to hide resources modified before this cut date.
        int resourceDuration = sakaiProxy.getConfigParam(SakaiProxy.CONFIG_FILE_DURATION, SakaiProxy.DEFAULT_FILE_DURATION);
        Calendar c = Calendar.getInstance();
        c.add(Calendar.DAY_OF_YEAR, - resourceDuration);
        Date hideDate = c.getTime();

        //Enable an admin session
        sakaiProxy.startAdminSession();

        log.info("-------START Executing ContentEventProcessorJob-------");
        List<IntellectualPropertyFile> files = new ArrayList<>();
        files.addAll(copyrightCheckerService.findIntellectualPropertyFilesByState(IntellectualPropertyFileState.NONE));
        files.addAll(copyrightCheckerService.findIntellectualPropertyFilesByState(IntellectualPropertyFileState.GT10PERM));

        for(IntellectualPropertyFile IntellectualPropertyFile : files) {
            Date IntellectualPropertyFileDate = IntellectualPropertyFile.getModified();
            if(IntellectualPropertyFileDate.before(hideDate)){
                String resourceId = IntellectualPropertyFile.getFileId();
                log.info("The file {} modified date is going to be hidden.", resourceId);
                boolean hidden = sakaiProxy.setContentResourceVisibility(resourceId, false);
                if (!hidden) {
                    log.error("Error hiding the file {}.", resourceId);
                    failed++;
                } else {
                    success++;
                    log.info("File {} hidden successfully.", resourceId);
                    IntellectualPropertyFile.setState(IntellectualPropertyFileState.DENIED);
                    boolean persisted = copyrightCheckerService.saveIntellectualPropertyFile(IntellectualPropertyFile);
                    log.info("IP File {} persisted? {}.", resourceId, persisted);
                }
            }
        }

        //Logging the results of the execution
        log.info("--Total users processed {}",total);
        log.info("--Success events {}",success);
        log.info("--Failed events {}",failed);
        long endExecutionTime = System.nanoTime();
        log.info("--Job executed in {} seconds",((double) (endExecutionTime - startExecutionTime) / 1000000000.0));
        log.info("-------END Executing ContentEventProcessorJob-------");

        //Invalidate the session
        sakaiProxy.invalidateCurrentSession();
    }
}
