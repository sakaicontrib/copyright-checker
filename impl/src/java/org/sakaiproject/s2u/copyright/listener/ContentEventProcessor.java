package org.sakaiproject.s2u.copyright.listener;

import java.util.Date;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import org.sakaiproject.content.api.ContentHostingService;
import org.sakaiproject.content.api.ContentResource;
import org.sakaiproject.entity.api.EntityPropertyNotDefinedException;
import org.sakaiproject.entity.api.EntityPropertyTypeException;
import org.sakaiproject.entity.api.ResourceProperties;
import org.sakaiproject.event.api.Event;
import org.sakaiproject.s2u.copyright.logic.EventProcessorLogic;
import org.sakaiproject.s2u.copyright.logic.CopyrightCheckerService;
import org.sakaiproject.s2u.copyright.logic.SakaiProxy;
import org.sakaiproject.s2u.copyright.logic.listener.EventProcessor;
import org.sakaiproject.s2u.copyright.model.IntellectualPropertyFile;
import org.sakaiproject.s2u.copyright.model.IntellectualPropertyFileProperty;
import org.sakaiproject.s2u.copyright.model.IntellectualPropertyFileState;

@Slf4j
public class ContentEventProcessor implements EventProcessor {

    @Setter protected CopyrightCheckerService copyrightCheckerService;
    @Setter protected EventProcessorLogic eventProcessorLogic;
    @Setter protected SakaiProxy sakaiProxy;

    private static final String CONTENT_ENTITY_PREFIX = "/content";
    private static final String ATTACHMENT_COLLECTION_PREFIX = "/attachment";

    @Override
    synchronized public void processEvent(Event event) {
        log.debug("\n\n\n=============================================================\n{}\n=============================================================\n\n\n", event);

        String eventKey = event.getEvent();
        log.debug("Found a {} event, processing it.", eventKey);

        //Get all the resource information from the event.
        String eventResource = event.getResource();
        String siteId = event.getContext();
        String userId = event.getUserId();

        //We must abort the process when....
        //The event reference is not valid.
        //The event has no user.
        //The event has no context
        if(StringUtils.isEmpty(eventResource) || StringUtils.isEmpty(userId) || StringUtils.isEmpty(siteId)) {
            log.error("Handled a not valid event {}, it needs an eventResource, an userId and a context.", event);
            return;
        }

        switch(eventKey) {
            case ContentHostingService.EVENT_RESOURCE_UPD_NEW_VERSION:
                handleIpFile(event);
                break;
            case ContentHostingService.EVENT_RESOURCE_UPD_VISIBILITY:
                handleIpFile(event);
                break;
            case ContentHostingService.EVENT_RESOURCE_REMOVE:
                handleRemovedIpFile(event);
                break;
            default:
                log.error("Event {} not supported.", eventKey);
                break;
        }
        log.debug("The event {} has been processed successfully.", event);
    }

