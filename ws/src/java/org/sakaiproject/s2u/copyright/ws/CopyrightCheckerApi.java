package org.sakaiproject.s2u.copyright.ws;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;
import java.util.List;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.StringUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.XML;

import org.sakaiproject.content.api.ContentResource;
import org.sakaiproject.entity.api.Entity;
import org.sakaiproject.entity.api.ResourceProperties;
import org.sakaiproject.exception.ServerOverloadException;
import org.sakaiproject.s2u.copyright.logic.CopyrightCheckerService;
import org.sakaiproject.s2u.copyright.logic.SakaiProxy;
import org.sakaiproject.s2u.copyright.model.IntellectualPropertyFile;
import org.sakaiproject.s2u.copyright.model.IntellectualPropertyFileProperty;
import org.sakaiproject.s2u.copyright.model.IntellectualPropertyFileState;
import org.sakaiproject.s2u.copyright.model.IntellectualPropertyFileStatus;
import org.sakaiproject.s2u.copyright.model.IntellectualPropertyFileType;
import org.sakaiproject.site.api.Site;
import org.sakaiproject.tool.api.Session;
import org.sakaiproject.user.api.User;
import org.sakaiproject.util.ResourceLoader;

@WebService
@SOAPBinding(style = SOAPBinding.Style.RPC, use = SOAPBinding.Use.LITERAL/*, parameterStyle = SOAPBinding.ParameterStyle.WRAPPED*/)
@Slf4j
public class CopyrightCheckerApi {

    private static final String PERMISSION_PROPERTY = "copyrightCheckerPermission";

    // Functions
    private static final String FUNCTION_GET_PENDING_FILES = "local_lpi_get_pending_files";
    private static final String FUNCTION_GET_FILE_META = "local_lpi_get_file_metadata";
    private static final String FUNCTION_SET_FILE_AUTHZ = "local_lpi_set_file_authorization";
    private static final String FUNCTION_GET_AUDITABLE_FILE_META = "get_auditable_file_metadata";
    private static final String FUNCTION_DOWNLOAD_FILE = "download_file";

    // Authz types
    private static final String AUTHZ_OK = "ok";
    private static final String AUTHZ_DENIED = "forbidden";

    // Document content types
    private static final String NONE = "notprintable";
    private static final String ADMINISTRATIVE = "administrative";
    private static final String TEACHERS = "ownedbyteachers";
    private static final String UNIVERSITY = "editedbyuniversity";
    private static final String PUBLIC_DOMAIN = "publicdomain";
    private static final String HAVE_LICENSE = "havelicense";
    private static final String FRAGMENT = "lt10fragment";
    private static final String OTHER = "other";

    // Return states
    private static final String STATE_OK = "ok";
    private static final String STATE_GT10 = "gt10";
    private static final String STATE_GT10PERM = "gt10perm";
    private static final String STATE_LOE10 = "loe10";
    private static final String STATE_FORBIDDEN = "forbidden";

    protected CopyrightCheckerService copyrightCheckerService;
    protected SakaiProxy sakaiProxy;

    private final ResourceLoader resourceLoader = new ResourceLoader("messages");

    private final JSONObject jsonError = new JSONObject();

    /**
     * Returns files to review by the external applicacion
     * @param sessionid the id of a valid session
     * @param page the page number to retrieve
     * @param count the number of elements per page to return
     * @param search a search criteria
     * @return pending files to review in JSON format
     * @throws JSONException JSON error
     */
    @WebMethod
    @Path("/" + FUNCTION_GET_PENDING_FILES + ".json")
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @GET @POST
    public String localLpiGetPendingFilesJson(
            @WebParam(name = "sessionid", partName = "sessionid") @QueryParam("sessionid") String sessionid,
            @WebParam(name = "page", partName = "page") @QueryParam("page") int page,
            @WebParam(name = "count", partName = "count") @QueryParam("count") int count,
            @WebParam(name = "search", partName = "search") @QueryParam("search") String search) throws JSONException {

        Session session = this.establishSession(sessionid);
        boolean valid = this.validateLocalLpiGetPendingFiles(session, page, count, search);
        if (!valid) {
            return jsonError.toString();
        }

        JSONObject jsonResponse = this.getPendingFiles(page, count, search);
        return jsonResponse.toString();
    }

