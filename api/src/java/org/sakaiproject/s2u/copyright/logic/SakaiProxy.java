package org.sakaiproject.s2u.copyright.logic;

import org.sakaiproject.content.api.ContentResource;
import org.sakaiproject.site.api.Site;
import org.sakaiproject.tool.api.Session;
import org.sakaiproject.user.api.User;

/**
 * An interface to abstract all Sakai related API calls in a central method that can be injected into our app.
 */
public interface SakaiProxy {

    public static final String CONFIG_EVENTPROCESSING_DISABLE = "copyright.checker.eventprocessing.disable";
    public static final String CONFIG_EVENTPROCESSING_LISTENERS = "copyright.checker.eventprocessing.listeners";
    public static final String CONFIG_FILE_DURATION = "copyright.checker.file.duration";
    public static final String CONFIG_SUPPORT_ENABLE = "copyright.checker.support.enable";
    public static final String CONFIG_SUPPORT_CONTACTS = "copyright.checker.support.contacts";
    public static final String CONFIG_CHECKER_ROLES = "copyright.checker.roles";
    public static final String CONFIG_CHECKER_ATTCH_COLLECTIONS = "copyright.checker.attachment.collections";
    public static final String CONFIG_POPUP_ENABLE = "copyright.checker.popup.enable";
    public static final String CONFIG_EMAIL_ENABLE = "copyright.checker.email.enable";
    public static final String CONFIG_CHECKED_MIMETYPES = "copyright.checker.checked.mimetypes";
    public static final String CONFIG_SITE_CHECKER_DISABLE = "copyright.checker.disable";
    public static final int DEFAULT_FILE_DURATION = 48;
    public static final String CONTENT_RESOURCE_ASSOC_IP_FILE_PROP = "cc-associated-ip-file";
    public static final String CONTENT_RESOURCE_NEW_IP_FILE_PROP = "cc-is-new-file";

    /**
     * Is the current user a superUser? (anyone in admin realm)
     * @return
     */
    public boolean isSuperUser();

    /**
     * Converts the current session in an admin session
     */
    public void startAdminSession();

    /**
     * Invalidates the current session
     */
    public void invalidateCurrentSession();

    /**
     * Post an event to Sakai
     * @param event         name of event
     * @param reference     reference
     * @param modify        true if something changed, false if just access
     */
    public void postEvent(String event,String reference,boolean modify);

    /**
     * Get a configuration parameter as a boolean
     * @param param the param name
     * @param dflt the default value if the param is not set
     * @return
     */
    public boolean getConfigParam(String param, boolean dflt);

    /**
     * Get a configuration parameter as a String
     * @param param the param name
     * @param dflt the default value if the param is not set
     * @return
     */
    public String getConfigParam(String param, String dflt);

    /**
     * Get a configuration parameter as a int
     * @param param the param name
     * @param dflt the default value if the param is not set
     * @return
     */
    public int getConfigParam(String param, int dflt);

    /**
     * Gets the current UserId as String
     * @return the current user id
     */
    public String getCurrentUserId();

    /**
     * Get a user by its user id
     * @param userId the id of the user
     * @return the user
     */
    public User getUserById(String userId);

    /**
     * Gets the UserEid as String
     * @param userId the internal user id
     * @return the current user eid
     */
    public String getUserEid(String userId);

    /**
     * Gets the user Display Name
     * @param userId
     * @return the current user name
     */
    public String getUserName(String userId);

    /**
     * Gets the current user email
     * @return the current user email
     */
    public String getCurrentUserEmail();

    /**
     * Get a site by its id
     * @param siteId the site id
     * @return the site
     */
    public Site getSite(String siteId);

    /**
     * Gets the members count of the site
     * @param siteId the site id
     * @return the current site member count
     */
    public int getSiteMemberCount(String siteId);

    /**
     * Returns the list of the attachment collections that should be processed.
     * @return a list of the attachment collections
     */
    public String[] getRestrictedAttachmentCollections();

    /**
     * Returns true if the site is a workspace
     * @param siteId the site id
     * @return if the site is a workspace
     */
    public boolean isSiteWorkspace(String siteId);