    private void handleIpFile(Event event) {
        //Get all the resource information from the event.
        String eventKey = event.getEvent();
        String eventResource = event.getResource();
        String siteId = event.getContext();
        String userId = event.getUserId();
        //Get the userEid for some purposes like PA system.
        String eventUserEid = sakaiProxy.getUserEid(userId);

        //The event reference has this format "/content/xxxxxxxxxxxxx" where "/xxxxxxxxxxxxx" is the resourceId
        String resourceId = StringUtils.remove(eventResource, CONTENT_ENTITY_PREFIX);

        //Filter by attachment collection
        //Filter by site, site property or role
        if(filterByAttachmentCollection(resourceId) || filterBySiteOrRole(siteId, userId)) {
            return;
        }

        log.debug("The event contains a valid resourceId with id {}, getting the resource from the service.", resourceId);

        //Get the resource using the service, abort if the resource is not found
        ContentResource contentResource = sakaiProxy.getContentResource(resourceId);
        if(contentResource == null) {
            log.error("Unable to get the content resource with resourceId {}, aborting the process.", resourceId);
            return;
        }

        //If the resource is hidden the processor must ignore the file.
        if(contentResource.isHidden()) {
            log.debug("The resource {} is hidden and should not be processed, aborting the process.", resourceId);
            return;
        }

        //If the mimeType is not in the checker list, the processor must ignore the file.
        String contentType = contentResource.getContentType();
        if(!sakaiProxy.isCheckedMimeType(contentType)) {
            log.debug("The mimetype {} of the resource {} is not in the list of checked resources, aborting the process.", contentType, resourceId);
            return;
        }

        //Get the most important information of the resource
        String propCreator;
        String propDisplayName;
        Date creationDate;
        Date modifiedDate;
        try {
            propCreator = contentResource.getProperties().getProperty(ResourceProperties.PROP_CREATOR);
            propDisplayName = contentResource.getProperties().getProperty(ResourceProperties.PROP_DISPLAY_NAME);
            creationDate = contentResource.getProperties().getDateProperty(ResourceProperties.PROP_CREATION_DATE);
            modifiedDate = contentResource.getProperties().getDateProperty(ResourceProperties.PROP_MODIFIED_DATE);
        } catch (EntityPropertyNotDefinedException | EntityPropertyTypeException e) {
            log.error("Error getting the properties of the file {}, aborting the process. {}", resourceId, e);
            return;
        }

        log.debug("Processing the resource {} with attributes, propCreator={} contentType={} propDisplayName={}.", resourceId, propCreator, contentType, propDisplayName);

        IntellectualPropertyFile resourceIntellectualPropertyFile;
        resourceIntellectualPropertyFile = copyrightCheckerService.findIntellectualPropertyFileByFileId(resourceId);

        boolean statusCopied = false;

        //If the file exists, update the object with the status
        if(resourceIntellectualPropertyFile == null) {
            log.debug("The IP File for the resource id {} doesn't exist, creating it.", resourceId);
            resourceIntellectualPropertyFile = new IntellectualPropertyFile();
            resourceIntellectualPropertyFile.setFileId(resourceId);
            resourceIntellectualPropertyFile.setCreated(creationDate);
            resourceIntellectualPropertyFile.setContext(siteId);
            resourceIntellectualPropertyFile.setTitle(propDisplayName);
            resourceIntellectualPropertyFile.setState(IntellectualPropertyFileState.NONE);
            resourceIntellectualPropertyFile.setProperty(IntellectualPropertyFileProperty.NOT_PROCESSED);

            //Check a previous association, this allows us to detect duplicated and imported files.
            String associatedIpFileId = sakaiProxy.getContentResourceProperty(resourceId, SakaiProxy.CONTENT_RESOURCE_ASSOC_IP_FILE_PROP);
            if(StringUtils.isNotEmpty(associatedIpFileId)) {
                log.debug("There is an associated ip file with id {}, copying the properties to {}.", associatedIpFileId, resourceId);
                IntellectualPropertyFile associatedIpFile = copyrightCheckerService.findIntellectualPropertyFileById(Long.valueOf(associatedIpFileId));
                //Copy the value and the status
                if(associatedIpFile!=null && !resourceId.equals(associatedIpFile.getFileId())) {
                    resourceIntellectualPropertyFile.setAuthor(associatedIpFile.getAuthor());
                    resourceIntellectualPropertyFile.setComments(associatedIpFile.getComments());
                    resourceIntellectualPropertyFile.setDenyReason(associatedIpFile.getDenyReason());
                    resourceIntellectualPropertyFile.setIdentification(associatedIpFile.getIdentification());
                    resourceIntellectualPropertyFile.setPerpetual(associatedIpFile.getPerpetual());
                    resourceIntellectualPropertyFile.setPages(associatedIpFile.getPages());
                    resourceIntellectualPropertyFile.setType(associatedIpFile.getType());
                    resourceIntellectualPropertyFile.setLicense(associatedIpFile.getLicense());
                    resourceIntellectualPropertyFile.setLicenseEndTime(associatedIpFile.getLicenseEndTime());
                    resourceIntellectualPropertyFile.setProperty(associatedIpFile.getProperty());
                    resourceIntellectualPropertyFile.setState(associatedIpFile.getState());
                    resourceIntellectualPropertyFile.setPublisher(associatedIpFile.getPublisher());
                    resourceIntellectualPropertyFile.setRightsEntity(associatedIpFile.getRightsEntity());
                    resourceIntellectualPropertyFile.setTotalPages(associatedIpFile.getTotalPages());
                    statusCopied=true;
                }
            }
        }

        //Update the resource
        resourceIntellectualPropertyFile.setUserId(propCreator);
        resourceIntellectualPropertyFile.setModified(modifiedDate);
        resourceIntellectualPropertyFile.setEnrollments(sakaiProxy.getSiteMemberCount(siteId));

        //Change the file status and the property when the file is a new version.
        if(!statusCopied && ContentHostingService.EVENT_RESOURCE_UPD_NEW_VERSION.equals(eventKey)) {
            resourceIntellectualPropertyFile.setState(IntellectualPropertyFileState.NONE);
            resourceIntellectualPropertyFile.setProperty(IntellectualPropertyFileProperty.NOT_PROCESSED);
        }

        log.debug("Updating the IP File {} for the resource id {}.", resourceIntellectualPropertyFile, resourceId);
        copyrightCheckerService.saveIntellectualPropertyFile(resourceIntellectualPropertyFile);

        //Save the association to control the duplicate, import, etc
        log.debug("Associating the IP File {} for the resource id {}.", resourceIntellectualPropertyFile.getId(), resourceId);
        resourceIntellectualPropertyFile = copyrightCheckerService.findIntellectualPropertyFileByFileId(resourceId);
        sakaiProxy.setContentResourceProperty(resourceId, SakaiProxy.CONTENT_RESOURCE_ASSOC_IP_FILE_PROP, String.valueOf(resourceIntellectualPropertyFile.getId()));

        // Set the popup for the user and send the email notification
        if(!statusCopied) {
            handleIpPopup(eventUserEid);
            handleEmailNotification(eventUserEid);
        }
    }

