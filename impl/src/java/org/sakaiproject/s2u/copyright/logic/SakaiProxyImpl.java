package org.sakaiproject.s2u.copyright.logic;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Observer;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.mail.internet.InternetAddress;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.sakaiproject.antivirus.api.VirusFoundException;

import org.sakaiproject.authz.api.Member;
import org.sakaiproject.authz.api.SecurityAdvisor;
import org.sakaiproject.authz.api.SecurityService;
import org.sakaiproject.component.api.ServerConfigurationService;
import org.sakaiproject.content.api.ContentHostingService;
import org.sakaiproject.content.api.ContentResource;
import org.sakaiproject.content.api.ContentResourceEdit;
import org.sakaiproject.email.api.EmailService;
import org.sakaiproject.entity.api.EntityPropertyNotDefinedException;
import org.sakaiproject.entity.api.EntityPropertyTypeException;
import org.sakaiproject.entity.api.ResourceProperties;
import org.sakaiproject.event.api.EventTrackingService;
import org.sakaiproject.exception.IdUnusedException;
import org.sakaiproject.exception.InUseException;
import org.sakaiproject.exception.OverQuotaException;
import org.sakaiproject.exception.PermissionException;
import org.sakaiproject.exception.ServerOverloadException;
import org.sakaiproject.exception.TypeException;
import org.sakaiproject.pasystem.api.MissingUuidException;
import org.sakaiproject.pasystem.api.PASystem;
import org.sakaiproject.pasystem.api.Popup;
import org.sakaiproject.pasystem.api.Popups;
import org.sakaiproject.pasystem.api.TemplateStream;
import org.sakaiproject.s2u.copyright.logic.listener.EventProcessor;
import org.sakaiproject.site.api.Site;
import org.sakaiproject.site.api.SiteService;
import org.sakaiproject.thread_local.api.ThreadLocalManager;
import org.sakaiproject.tool.api.Session;
import org.sakaiproject.tool.api.SessionManager;
import org.sakaiproject.user.api.User;
import org.sakaiproject.user.api.UserDirectoryService;
import org.sakaiproject.user.api.UserNotDefinedException;
import org.sakaiproject.util.ResourceLoader;

/**
 * Implementation of our SakaiProxy API
 *
 */
@Slf4j
public class SakaiProxyImpl implements SakaiProxy {

    public static final int CONSTANT_DAY_IN_MILLIS = 24 * 3600 * 1000;
    public static final String TEMPLATE_IP_POPUP_PREFIX = "IP-%s";
    public static final String TEMPLATE_IP_POPUP_CONTENT = "<header class=\"popup-container-header\">%s</header><section><p>%s</p></section>";

    /************************************************************************
     * Spring-injected classes
     ************************************************************************/
    @Setter private Cache cache;
    @Setter private ContentHostingService contentHostingService;
    @Setter private EmailService emailService;
    @Setter private EventTrackingService eventTrackingService;
    @Setter private SessionManager sessionManager;
    @Setter private SecurityService securityService;
    @Setter private ServerConfigurationService serverConfigurationService;
    @Setter private SiteService siteService;
    @Setter protected ThreadLocalManager threadLocalManager;
    @Setter protected PASystem PASystem;
    @Setter protected UserDirectoryService userDirectoryService;