    /**
     * Returns files to review by the external applicacion
     * @param sessionid the id of a valid session
     * @param page the page number to retrieve
     * @param count the number of elements per page to return
     * @param search a search criteria
     * @return pending files to review in XML format
     * @throws JSONException JSON error
     */
    @WebMethod
    @Path("/" + FUNCTION_GET_PENDING_FILES + ".xml")
    @Produces(MediaType.APPLICATION_XML + ";charset=utf-8")
    @GET @POST
    public String localLpiGetPendingFilesXml(
            @WebParam(name = "sessionid", partName = "sessionid") @QueryParam("sessionid") String sessionid,
            @WebParam(name = "page", partName = "page") @QueryParam("page") int page,
            @WebParam(name = "count", partName = "count") @QueryParam("count") int count,
            @WebParam(name = "search", partName = "search") @QueryParam("search") String search) throws JSONException {

        Session session = this.establishSession(sessionid);
        boolean valid = this.validateLocalLpiGetPendingFiles(session, page, count, search);
        if (!valid) {
            return "<" + FUNCTION_GET_PENDING_FILES + ">" + XML.toString(jsonError) + "</" + FUNCTION_GET_PENDING_FILES + ">";
        }

        JSONObject jsonResponse = this.getPendingFiles(page, count, search);
        return "<" + FUNCTION_GET_PENDING_FILES + ">" + XML.toString(jsonResponse) + "</" + FUNCTION_GET_PENDING_FILES + ">";
    }

    /**
     * Get file metadata
     * @param sessionid the id of a valid session
     * @param id file id
     * @return file metadata in JSON format
     * @throws JSONException JSON error
     */
    @WebMethod
    @Path("/" + FUNCTION_GET_FILE_META + ".json")
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @GET @POST
    public String localLpiGetFileMetadataJson(
            @WebParam(name = "sessionid", partName = "sessionid") @QueryParam("sessionid") String sessionid,
            @WebParam(name = "id", partName = "id") @QueryParam("id") String id) throws JSONException {

        Session session = establishSession(sessionid);
        boolean valid = validateLocalLpiGetFileMetadata(session, id);
        if (!valid) {
            return jsonError.toString();
        }

        JSONObject jsonResponse = this.getFileMetadata(id);
        return jsonResponse.toString();
    }

    /**
     * Get file metadata
     * @param sessionid the id of a valid session
     * @param id file id
     * @return file metadata in XML format
     * @throws JSONException JSON error
     */
    @WebMethod
    @Path("/" + FUNCTION_GET_FILE_META + ".xml")
    @Produces(MediaType.APPLICATION_XML + ";charset=utf-8")
    @GET @POST
    public String localLpiGetFileMetadataXml(
            @WebParam(name = "sessionid", partName = "sessionid") @QueryParam("sessionid") String sessionid,
            @WebParam(name = "id", partName = "id") @QueryParam("id") String id) throws JSONException {

        Session session = establishSession(sessionid);
        boolean valid = validateLocalLpiGetFileMetadata(session, id);
        if (!valid) {
            return "<" + FUNCTION_GET_FILE_META + ">" + XML.toString(jsonError) + "</" + FUNCTION_GET_FILE_META + ">";
        }

        JSONObject jsonResponse = this.getFileMetadata(id);
        return "<" + FUNCTION_GET_FILE_META + ">" + XML.toString(jsonResponse) + "</" + FUNCTION_GET_FILE_META + ">";
    }