    private void handleRemovedIpFile(Event event) {
        //Get all the resource information from the event.
        String eventResource = event.getResource();
        String eventKey = event.getEvent();

        //The event reference has this format "/content/xxxxxxxxxxxxx" where "/xxxxxxxxxxxxx" is the resourceId
        String resourceId = StringUtils.remove(eventResource, CONTENT_ENTITY_PREFIX);

        log.debug("Detected {} event, setting the IP file status to deleted for the resource {}.", eventKey, resourceId);
        //The file must be processed, persist the resource in the checker tables
        IntellectualPropertyFile resourceIntellectualPropertyFile = copyrightCheckerService.findIntellectualPropertyFileByFileId(resourceId);
        if(resourceIntellectualPropertyFile != null) {
            resourceIntellectualPropertyFile.setState(IntellectualPropertyFileState.DELETED);
            copyrightCheckerService.saveIntellectualPropertyFile(resourceIntellectualPropertyFile);
        }
    }

    private boolean filterByAttachmentCollection(String resourceId) {
        //Filter by attachment collection, some collections should be excluded
        if(resourceId.startsWith(ATTACHMENT_COLLECTION_PREFIX)) {
            String[] resourceIdParts = resourceId.split("/");
            if(resourceIdParts.length > 3) {
                String attachmentCollection = resourceIdParts[3];
                String[] restrictedAttachmentCollections = sakaiProxy.getRestrictedAttachmentCollections();
                if(ArrayUtils.isNotEmpty(restrictedAttachmentCollections) && !ArrayUtils.contains(restrictedAttachmentCollections, attachmentCollection)) {
                    log.debug("The files of the collection {} is restricted and will not be processed, aborting the process.", attachmentCollection);
                    return true;
                }
            }
        }
        return false;
    }

    private boolean filterBySiteOrRole(String siteId, String userId) {
        //Check if the site is a workspace or it's a special site.
        if(sakaiProxy.isSpecialSite(siteId) || sakaiProxy.isSiteWorkspace(siteId)) {
            log.debug("The checker is disabled for special sites and workspaces, aborting the process.", siteId);
            return true;
        }
        // Check if the processor is disabled for that site
        if(sakaiProxy.isCheckerDisabled(siteId)) {
            log.debug("The checker has been disabled for the site {} by property, aborting the process.", siteId);
            return true;
        }
        // Check if the files of that role should be processed or not.
        if(!sakaiProxy.isRoleChecked(userId, siteId)) {
            log.debug("The files of the user {} for the site {} are not included in the checker list by role, aborting the process.", userId, siteId);
            return true;
        }
        return false;
    }

    private void handleIpPopup(String eventUserEid) {
        if(sakaiProxy.popupEnabled()) {
            if(sakaiProxy.existsIpPopup(eventUserEid)) {
                try {
                    //Remove the existing pa system popup for that user.
                    sakaiProxy.removeIpPopup(eventUserEid);
                } catch(Exception ex) {
                    log.error("Error removing the IP popup of the user {}.", eventUserEid, ex);
                }
            }

            //Create a PA system popup for that user
            try{
                sakaiProxy.createIpPopup(eventUserEid);
            } catch(Exception ex) {
                log.error("Error creating the IP popup of the user {}.", eventUserEid, ex);
            }
        }
    }

    private void handleEmailNotification(String eventUserEid) {
        if(sakaiProxy.emailEnabled()) {
            //Send email notification to the user.
            try {
                sakaiProxy.sendNotificationEmail(eventUserEid);
            } catch (Exception ex) {
                log.error("Error sending the notification email to the user {}.", eventUserEid, ex);
            }
        }
    }

    public void init() {
        this.eventProcessorLogic.registerEventProcessor(ContentHostingService.EVENT_RESOURCE_UPD_NEW_VERSION, this);
        this.eventProcessorLogic.registerEventProcessor(ContentHostingService.EVENT_RESOURCE_UPD_VISIBILITY, this);
        this.eventProcessorLogic.registerEventProcessor(ContentHostingService.EVENT_RESOURCE_REMOVE, this);
    }

    @Override
    public String getEventIdentifer() {
        return null;
    }
}
