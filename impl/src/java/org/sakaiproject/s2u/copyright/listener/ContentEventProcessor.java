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

        //Get the userEid associated to the event
        String eventUserEid = sakaiProxy.getUserEid(userId);
        
        ContentResource contentResource = null;
        if(!ContentHostingService.EVENT_RESOURCE_REMOVE.equals(eventKey)) {
            contentResource = validateContentResourceEvent(eventResource, siteId, userId);

            if(contentResource == null) {
                //validateContentResourceEvent has enough error logging to figure out the problem.
                return;
            }
        }

        switch(eventKey) {
            case ContentHostingService.EVENT_RESOURCE_ADD:
                handleNewIpFile(contentResource, siteId, eventUserEid);
                break;
            case ContentHostingService.EVENT_RESOURCE_UPD_NEW_VERSION:
                handleIpFileNewVersion(contentResource, siteId, eventUserEid);
                break;
            case ContentHostingService.EVENT_RESOURCE_UPD_VISIBILITY:
                handleIpFileVisibility(contentResource, siteId, eventUserEid);
                break;
            case ContentHostingService.EVENT_RESOURCE_REMOVE:
                handleRemovedIpFile(eventResource);
                break;
            default:
                log.error("Event {} not supported.", eventKey);
                break;
        }
        log.debug("The event {} has been processed successfully.", event);
    }

    private void handleIpFileVisibility(ContentResource contentResource, String siteId, String eventUserEid) {
        String resourceId = contentResource.getId();
        ResourceProperties contentResourceProperties = contentResource.getProperties();
        String associatedIpFileId = contentResourceProperties.getProperty(SakaiProxy.CONTENT_RESOURCE_ASSOC_IP_FILE_PROP);

        if(StringUtils.isEmpty(associatedIpFileId)) {
            //The file was created hidden but now is visible.
            createNewIpFile(contentResource, siteId);
            // Set the popup for the user and send the email notification
            handleIpPopup(eventUserEid);
            handleEmailNotification(eventUserEid);
        }else {
            //The file exists and it's visible, hide it if the status is DENIED.
            IntellectualPropertyFile existingIpFile = copyrightCheckerService.findIntellectualPropertyFileByFileId(resourceId);
            if(existingIpFile != null && existingIpFile.getState().equals(new Integer(IntellectualPropertyFileState.DENIED))) {
                log.debug("The resource with id {} has a denied status, hiding it.", resourceId);
                sakaiProxy.setContentResourceVisibility(resourceId, false);
            }else {
                log.error("Fatal, cannot get IP file with resourceId {}.", resourceId);
            }
        }
    }
    
    private void handleNewIpFile(ContentResource contentResource, String siteId, String eventUserEid) {
        String resourceId = contentResource.getId();
        ResourceProperties contentResourceProperties = contentResource.getProperties();

        //Get the associatedIpFileId
        String associatedIpFileId = contentResourceProperties.getProperty(SakaiProxy.CONTENT_RESOURCE_ASSOC_IP_FILE_PROP);
        
        //Create a new ip file
        IntellectualPropertyFile newIpFile = createNewIpFile(contentResource, siteId);

        if(StringUtils.isEmpty(associatedIpFileId)) {
            // Set the popup for the user and send the email notification
            handleIpPopup(eventUserEid);
            handleEmailNotification(eventUserEid);
        }else {
            //File is moved, duplicated or imported.
            IntellectualPropertyFile associatedIpFile = copyrightCheckerService.findIntellectualPropertyFileById(Long.valueOf(associatedIpFileId));

            if(associatedIpFile != null) {
                //Copy all the properties from the associated file
                newIpFile.setAuthor(associatedIpFile.getAuthor());
                newIpFile.setComments(associatedIpFile.getComments());
                newIpFile.setDenyReason(associatedIpFile.getDenyReason());
                newIpFile.setIdentification(associatedIpFile.getIdentification());
                newIpFile.setPerpetual(associatedIpFile.getPerpetual());
                newIpFile.setPages(associatedIpFile.getPages());
                newIpFile.setType(associatedIpFile.getType());
                newIpFile.setLicense(associatedIpFile.getLicense());
                newIpFile.setLicenseEndTime(associatedIpFile.getLicenseEndTime());
                newIpFile.setProperty(associatedIpFile.getProperty());
                if(!associatedIpFile.getState().equals(new Integer(IntellectualPropertyFileState.DELETED))) {
                    newIpFile.setState(associatedIpFile.getState());
                }
                newIpFile.setPublisher(associatedIpFile.getPublisher());
                newIpFile.setRightsEntity(associatedIpFile.getRightsEntity());
                newIpFile.setTotalPages(associatedIpFile.getTotalPages());
                copyrightCheckerService.saveIntellectualPropertyFile(newIpFile);
            } else {
                log.error("Fatal error copying IP attributes, the associated ip file with id {} not found.", associatedIpFileId);
            }
        }

        //Notify the content resource that a new IP file has been created
        sakaiProxy.setContentResourceProperty(resourceId, SakaiProxy.CONTENT_RESOURCE_NEW_IP_FILE_PROP, "true");
    }

    private void handleIpFileNewVersion(ContentResource contentResource, String siteId, String userEid) {
        String resourceId = contentResource.getId();
        ResourceProperties contentResourceProperties = contentResource.getProperties();
        String isNewIpFile = contentResourceProperties.getProperty(SakaiProxy.CONTENT_RESOURCE_NEW_IP_FILE_PROP);
        
        if(StringUtils.isEmpty(isNewIpFile)) {
            //A new version of the file has been uploaded, reset the status to not processed.
            IntellectualPropertyFile existingIpFile = copyrightCheckerService.findIntellectualPropertyFileByFileId(resourceId);
            if(existingIpFile !=null) {
                existingIpFile.setState(IntellectualPropertyFileState.NONE);
                existingIpFile.setProperty(IntellectualPropertyFileProperty.NOT_PROCESSED);
                copyrightCheckerService.saveIntellectualPropertyFile(existingIpFile);
                // Set the popup for the user and send the email notification
                handleIpPopup(userEid);
                handleEmailNotification(userEid);
            }
        }else {
            //Remove the notification in the resource
            sakaiProxy.setContentResourceProperty(resourceId, SakaiProxy.CONTENT_RESOURCE_NEW_IP_FILE_PROP, null);
        }
    }

    private void handleRemovedIpFile(String eventResource) {
        //The event reference has this format "/content/xxxxxxxxxxxxx" where "/xxxxxxxxxxxxx" is the resourceId
        String resourceId = StringUtils.remove(eventResource, CONTENT_ENTITY_PREFIX);
        log.debug("Delete event detected, setting the IP file status to deleted for the resource {}.", resourceId);
        IntellectualPropertyFile ipFiletoDelete = copyrightCheckerService.findIntellectualPropertyFileByFileId(resourceId);
        if(ipFiletoDelete != null) {
            //Logic deletion instead of physical deletion
            ipFiletoDelete.setFileId("DELETED-"+ipFiletoDelete.getFileId());
            ipFiletoDelete.setState(IntellectualPropertyFileState.DELETED);
            copyrightCheckerService.saveIntellectualPropertyFile(ipFiletoDelete);
        }
    }
    
    private ContentResource validateContentResourceEvent(String eventResource, String siteId, String userId) {
        //The event reference has this format "/content/xxxxxxxxxxxxx" where "/xxxxxxxxxxxxx" is the resourceId
        String resourceId = StringUtils.remove(eventResource, CONTENT_ENTITY_PREFIX);

        //Filter by attachment collection
        //Filter by site, site property or role
        if(filterByAttachmentCollection(resourceId) || filterBySiteOrRole(siteId, userId)) {
            return null;
        }

        log.debug("The event contains a valid resourceId with id {}, getting the resource from the service.", resourceId);
        
        //Get the resource using the service, abort if the resource is not found
        ContentResource contentResource = sakaiProxy.getContentResource(resourceId);
        if(contentResource == null) {
            log.error("Unable to get the content resource with resourceId {}, aborting the process.", resourceId);
            return null;
        }
        
        //If the resource is hidden the processor must ignore the file.
        if(contentResource.isHidden()) {
            log.debug("The resource {} is hidden and should not be processed, aborting the process.", resourceId);
            return null;
        }
        
        //If the mimeType is not in the checker list, the processor must ignore the file.
        String contentType = contentResource.getContentType();
        if(!sakaiProxy.isCheckedMimeType(contentType)) {
            log.debug("The mimetype {} of the resource {} is not in the list of checked resources, aborting the process.", contentType, resourceId);
            return null;
        }

        //Return the contentResource object if everything is fine
        return contentResource;
    }

    private IntellectualPropertyFile createNewIpFile(ContentResource contentResource, String siteId){
        String resourceId = contentResource.getId();
        ResourceProperties contentResourceProperties = contentResource.getProperties();

        //Get the most important information of the resource
        String propCreator = contentResourceProperties.getProperty(ResourceProperties.PROP_CREATOR);
        String propDisplayName = contentResourceProperties.getProperty(ResourceProperties.PROP_DISPLAY_NAME);
        Date creationDate = null;
        Date modifiedDate = null;
        try {
            creationDate = contentResourceProperties.getDateProperty(ResourceProperties.PROP_CREATION_DATE);
            modifiedDate = contentResourceProperties.getDateProperty(ResourceProperties.PROP_MODIFIED_DATE);
        } catch (EntityPropertyNotDefinedException | EntityPropertyTypeException e) {
            log.error("Error getting the creation and modified dates of the resourceId {}.", resourceId);
            return null;
        }

        IntellectualPropertyFile newIpFile = new IntellectualPropertyFile();
        newIpFile = new IntellectualPropertyFile();
        newIpFile.setFileId(resourceId);
        newIpFile.setCreated(creationDate);
        newIpFile.setContext(siteId);
        newIpFile.setTitle(propDisplayName);
        newIpFile.setState(IntellectualPropertyFileState.NONE);
        newIpFile.setProperty(IntellectualPropertyFileProperty.NOT_PROCESSED);
        newIpFile.setUserId(propCreator);
        newIpFile.setModified(modifiedDate);
        newIpFile.setEnrollments(sakaiProxy.getSiteMemberCount(siteId));

        log.debug("Saving the new the IP File {} for the resource id {}.", newIpFile, resourceId);
        copyrightCheckerService.saveIntellectualPropertyFile(newIpFile);

        //Save the association to control the copy, move, duplicate, import, etc
        log.debug("Associating the IP File {} for the resource id {}.", newIpFile.getId(), resourceId);
        newIpFile = copyrightCheckerService.findIntellectualPropertyFileByFileId(resourceId);
        sakaiProxy.setContentResourceProperty(resourceId, SakaiProxy.CONTENT_RESOURCE_ASSOC_IP_FILE_PROP, String.valueOf(newIpFile.getId()));
        return newIpFile;
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

    private void handleIpPopup(String userEid) {
        if(sakaiProxy.popupEnabled()) {
            if(sakaiProxy.existsIpPopup(userEid)) {
                try {
                    //Remove the existing pa system popup for that user.
                    sakaiProxy.removeIpPopup(userEid);
                } catch(Exception ex) {
                    log.error("Error removing the IP popup of the user {}.", userEid, ex);
                }
            }

            //Create a PA system popup for that user
            try{
                sakaiProxy.createIpPopup(userEid, false);
            } catch(Exception ex) {
                log.error("Error creating the IP popup of the user {}.", userEid, ex);
            }
        }
    }

    private void handleEmailNotification(String userEid) {
        if(sakaiProxy.emailEnabled()) {
            //Send email notification to the user.
            try {
                sakaiProxy.sendNotificationEmail(userEid);
            } catch (Exception ex) {
                log.error("Error sending the notification email to the user {}.", userEid, ex);
            }
        }
    }

    public void init() {
        this.eventProcessorLogic.registerEventProcessor(ContentHostingService.EVENT_RESOURCE_ADD, this);
        this.eventProcessorLogic.registerEventProcessor(ContentHostingService.EVENT_RESOURCE_UPD_NEW_VERSION, this);
        this.eventProcessorLogic.registerEventProcessor(ContentHostingService.EVENT_RESOURCE_UPD_VISIBILITY, this);
        this.eventProcessorLogic.registerEventProcessor(ContentHostingService.EVENT_RESOURCE_REMOVE, this);
    }

    @Override
    public String getEventIdentifer() {
        return null;
    }
}