    /**
     * Set file authorization
     * @param sessionid the id of a valid session
     * @param id file id
     * @param authz authorization for the file (ok, forbidden)
     * @param documentcontent documentcontent for the file (notprintable, administrative, ownedbyteachers, editedbyuniversity, publicdomain, havelicense, lt10fragment, other)
     * @param timeendlicense end license date (unix timestamp)
     * @param denyreason reason for deny authorization
     * @return file authorization in JSON format
     * @throws JSONException JSON error
     */
    @WebMethod
    @Path("/" + FUNCTION_SET_FILE_AUTHZ + ".json")
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @GET @POST
    public String localLpiSetFileAuthorizationJson(
            @WebParam(name = "sessionid", partName = "sessionid") @QueryParam("sessionid") String sessionid,
            @WebParam(name = "id", partName = "id") @QueryParam("id") String id,
            @WebParam(name = "authz", partName = "authz") @QueryParam("authz") String authz,
            @WebParam(name = "documentcontent", partName = "documentcontent") @QueryParam("documentcontent") String documentcontent,
            @WebParam(name = "timeendlicense", partName = "timeendlicense") @QueryParam("timeendlicense") long timeendlicense,
            @WebParam(name = "denyreason", partName = "denyreason") @QueryParam("denyreason") String denyreason) throws JSONException {

        Session session = establishSession(sessionid);
        boolean valid = validateLocalLpiSetFileAuthorization(session, id, authz, documentcontent, timeendlicense, denyreason);
        if (!valid) {
            return jsonError.toString();
        }

        JSONObject jsonResponse = this.setFileAuthorization(id, authz, documentcontent, timeendlicense, denyreason);
        return jsonResponse.toString();
    }

    /**
     * Set file authorization
     * @param sessionid the id of a valid session
     * @param id file id
     * @param authz authorization for the file (ok, forbidden)
     * @param documentcontent documentcontent for the file (notprintable, administrative, ownedbyteachers, editedbyuniversity, publicdomain, havelicense, lt10fragment, other)
     * @param timeendlicense end license date (unix timestamp)
     * @param denyreason reason for deny authorization
     * @return file authorization in XML format
     * @throws JSONException JSON error
     */
    @WebMethod
    @Path("/" + FUNCTION_SET_FILE_AUTHZ + ".xml")
    @Produces(MediaType.APPLICATION_XML + ";charset=utf-8")
    @GET @POST
    public String localLpiSetFileAuthorizationXml(
            @WebParam(name = "sessionid", partName = "sessionid") @QueryParam("sessionid") String sessionid,
            @WebParam(name = "id", partName = "id") @QueryParam("id") String id,
            @WebParam(name = "authz", partName = "authz") @QueryParam("authz") String authz,
            @WebParam(name = "documentcontent", partName = "documentcontent") @QueryParam("documentcontent") String documentcontent,
            @WebParam(name = "timeendlicense", partName = "timeendlicense") @QueryParam("timeendlicense") long timeendlicense,
            @WebParam(name = "denyreason", partName = "denyreason") @QueryParam("denyreason") String denyreason) throws JSONException {

        Session session = establishSession(sessionid);
        boolean valid = validateLocalLpiSetFileAuthorization(session, id, authz, documentcontent, timeendlicense, denyreason);
        if (!valid) {
            return "<" + FUNCTION_SET_FILE_AUTHZ + ">" + XML.toString(jsonError) + "</" + FUNCTION_SET_FILE_AUTHZ + ">";
        }

        JSONObject jsonResponse = this.setFileAuthorization(id, authz, documentcontent, timeendlicense, denyreason);
        return "<" + FUNCTION_SET_FILE_AUTHZ + ">" + XML.toString(jsonResponse) + "</" + FUNCTION_SET_FILE_AUTHZ + ">";
    }

    /**
     * Get metadata of auditable files
     * @param sessionid the id of a valid session
     * @param page the page number to retrieve
     * @param count the number of elements per page to return
     * @return auditable files in JSON format
     * @throws JSONException JSON error
     */
    @WebMethod
    @Path("/" + FUNCTION_GET_AUDITABLE_FILE_META + ".json")
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @GET @POST
    public String getAuditableFileMetadataJson(
            @WebParam(name = "sessionid", partName = "sessionid") @QueryParam("sessionid") String sessionid,
            @WebParam(name = "page", partName = "page") @QueryParam("page") int page,
            @WebParam(name = "count", partName = "count") @QueryParam("count") int count) throws JSONException {

        Session session = establishSession(sessionid);
        boolean valid = validateGetAuditableFileMetadata(session, page, count);
        if (!valid) {
            return jsonError.toString();
        }

        JSONObject jsonResponse = this.getAuditableFileMetadata(page, count);
        return jsonResponse.toString();
    }

