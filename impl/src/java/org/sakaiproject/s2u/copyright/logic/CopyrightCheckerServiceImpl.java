package org.sakaiproject.s2u.copyright.logic;

import java.util.List;

import lombok.extern.slf4j.Slf4j;
import lombok.Setter;

import org.sakaiproject.s2u.copyright.dao.CopyrightCheckerDao;
import org.sakaiproject.s2u.copyright.model.IntellectualPropertyFile;
import org.sakaiproject.s2u.copyright.model.IntellectualPropertyFileDoubt;
import org.sakaiproject.s2u.copyright.model.IntellectualPropertyFileProperty;
import org.sakaiproject.s2u.copyright.model.IntellectualPropertyFileState;
import org.sakaiproject.s2u.copyright.model.IntellectualPropertyFileStatus;

/**
 * Implementation of {@link CopyrightCheckerService}
 *
 */
@Slf4j
public class CopyrightCheckerServiceImpl implements CopyrightCheckerService {

    @Setter private CopyrightCheckerDao dao;

    /**
     * {@inheritDoc}
     */
    @Override
    public IntellectualPropertyFile findIntellectualPropertyFileById(long id) {
        return dao.findIntellectualPropertyFileById(id);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean existsIntellectualPropertyFileById(long id) {
        return dao.existsIntellectualPropertyFileById(id);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IntellectualPropertyFile findIntellectualPropertyFileByFileId(String fileId) {
        return dao.findIntellectualPropertyFileByFileId(fileId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<IntellectualPropertyFile> findIntellectualPropertyFilesByUserId(String userId){
        return dao.findIntellectualPropertyFilesByUserId(userId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<IntellectualPropertyFile> getPendingFiles(int page, int count, String search) {
        return dao.getPendingFiles(page, count, search);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Integer countPendingFiles(String search) {
        return dao.countPendingFiles(search);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<IntellectualPropertyFile> getAuditableFiles(int page, int count) {
        return dao.getAuditableFiles(page, count);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Integer countAuditableFiles() {
        return dao.countAuditableFiles();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Integer countIntellectualPropertyFilesByUserId(String userId) {
        return dao.countIntellectualPropertyFilesByUserId(userId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<IntellectualPropertyFile> findIntellectualPropertyFilesByUserIdAndState(String userId, Integer state){
        return dao.findIntellectualPropertyFilesByUserIdAndState(userId, state);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<IntellectualPropertyFile> findIntellectualPropertyFiles() {
        return dao.findIntellectualPropertyFiles();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<IntellectualPropertyFile> findIntellectualPropertyFilesByState(Integer state){
        return dao.findIntellectualPropertyFilesByState(state);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean saveIntellectualPropertyFile(IntellectualPropertyFile t) {
        boolean saved = dao.saveIntellectualPropertyFile(t);
        return saved;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean saveIntellectualPropertyFileDoubt(IntellectualPropertyFileDoubt t) {
    return dao.saveIntellectualPropertyFileDoubt(t);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getFileState(int fileStatus, int state, boolean deleted) {
        int stateValue = 0;
        if (deleted) {
            return IntellectualPropertyFileState.DELETED;
        }
        // Based on file state
        switch (state) {
            case IntellectualPropertyFileState.DENIED:
                stateValue = IntellectualPropertyFileStatus.NOT_AUTHORIZED;
                break;
            case IntellectualPropertyFileState.OK:
                stateValue = IntellectualPropertyFileStatus.AUTHORIZED;
                break;
            default:
                break;
        }
        if (stateValue != 0) return stateValue;

        // Based on file status
        switch(fileStatus){
            case IntellectualPropertyFileProperty.NOT_PROCESSED:
                stateValue = IntellectualPropertyFileStatus.NOT_PROCESSED;
                break;
            case IntellectualPropertyFileProperty.NONE:
                stateValue = IntellectualPropertyFileStatus.NOT_PRINTABLE;
                break;
            case IntellectualPropertyFileProperty.ADMINISTRATIVE:
            case IntellectualPropertyFileProperty.TEACHERS:
            case IntellectualPropertyFileProperty.UNIVERSITY:
            case IntellectualPropertyFileProperty.PUBLIC_DOMAIN:
            case IntellectualPropertyFileProperty.MINE:
            case IntellectualPropertyFileProperty.FRAGMENT:
                stateValue = IntellectualPropertyFileStatus.AUTHORIZED;
                break;
            case IntellectualPropertyFileProperty.FULL:
                stateValue = IntellectualPropertyFileStatus.WAITING_LICENSE;
                break;
            default:
                break;
        }
        return stateValue;
    }

    public void init() {
        log.info("init");
    }
}
