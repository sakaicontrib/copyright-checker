package org.sakaiproject.s2u.copyright.logic;

import java.util.List;

import org.sakaiproject.s2u.copyright.model.IntellectualPropertyFile;
import org.sakaiproject.s2u.copyright.model.IntellectualPropertyFileDoubt;

/**
 * Service interface for the Copyright Checker
 *
 */
public interface CopyrightCheckerService {

    /**
     * Get a IntellectualPropertyFile by Id
     * @param id File id
     * @return an item or null if no result
     */
    public IntellectualPropertyFile findIntellectualPropertyFileById(long id);

    /**
     * Exists a IntellectualPropertyFile
     * @param id File id
     * @return if an item exists
     */
    public boolean existsIntellectualPropertyFileById(long id);

    /**
     * Get a IntellectualPropertyFile from fileId
     * @param fileId File id
     * @return an item or null if no result
     */
    public IntellectualPropertyFile findIntellectualPropertyFileByFileId(String fileId);

    /**
     * Get all IntellectualPropertyFiles
     * @return a list of items, an empty list if no items
     */
    public List<IntellectualPropertyFile> findIntellectualPropertyFiles();

    /**
     * Get IntellectualPropertyFiles by state
     * @param state File state
     * @return a list of items, an empty list if no items
     */
    public List<IntellectualPropertyFile> findIntellectualPropertyFilesByState(Integer state);

    /**
     * Get IntellectualPropertyFiles by userId
     * @param userId User id
     * @return a list of items, an empty list if no items
     */
    public List<IntellectualPropertyFile> findIntellectualPropertyFilesByUserId(String userId);

    /**
     * Returns files to review by the external applicacion
     * @param page the page number to retrieve
     * @param count the number of elements per page to return
     * @param search a search criteria
     * @return a list of pending files to review
     */
    public List<IntellectualPropertyFile> getPendingFiles(int page, int count, String search);

    /**
     * Count pending files to review
     * @param search a search criteria
     * @return count of pending files to review
     */
    public Integer countPendingFiles(String search);

    /**
     * Get auditable files
     * @param page the page number to retrieve
     * @param count the number of elements per page to return
     * @return a list of auditable files
     */
    public List<IntellectualPropertyFile> getAuditableFiles(int page, int count);

    /**
     * Count auditable files
     * @return count of auditable files
     */
    public Integer countAuditableFiles();

    /**
     * Count IntellectualPropertyFiles by userId
     * @param userId User id
     * @return a count of items
     */
    public Integer countIntellectualPropertyFilesByUserId(String userId);

    /**
     * Get IntellectualPropertyFiles by userId and state
     * @param userId User id
     * @param state File state
     * @return a list of items, an empty list if no items
     */
    public List<IntellectualPropertyFile> findIntellectualPropertyFilesByUserIdAndState(String userId, Integer state);

    /**
     * Saves or update a IntellectualPropertyFile record to the database. Only the name property is actually used.
     * @param t IntellectualPropertyFile
     * @return true if success, false if not
     */
    public boolean saveIntellectualPropertyFile(IntellectualPropertyFile t);

    /**
     * Saves or update a IntellectualPropertyFileDoubt record to the database.
     * @param t IntellectualPropertyFileDoubt
     * @return true if success, false if not
     */
    public boolean saveIntellectualPropertyFileDoubt(IntellectualPropertyFileDoubt t);

    /**
     * Get the file state by its file status
     * @param fileStatus the file status
     * @param state the file state
     * @param deleted is the file deleted?
     * @return the file state
     */
    public int getFileState(int fileStatus, int state, boolean deleted);
}