    /**
     * Get metadata of auditable files
     * @param sessionid the id of a valid session
     * @param page the page number to retrieve
     * @param count the number of elements per page to return
     * @return auditable files in XML format
     * @throws JSONException JSON error
     */
    @WebMethod
    @Path("/" + FUNCTION_GET_AUDITABLE_FILE_META + ".xml")
    @Produces(MediaType.APPLICATION_XML + ";charset=utf-8")
    @GET @POST
    public String getAuditableFileMetadataXml(
            @WebParam(name = "sessionid", partName = "sessionid") @QueryParam("sessionid") String sessionid,
            @WebParam(name = "page", partName = "page") @QueryParam("page") int page,
            @WebParam(name = "count", partName = "count") @QueryParam("count") int count) throws JSONException {

        Session session = establishSession(sessionid);
        boolean valid = validateGetAuditableFileMetadata(session, page, count);
        if (!valid) {
            return "<" + FUNCTION_GET_AUDITABLE_FILE_META + ">" + XML.toString(jsonError) + "</" + FUNCTION_GET_AUDITABLE_FILE_META + ">";
        }

        JSONObject jsonResponse = this.getAuditableFileMetadata(page, count);
        return "<" + FUNCTION_GET_AUDITABLE_FILE_META + ">" + XML.toString(jsonResponse) + "</" + FUNCTION_GET_AUDITABLE_FILE_META + ">";
    }

    /**
     * Download a file in gt10perm state
     * @param sessionid the id of a valid session
     * @param id file id
     * @return the gt10perm file
     * @throws JSONException JSON error
     */
    @WebMethod
    @Path("/" + FUNCTION_DOWNLOAD_FILE)
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @GET @POST
    public Response downloadFile(
            @WebParam(name = "sessionid", partName = "sessionid") @QueryParam("sessionid") String sessionid,
            @WebParam(name = "id", partName = "id") @QueryParam("id") String id) throws JSONException {

        Session session = establishSession(sessionid);
        boolean valid = validateDownloadFile(session, id);
        if (!valid) {
            return Response.ok(jsonError.toString(), MediaType.APPLICATION_JSON).build();
        }

        IntellectualPropertyFile file = copyrightCheckerService.findIntellectualPropertyFileById(Long.valueOf(id));
        if (copyrightCheckerService.getFileState(file.getProperty(), file.getState(), false) != IntellectualPropertyFileStatus.WAITING_LICENSE) {
            jsonError.put("exception", "exception\\cannot_access_file");
            jsonError.put("errorcode", "error_cannotaccessfile");
            jsonError.put("message", resourceLoader.getString("error_cannotaccessfile"));
            return Response.ok(jsonError.toString(), MediaType.APPLICATION_JSON).build();
        }

        ContentResource resource = sakaiProxy.getContentResource(file.getFileId());
        StreamingOutput output = new StreamingOutput() {
            @Override
            public void write(OutputStream out) throws IOException, WebApplicationException {
                try {
                    out.write(resource.getContent());
                } catch (ServerOverloadException ex) {
                    log.error("Failed to get file {}" + file.getFileId());
                }
                out.flush();
                out.close();
            }
        };
        String fileName = file.getFileId().substring(file.getFileId().lastIndexOf(Entity.SEPARATOR), file.getFileId().length());
        return Response.ok(output, MediaType.APPLICATION_OCTET_STREAM)
                .header("Content-Disposition", "attachment; filename=\"" + fileName + "\"" )
                .build();
    }