    /**
     * Returns true if the site is special
     * @param siteId the site id
     * @return if the site is special
     */
    public boolean isSpecialSite(String siteId);

    /**
     * Returns true if the checker is disabled for that site
     * @param siteId the site id
     * @return if the checker is disabled for that site
     */
    public boolean isCheckerDisabled(String siteId);

    /**
     * Returns true if the role of the user in that site must be processed by the checker.
     * @param siteId the site id
     * @param userId the user id
     * @return if the role of the user in that site must be processed by the checker
     */
    public boolean isRoleChecked(String userId, String siteId);

    /**
     * Returns true if the PA System popup integration is enabled in sakai.properties
     * @return if the PA System is enabled
     */
    public boolean popupEnabled();

    /**
     * Returns true if the user has an active IP popup
     * @param userEid the external user id
     * @return
     */
    public boolean existsIpPopup(String userEid);

    /**
     * Creates a new IP popup for the user
     * @param userEid the external user id
     * @param hiddenFiles notify if there are hidden files, the message should inform that case.
     * @throws java.lang.Exception if there is a problem to create the popup
     */
    public void createIpPopup(String userEid, boolean hiddenFiles) throws Exception;

    /**
     * Removes the IP popup for the user
     * @param userEid the external user id
     * @throws java.lang.Exception if there is a problem to remove the popup
     */
    public void removeIpPopup(String userEid) throws Exception;

    /**
     * Returns true if the EmailService integration is enabled in sakai.properties
     * @return if the EmailService integration is enabled
     */
    public boolean emailEnabled();

    /**
     * Sends a IP notification email to the user
     * @param userEid the external user id
     * @throws java.lang.Exception if the email could not be sent
     */
    public void sendNotificationEmail(String userEid) throws Exception;

    /**
     * Sends an IP query email to the support contacts
     * @param userEid the external user id.
     * @param userContent content of the email
     * @param fileId File id
     * @param siteId Site id
     * @throws java.lang.Exception if the email could not be sent
     */
    public void sendSupportEmail(String userEid, String userContent, String fileId, String siteId) throws Exception;

    /**
     * Gets a content resource using the resourceId
     * @param resourceId The resource identifier
     * @return a content resource
     */
    public ContentResource getContentResource(String resourceId);

    /**
     * Returns true if the mimetype is in the list of the copyright checker mimetypes
     * @param fileMimeType The resource mime type.
     * @return if the mimetype is in the list of the copyright checker mimetypes
     */
    public boolean isCheckedMimeType(String fileMimeType);

    /**
     * Get the file DisplayName
     * @param id
     * @return file name
     */
    public String getFileName(String id);

    /**
     * Gets the file URL
     * @param id
     * @return file URL
     */
    public String getFileUrl(String id);

    /**
     * Get the file context name, wich means the site Title
     * @param id
     * @return file contect name
     */
    public String getFileContextName(String id);

    /**
     * Check if a file is hidden
     * @param id
     * @return if the file is hidden
     */
    public boolean fileIsHidden(String id);

    /**
     * Gets the content of a file converted to string
     * @param id
     * @return if the file is hidden
     */
    public String getFileContentAsString(String id);

    /**
     * Set the content resource visibility by resourceId
     * @param resourceId the resource id to modify its visibility
     * @param visible true if the visibility of the resource should be visible
     * @return return true if sucess, false otherwise
     */
    public boolean setContentResourceVisibility(String resourceId, boolean visible);

    /**
     * Set a content resource property
     * @param resourceId the resource id set a property
     * @param propertyKey property key to modify
     * @param value the new value for that property
     */
    public void setContentResourceProperty(String resourceId, String propertyKey, String value);

    /**
     * Get a session by its id
     * @param sessionId the session id
     * @return the session
     */
    public Session getSession(String sessionId);

    /**
     * Set a session
     * @param session the session
     */
    public void setCurrentSession(Session session);

    /**
     * Is support button enabled
     * @return if the support help buttons are enabled
     */
    public boolean isSupportEnabled();
}