    public void addLocalEventListener(Observer observer) {
        this.eventTrackingService.addLocalObserver(observer);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean getConfigParam(String param, boolean dflt) {
        return serverConfigurationService.getBoolean(param, dflt);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getConfigParam(String param, String dflt) {
        return serverConfigurationService.getString(param, dflt);
    }

    /**
     * Get config multiple params
     * @param param parameter
     * @return array of values
     */
    public String[] getConfigParams(String param) {
        return serverConfigurationService.getStrings(param);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getConfigParam(String param, int dflt) {
        return serverConfigurationService.getInt(param, dflt);
    }

    public String getServerId() {
        return serverConfigurationService.getServerId();
    }

    public String getPortalUrl(){
        return serverConfigurationService.getPortalUrl();
    }

    public void pushSecurityAdvisor(SecurityAdvisor securityAdvisor) {
        this.securityService.pushAdvisor(securityAdvisor);
    }

    public void popSecurityAdvisor(SecurityAdvisor securityAdvisor) {
        this.securityService.popAdvisor(securityAdvisor);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void startAdminSession() {
        log.debug("Creating session: EventProcessor");
        Session session = this.sessionManager.startSession("EventProcessor");
        session.setUserId("admin");
        session.setUserEid("admin");
        this.sessionManager.setCurrentSession(session);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void invalidateCurrentSession(){
        Session session = sessionManager.getCurrentSession();
        session.invalidate();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isSuperUser() {
        return securityService.isSuperUser();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getCurrentUserId() {
        return sessionManager.getCurrentSessionUserId();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getCurrentUserEmail(){
        return getUserEmail(getUserEid(getCurrentUserId()));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public User getUserById(String userId) {
        try {
            return userDirectoryService.getUser(userId);
        } catch (UserNotDefinedException e) {
            return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getUserEid(String userId) {
        try {
            return userDirectoryService.getUserEid(userId);
        } catch (UserNotDefinedException e) {
            return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getUserName(String userId) {
        try {
            return userDirectoryService.getUser(userId).getDisplayName();
        } catch (UserNotDefinedException e) {
            return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Site getSite(String siteId){
        try {
            return siteService.getSite(siteId);
        } catch (IdUnusedException e) {
            log.error("Error getting the site members {}.", e);
            return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getSiteMemberCount(String siteId){
        try {
            return siteService.getSite(siteId).getMembers().size();
        } catch (IdUnusedException e) {
            log.error("Error getting the site members {}.", e);
            return 0;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String[] getRestrictedAttachmentCollections() {
        return this.getConfigParams(CONFIG_CHECKER_ATTCH_COLLECTIONS);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isSiteWorkspace(String siteId) {
        return siteService.isUserSite(siteId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isSpecialSite(String siteId) {
        return siteService.isSpecialSite(siteId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isCheckerDisabled(String siteId) {
        try {
            return siteService.getSite(siteId).getProperties().getBooleanProperty(CONFIG_SITE_CHECKER_DISABLE);
        } catch (EntityPropertyNotDefinedException | EntityPropertyTypeException | IdUnusedException e) {
            return false;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isRoleChecked(String userId, String siteId) {
        //Get the checker roles from Sakai properties
        String[] checkerRoles = this.getConfigParams(CONFIG_CHECKER_ROLES);

        if(ArrayUtils.isEmpty(checkerRoles)) {
            log.error("Please configure the property {} to define the checker roles.", CONFIG_CHECKER_ROLES);
            return false;
        }
        try {
            Member member = siteService.getSite(siteId).getMember(userId);
            return member != null ? ArrayUtils.contains(checkerRoles, member.getRole().getId()) : false;
        } catch (IdUnusedException e) {
            return false;
        }
    }

    private String getUserEmail(String userEid) {
        try {
            return userDirectoryService.getUserByEid(userEid).getEmail();
        } catch (UserNotDefinedException e) {
            return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void postEvent(String event, String reference, boolean modify) {
        eventTrackingService.post(eventTrackingService.newEvent(event,reference,modify));
    }

    /************************************************************************
     * EVENT PROCESSOR METHODS
     ************************************************************************/

    /**
     * Remove from the thread-local cache all items bound to the current thread.
     */
    public void clearThreadLocalCache() {
        this.threadLocalManager.clear();
    }

    /**
     * check with the server configuration whether the event process thread should be disabled or not
     * @return
     */
    public boolean isEventProcessingThreadDisabled() {
        return serverConfigurationService.getBoolean(CONFIG_EVENTPROCESSING_DISABLE, false);
    }

    public boolean isEventProcessingListenerEnabled(EventProcessor listener) {
        String listeners = serverConfigurationService.getString(CONFIG_EVENTPROCESSING_LISTENERS, "*");
        if("*".equals(listeners)) {
            return true;
        }
        if(StringUtils.isNotBlank(listeners)) {
            List<String> listenersList = new ArrayList(Arrays.asList(listeners.split(",")));
            return listenersList.contains(listener.getClass().getSimpleName());
        }
        return false;
    }

    /************************************************************************
     * PA SYSTEM INTEGRATION
     ************************************************************************/
    /**
     * {@inheritDoc}
     */
    @Override
    public boolean existsIpPopup(String userEid) {
        //If the popupId is not empty for the userEid, exists a IP popup
        String popupId = this.getIpPopupId(userEid);
        boolean existsPopup = StringUtils.isNotEmpty(popupId);
        log.debug("For {}: {} with uuid {}", userEid, existsPopup, popupId);
        return existsPopup;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void createIpPopup(String userEid, boolean hiddenFiles) throws Exception{
        //Popup assigned to the userEid
        List<String> assignToUsers = Arrays.asList(userEid);

        //Prefix to identify the IP popup, format IP-userEid
        String ipPopupPrefix = String.format(TEMPLATE_IP_POPUP_PREFIX, userEid);

        //TTL of the popup, 2 days by default.
        int popupDuration = this.getConfigParam(CONFIG_FILE_DURATION, DEFAULT_FILE_DURATION);
        long startTime = System.currentTimeMillis();
        long endTime = System.currentTimeMillis() + CONSTANT_DAY_IN_MILLIS * popupDuration;
        log.debug("Creating a new IP popup, user {}, startLocalDateTime {}, endLocalDateTime {} ", userEid , Instant.ofEpochMilli(startTime), Instant.ofEpochMilli(endTime));

        //Create the Popup object
        Popup popup = Popup.create(ipPopupPrefix, startTime, endTime, false);

        //Create the TemplateStream object that contains the message template.
        ResourceLoader resourceLoader = new ResourceLoader("messages");
        resourceLoader.setContextLocale(resourceLoader.getLocale(userDirectoryService.getUserId(userEid)));
        String popupTitle = resourceLoader.getString("ip.popup.title");
        String popupContent = resourceLoader.getFormattedMessage(hiddenFiles ? "ip.popup.content.hidden" : "ip.popup.content", String.valueOf(popupDuration));
        String popupTemplateContent = String.format(TEMPLATE_IP_POPUP_CONTENT, popupTitle, popupContent);
        InputStream targetStream = new ByteArrayInputStream(popupTemplateContent.getBytes());
        TemplateStream templateStream = new TemplateStream(targetStream, popupTemplateContent.length());

        //Persist the Popup and the TemplateStream objects using the service
        Popups popupService = PASystem.getPopups();
        String popupId = popupService.createCampaign(popup, templateStream, Optional.of((assignToUsers)));
        log.debug("Popup created successfully.");

        //Adding the popupId to the cache to perform optimal searches
        if(popupId != null) {
            log.debug("Adding item to cache for: " + popupId);
            cache.put(new Element(ipPopupPrefix, popupId));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeIpPopup(String userEid) throws Exception {
        String popupId = this.getIpPopupId(userEid);
        log.debug("Removing the IP popup {} of the user {}.", popupId, userEid);
        Popups popupService = PASystem.getPopups();
        popupService.deleteCampaign(popupId);
        String ipPopupPrefix = String.format(TEMPLATE_IP_POPUP_PREFIX, userEid);
        cache.remove(ipPopupPrefix);
        log.debug("IP popup {} removed successfully.", popupId);
    }

    private String getIpPopupId(String userEid) {
        String popupId = null;
        String ipPopupPrefix = String.format(TEMPLATE_IP_POPUP_PREFIX, userEid);

        //check cache
        Element element = cache.get(ipPopupPrefix);
        if(element != null) {
            log.debug("Fetching item from cache for: " + ipPopupPrefix);
            return (String) element.getValue();
        }

        Popups popupService = PASystem.getPopups();
        for(Popup popup : popupService.getAll()) {
            try {
                if(StringUtils.isNotEmpty(popup.getDescriptor()) && popup.getDescriptor().equals(ipPopupPrefix)){
                    popupId = popup.getUuid();
                }
            } catch (MissingUuidException e) {
                log.error("Missing popup uuid {}.", e);
            }
        }

        if(popupId != null) {
            log.debug("Adding item to cache for: " + popupId);
            cache.put(new Element(ipPopupPrefix, popupId));
        }

        return popupId;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean popupEnabled() {
        return this.getConfigParam(CONFIG_POPUP_ENABLE, false);
    }

    /************************************************************************
     * Email Service methods
     ************************************************************************/
    /**
     * {@inheritDoc}
     */
    @Override
    public void sendSupportEmail(String userEid, String userContent, String fileId, String siteId) throws Exception{
        //Get the support contacts from Sakai properties
        String[] supportContacts = this.getConfigParams(CONFIG_SUPPORT_CONTACTS);

        if(ArrayUtils.isEmpty(supportContacts)) {
            log.error("Please configure the property {} to send email notifications.", CONFIG_SUPPORT_CONTACTS);
            throw new Exception(CONFIG_SUPPORT_CONTACTS + " property is not configured.");
        }
        InternetAddress[] to = new InternetAddress[supportContacts.length];
        for(int i = 0; i < supportContacts.length; i++) {
            to[i] = new InternetAddress(supportContacts[i]);
        }

        //From address will use setup.request property as other tools
        InternetAddress from = new InternetAddress(this.getConfigParam("setup.request","no-reply@" + serverConfigurationService.getServerName()));

        User user = userDirectoryService.getUserByEid(userEid);

        //File URL should be something like http://localhost/access/content/group/xxxxxx
        String fileUrl = serverConfigurationService.getServerUrl() + this.getFileUrl(fileId);

        //Title of the notification
        ResourceLoader resourceLoader = new ResourceLoader("messages");
        resourceLoader.setContextLocale(resourceLoader.getLocale(user.getId()));
        String subject = resourceLoader.getFormattedMessage("ip.support.mail.subject", user.getEmail());
        String content = resourceLoader.getFormattedMessage("ip.support.mail.content", user.getDisplayName(), user.getEmail(), this.getFileContextName(siteId), this.getFileName(fileId), fileUrl, userContent);
        log.debug("EMAIL CONTENT: "+content);

        InternetAddress[] headerTo = null;
        InternetAddress[] replyTo = null;
        List<String> additionalHeaders = null;
        emailService.sendMail(from, to, subject, content, headerTo, replyTo, additionalHeaders);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void sendNotificationEmail(String userEid) throws Exception{
        //To address
        String toStr = this.getUserEmail(userEid);
        if(StringUtils.isEmpty(toStr)) {
            throw new Exception(String.format("The user %s needs a valid email address.", userEid));
        }

        //From address will use setup.request property as other tools
        String fromStr = this.getConfigParam("setup.request","no-reply@" + serverConfigurationService.getServerName());

        //Title of the notification
        ResourceLoader resourceLoader = new ResourceLoader("messages");
        resourceLoader.setContextLocale(resourceLoader.getLocale(userDirectoryService.getUserId(userEid)));
        String subject = resourceLoader.getString("ip.notification.mail.subject");
        //TTL of the popup, 2 days by default.
        int popupDuration = this.getConfigParam(CONFIG_FILE_DURATION, DEFAULT_FILE_DURATION);
        String content = resourceLoader.getFormattedMessage("ip.notification.mail.content", String.valueOf(popupDuration));

        List<String> additionalHeaders = null;
        String headerToStr = null;
        String replyToStr = null;
        emailService.send(fromStr, toStr, subject, content, headerToStr, replyToStr, additionalHeaders);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean emailEnabled() {
        return this.getConfigParam(CONFIG_EMAIL_ENABLE, false);
    }

    /************************************************************************
     * Content Hosting Service methods
     ************************************************************************/
    /**
     * {@inheritDoc}
     */
    @Override
    public ContentResource getContentResource(String resourceId) {
        try {
            if(!contentHostingService.isCollection(resourceId)) {
                return contentHostingService.getResource(resourceId);
            }
        } catch (PermissionException | IdUnusedException | TypeException e) {
            log.error("Error getting the resource {}.", resourceId, e);
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean setContentResourceVisibility(String resourceId, boolean visible) {
        ContentResourceEdit contentResourceEdit;
        try {
            contentResourceEdit = (ContentResourceEdit) contentHostingService.editResource(resourceId);
            contentResourceEdit.setAvailability(!visible, null, null);
            contentHostingService.commitResource(contentResourceEdit);
            return true;
        } catch (VirusFoundException | IdUnusedException | InUseException |
                OverQuotaException | PermissionException | ServerOverloadException |
                TypeException e) {
            log.error("Error editing the resource {}.", resourceId, e);
            return false;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setContentResourceProperty(String resourceId, String propertyKey, String value) {
        ContentResourceEdit contentResourceEdit;
        try {
            contentResourceEdit = (ContentResourceEdit) contentHostingService.editResource(resourceId);
            contentResourceEdit.getPropertiesEdit().addProperty(propertyKey, value);
            contentHostingService.commitResource(contentResourceEdit);
        } catch (VirusFoundException | IdUnusedException | InUseException |
                OverQuotaException | PermissionException | ServerOverloadException |
                TypeException e) {
            log.error("Error editing the resource {}.", resourceId, e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isCheckedMimeType(String fileMimeType) {
        String[] allowedMimeTypes = this.getConfigParams(CONFIG_CHECKED_MIMETYPES);
        return ArrayUtils.isNotEmpty(allowedMimeTypes) ? ArrayUtils.contains(allowedMimeTypes, fileMimeType) : false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getFileName(String id) {
        String ret = "";
        try {
            ret = contentHostingService.getProperties(id).getProperty(ResourceProperties.PROP_DISPLAY_NAME);
        } catch (IdUnusedException | PermissionException ex) {
            log.error("Error at getFileName(" + id + ") :" + ex);
        }
        return ret;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getFileUrl(String id){
        String ret = "";
        try {
            ret = serverConfigurationService.getServerUrl() + contentHostingService.getResource(id).getUrl(true);
        } catch (IdUnusedException | PermissionException | TypeException ex) {
            log.error("Error at getFileUrl() : "+ex.toString());
        }
        return ret;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getFileContextName(String id) {
        String ret = "";
        try {
            ret = siteService.getSite(id).getTitle();
        } catch (IdUnusedException ex) {
            log.error("Error at getFileName(" + id + ") :" + ex);
        }
        return ret;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getFileContentAsString(String id) {
        String ret = "";
        InputStream inputStream;
        try {
            inputStream =contentHostingService.getResource(id).streamContent();

            try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream, Charset.defaultCharset()))) {
                ret = br.lines().collect(Collectors.joining(System.lineSeparator()));
            }
        } catch (IOException | IdUnusedException | PermissionException | ServerOverloadException | TypeException e) {
            log.error("Error at getFileContentAsString(String id) : "+e);
        }
        return ret;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean fileIsHidden(String id) {
        boolean ret = false;
        try {
            ret = contentHostingService.getResource(id).isHidden();
        } catch (IdUnusedException | PermissionException | TypeException ex) {
            log.error("Error at getFileName(" + id + ") :" + ex);
        }
        return ret;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Session getSession(String sessionId) {
        if (StringUtils.isNotBlank(sessionId)) {
            return sessionManager.getSession(sessionId);
        } else {
            return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setCurrentSession(Session session) {
        sessionManager.setCurrentSession(session);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isSupportEnabled() {
        return this.getConfigParam(CONFIG_SUPPORT_ENABLE, false);
    }

    public void init() {
        log.info("init");
    }
}