    private JSONObject getPendingFiles(int page, int count, String search) throws JSONException {
        JSONObject jsonObj = new JSONObject();
        List<IntellectualPropertyFile> files = copyrightCheckerService.getPendingFiles(page, count, search);
        JSONArray jsonArray = new JSONArray();
        for (IntellectualPropertyFile file : files) {
            JSONObject fileJson = new JSONObject();
            User user = sakaiProxy.getUserById(file.getUserId());
            fileJson.put("id", file.getId());
            fileJson.put("title", file.getTitle());
            fileJson.put("author", file.getAuthor());
            fileJson.put("fragment", false);
            fileJson.put("state", "gt10");
            fileJson.put("metadatauserid", user.getEid());
            fileJson.put("metadatauseridnumber", user.getId());
            fileJson.put("metadatauseremail", user.getEmail());
            fileJson.put("metadatatimecreated", file.getCreated().toInstant().toEpochMilli() / 1000);
            jsonArray.put(fileJson);
        }
        jsonObj.put("data", jsonArray);
        jsonObj.put("count", copyrightCheckerService.countPendingFiles(search));
        return jsonObj;
    }

    private JSONObject getFileMetadata(String id) throws JSONException {
        JSONObject jsonObj = new JSONObject();
        IntellectualPropertyFile file = copyrightCheckerService.findIntellectualPropertyFileById(Long.valueOf(id));
        if (file.getState() == IntellectualPropertyFileState.GT10PERM) {
            User user = sakaiProxy.getUserById(file.getUserId());
            ContentResource contentResource = sakaiProxy.getContentResource(file.getFileId());
            jsonObj.put("id", file.getId());
            jsonObj.put("title", file.getTitle());
            jsonObj.put("author", file.getAuthor());
            jsonObj.put("fragmenttitle", JSONObject.NULL);
            jsonObj.put("fragmentauthor", JSONObject.NULL);
            jsonObj.put("publisher", file.getPublisher());
            jsonObj.put("url", sakaiProxy.getFileUrl(file.getFileId()));
            jsonObj.put("identification", file.getIdentification());
            jsonObj.put("firstpage", file.getPages());
            jsonObj.put("lastpage", file.getPages());
            jsonObj.put("totalpages", file.getTotalPages());
            jsonObj.put("enroled", sakaiProxy.getSiteMemberCount(file.getContext()));
            jsonObj.put("metadatauserid", user.getEid());
            jsonObj.put("metadatauseridnumber", user.getId());
            jsonObj.put("metadatauseremail", user.getEmail());
            jsonObj.put("metadatatimecreated", file.getCreated().toInstant().toEpochMilli() / 1000);
            jsonObj.put("uploaduserid", user.getEid());
            jsonObj.put("uploadtimecreated", file.getCreated().toInstant().toEpochMilli() / 1000);
            jsonObj.put("mimetype", contentResource.getContentType());
            try {
                jsonObj.put("filesize", contentResource.getContent().length);
            } catch (ServerOverloadException ex) {
                jsonObj.put("filesize", JSONObject.NULL);
            }
        }
        return jsonObj;
    }

    private JSONObject setFileAuthorization(String id, String authz, String documentcontent, long timeendlicense, String denyreason) throws JSONException {
        JSONObject jsonObj = new JSONObject();
        IntellectualPropertyFile file = copyrightCheckerService.findIntellectualPropertyFileById(Long.valueOf(id));
        if (file.getState() == IntellectualPropertyFileState.GT10PERM) {
            jsonObj.put("id", file.getId());
            jsonObj.put("authz", authz);

            switch (authz) {
                case AUTHZ_OK:
                    file.setState(IntellectualPropertyFileState.OK);
                    jsonObj.put("document_content", documentcontent);
                    switch (documentcontent) {
                        case NONE:
                            file.setType(IntellectualPropertyFileType.NOT_PRINTED_OR_PRINTABLE);
                            file.setProperty(IntellectualPropertyFileProperty.NONE);
                            break;
                        case ADMINISTRATIVE:
                            file.setType(IntellectualPropertyFileType.ADMINISTRATIVE);
                            file.setProperty(IntellectualPropertyFileProperty.ADMINISTRATIVE);
                            break;
                        case TEACHERS:
                            file.setType(IntellectualPropertyFileType.PRINTED_OR_PRINTABLE);
                            file.setProperty(IntellectualPropertyFileProperty.TEACHERS);
                            break;
                        case UNIVERSITY:
                            file.setType(IntellectualPropertyFileType.PRINTED_OR_PRINTABLE);
                            file.setProperty(IntellectualPropertyFileProperty.UNIVERSITY);
                            break;
                        case PUBLIC_DOMAIN:
                            file.setType(IntellectualPropertyFileType.PRINTED_OR_PRINTABLE);
                            file.setProperty(IntellectualPropertyFileProperty.PUBLIC_DOMAIN);
                            break;
                        case HAVE_LICENSE:
                            file.setType(IntellectualPropertyFileType.PRINTED_OR_PRINTABLE);
                            file.setProperty(IntellectualPropertyFileProperty.MINE);
                            if (timeendlicense != 0) {
                                jsonObj.put("timeendlicense", timeendlicense);
                                file.setLicenseEndTime(new Date(timeendlicense*1000));
                            }
                            break;
                        case FRAGMENT:
                            file.setType(IntellectualPropertyFileType.PRINTED_OR_PRINTABLE);
                            file.setProperty(IntellectualPropertyFileProperty.FRAGMENT);
                            break;
                        case OTHER:
                            file.setType(IntellectualPropertyFileType.PRINTED_OR_PRINTABLE);
                            file.setProperty(IntellectualPropertyFileProperty.FULL);
                            if (timeendlicense != 0) {
                                jsonObj.put("timeendlicense", timeendlicense);
                                file.setLicenseEndTime(new Date(timeendlicense*1000));
                            }
                            break;
                        default:
                            break;
                    }
                    if (sakaiProxy.fileIsHidden(file.getFileId())) {
                        sakaiProxy.setContentResourceVisibility(file.getFileId(), true);
                    }
                    break;
                case AUTHZ_DENIED:
                    file.setState(IntellectualPropertyFileState.DENIED);
                    file.setDenyReason(denyreason);
                    jsonObj.put("denystate", denyreason);
                    break;
                default:
                    break;
            }
            copyrightCheckerService.saveIntellectualPropertyFile(file);
        }
        return jsonObj;
    }

    private JSONObject getAuditableFileMetadata(int page, int count) throws JSONException {
        JSONObject jsonObj = new JSONObject();
        List<IntellectualPropertyFile> files = copyrightCheckerService.getAuditableFiles(page, count);
        JSONArray jsonArray = new JSONArray();
        for (IntellectualPropertyFile file : files) {
            JSONObject fileJson = new JSONObject();
            Site site = sakaiProxy.getSite(file.getContext());
            fileJson.put("id", file.getId());
            switch (file.getState()) {
                case IntellectualPropertyFileState.OK:
                    fileJson.put("state", STATE_OK);
                    break;
                case IntellectualPropertyFileState.GT10:
                    fileJson.put("state", STATE_GT10);
                    break;
                case IntellectualPropertyFileState.GT10PERM:
                    fileJson.put("state", STATE_GT10PERM);
                    break;
                case IntellectualPropertyFileState.LOE10:
                    fileJson.put("state", STATE_LOE10);
                    break;
                case IntellectualPropertyFileState.DENIED:
                    fileJson.put("state", STATE_FORBIDDEN);
                    break;
                default:
                    break;
            }
            if (site != null) {
                fileJson.put("coursetimecreated", site.getCreatedDate().toInstant().toEpochMilli() / 1000);
                fileJson.put("enroled", site.getMembers().size());
            } else {
                fileJson.put("coursetimecreated", JSONObject.NULL);
                fileJson.put("enroled", JSONObject.NULL);
            }
            fileJson.put("timemodified", file.getModified().toInstant().toEpochMilli() / 1000);
            fileJson.put("license", file.getLicense());
            fileJson.put("title", file.getTitle());
            fileJson.put("author", file.getAuthor());
            fileJson.put("publisher", file.getPublisher());
            fileJson.put("identification", file.getIdentification());
            fileJson.put("pages", file.getPages());
            fileJson.put("totalpages", file.getTotalPages());
            if (file.getLicenseEndTime() != null) {
                fileJson.put("timeendlicense", file.getLicenseEndTime().toInstant().toEpochMilli() / 1000);
            } else {
                fileJson.put("timeendlicense", JSONObject.NULL);
            }
            fileJson.put("comments", file.getComments());
            fileJson.put("courseid", file.getContext());
            fileJson.put("downloadurl", sakaiProxy.getFileUrl(file.getFileId()));
            jsonArray.put(fileJson);
        }
        jsonObj.put("data", jsonArray);
        jsonObj.put("count", copyrightCheckerService.countAuditableFiles());
        return jsonObj;
    }

    protected Session establishSession(String sessionid) {
        Session s = sakaiProxy.getSession(sessionid);
        if (s != null) {
            s.setActive();
            sakaiProxy.setCurrentSession(s);
        }
        return s;
    }

    private boolean validateSessionAndPermission(Session session) throws JSONException {
        // Session validation
        if (session == null) {
            jsonError.put("exception", "exception\\invalid_session");
            jsonError.put("errorcode", "error_invalidsession");
            jsonError.put("message", resourceLoader.getString("error_invalidsession"));
            return false;
        }
        // Permission validation
        User user = sakaiProxy.getUserById(sakaiProxy.getCurrentUserId());
        ResourceProperties userProperties = user.getProperties();
        if (!sakaiProxy.isSuperUser() && !"true".equals(userProperties.getProperty(PERMISSION_PROPERTY))) {
            jsonError.put("exception", "exception\\no_permission");
            jsonError.put("errorcode", "error_nopermission");
            jsonError.put("message", resourceLoader.getString("error_nopermission"));
            return false;
        }
        return true;
    }

    private boolean validateLocalLpiGetPendingFiles(Session session, int page, int count, String search) throws JSONException {
        log.debug("Validate pending files page={}, count={}, search={}", page, count, search);

        if (!validateSessionAndPermission(session)) return false;

        if (page == 0) {
            jsonError.put("exception", "exception\\page_cannotbe_0");
            jsonError.put("errorcode", "error_pagecannotbe0");
            jsonError.put("message", resourceLoader.getString("error_pagecannotbe0"));
            return false;
        }
        if (count == 0) {
            jsonError.put("exception", "exception\\count_cannotbe_0");
            jsonError.put("errorcode", "error_countcannotbe0");
            jsonError.put("message", resourceLoader.getString("error_countcannotbe0"));
            return false;
        }
        return true;
    }

    private boolean validateLocalLpiGetFileMetadata(Session session, String id) throws JSONException {
        if (!validateSessionAndPermission(session)) return false;
        if (StringUtils.isBlank(id)) {
            jsonError.put("exception", "exception\\id_empty");
            jsonError.put("errorcode", "error_idempty");
            jsonError.put("message", resourceLoader.getString("error_idempty"));
            return false;
        }
        if (!copyrightCheckerService.existsIntellectualPropertyFileById(Long.valueOf(id))) {
            jsonError.put("exception", "exception\\file_not_found");
            jsonError.put("errorcode", "error_filenotfound");
            jsonError.put("message", resourceLoader.getString("error_filenotfound"));
            return false;
        }
        return true;
    }

    private boolean validateLocalLpiSetFileAuthorization(Session session, String id, String authz, String documentcontent, long timeendlicense, String denyreason) throws JSONException {

        log.debug("Validating file authoritazion id={}, authz={}, documentcontent={}, timeendlicense={}, denyreason={}", id, authz, documentcontent, timeendlicense, denyreason);

        if (!validateSessionAndPermission(session)) return false;

        if (StringUtils.isBlank(id)) {
            jsonError.put("exception", "exception\\id_empty");
            jsonError.put("errorcode", "error_idempty");
            jsonError.put("message", resourceLoader.getString("error_idempty"));
            return false;
        }
        if (!copyrightCheckerService.existsIntellectualPropertyFileById(Long.valueOf(id))) {
            jsonError.put("exception", "exception\\file_not_found");
            jsonError.put("errorcode", "error_filenotfound");
            jsonError.put("message", resourceLoader.getString("error_filenotfound"));
            return false;
        }
        if (StringUtils.isBlank(authz)) {
            jsonError.put("exception", "exception\\authz_empty");
            jsonError.put("errorcode", "error_authzempty");
            jsonError.put("message", resourceLoader.getString("error_authzempty"));
            return false;
        }
        if (!AUTHZ_OK.equals(authz) && !AUTHZ_DENIED.equals(authz)) {
            jsonError.put("exception", "exception\\authz_invalid");
            jsonError.put("errorcode", "error_authzinvalid");
            jsonError.put("message", resourceLoader.getString("error_authzinvalid"));
            return false;
        }
        if (AUTHZ_OK.equals(authz)) {
            if (StringUtils.isBlank(documentcontent)) {
                jsonError.put("exception", "exception\\documentcontent_empty");
                jsonError.put("errorcode", "error_documentcontentempty");
                jsonError.put("message", resourceLoader.getString("error_documentcontentempty"));
                return false;
            }
            if (!NONE.equals(documentcontent) && !ADMINISTRATIVE.equals(documentcontent) &&
                    !TEACHERS.equals(documentcontent) && !UNIVERSITY.equals(documentcontent) &&
                    !PUBLIC_DOMAIN.equals(documentcontent) && !HAVE_LICENSE.equals(documentcontent) &&
                    !FRAGMENT.equals(documentcontent) && !OTHER.equals(documentcontent)) {
                jsonError.put("exception", "exception\\documentcontent_invalid");
                jsonError.put("errorcode", "error_documentcontentinvalid");
                jsonError.put("message", resourceLoader.getString("error_documentcontentinvalid"));
                return false;
            }
        }
        return true;
    }

    private boolean validateGetAuditableFileMetadata(Session session, int page, int count) throws JSONException {
        if (!validateSessionAndPermission(session)) return false;
        if (page == 0) {
            jsonError.put("exception", "exception\\page_cannotbe_0");
            jsonError.put("errorcode", "error_pagecannotbe0");
            jsonError.put("message", resourceLoader.getString("error_pagecannotbe0"));
            return false;
        }
        if (count == 0) {
            jsonError.put("exception", "exception\\count_cannotbe_0");
            jsonError.put("errorcode", "error_countcannotbe0");
            jsonError.put("message", resourceLoader.getString("error_countcannotbe0"));
            return false;
        }
        return true;
    }

    private boolean validateDownloadFile(Session session, String id) throws JSONException {
        if (!validateSessionAndPermission(session)) return false;
        if (StringUtils.isBlank(id)) {
            jsonError.put("exception", "exception\\id_empty");
            jsonError.put("errorcode", "error_idempty");
            jsonError.put("message", resourceLoader.getString("error_idempty"));
            return false;
        }
        if (!copyrightCheckerService.existsIntellectualPropertyFileById(Long.valueOf(id))) {
            jsonError.put("exception", "exception\\file_not_found");
            jsonError.put("errorcode", "error_filenotfound");
            jsonError.put("message", resourceLoader.getString("error_filenotfound"));
            return false;
        }
        return true;
    }

    @WebMethod(exclude = true)
    public void init() {
        resourceLoader.setContextLocale(resourceLoader.getLocale());
    }

    @WebMethod(exclude = true)
    public CopyrightCheckerService getCopyrightCheckerService() {
        return copyrightCheckerService;
    }

    @WebMethod(exclude = true)
    public void setCopyrightCheckerService(CopyrightCheckerService copyrightCheckerService) {
        this.copyrightCheckerService = copyrightCheckerService;
    }

    @WebMethod(exclude = true)
    public SakaiProxy getSakaiProxy() {
        return sakaiProxy;
    }

    @WebMethod(exclude = true)
    public void setSakaiProxy(SakaiProxy sakaiProxy) {
        this.sakaiProxy = sakaiProxy;
    }
}
