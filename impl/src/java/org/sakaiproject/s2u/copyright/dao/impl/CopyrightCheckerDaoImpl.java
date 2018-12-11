package org.sakaiproject.s2u.copyright.dao.impl;

import java.io.Serializable;
import java.util.stream.Collectors;
import java.util.List;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.StringUtils;

import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

import org.springframework.orm.hibernate4.HibernateCallback;
import org.springframework.orm.hibernate4.support.HibernateDaoSupport;

import org.sakaiproject.s2u.copyright.dao.CopyrightCheckerDao;
import org.sakaiproject.s2u.copyright.model.IntellectualPropertyFile;
import org.sakaiproject.s2u.copyright.model.IntellectualPropertyFileDoubt;
import org.sakaiproject.s2u.copyright.model.IntellectualPropertyFileProperty;
import org.sakaiproject.s2u.copyright.model.IntellectualPropertyFileState;
import org.springframework.dao.DataAccessException;

/**
 * Implementation of CopyrightCheckerDao
 *
 */
@Slf4j
public class CopyrightCheckerDaoImpl extends HibernateDaoSupport implements CopyrightCheckerDao, Serializable {

    public static final String ID = "id";
    public static final String FILE_ID = "fileId";
    public static final String USER_ID = "userId";
    public static final String PROPERTY = "state";
    public static final String STATE = "state";
    public static final String AUTHOR = "author";
    public static final String TITLE = "title";

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("unchecked")
    public List<IntellectualPropertyFile> findIntellectualPropertyFiles() {
        return this.findFilesByCriteria(null, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("unchecked")
    public List<IntellectualPropertyFile> findIntellectualPropertyFilesByState(Integer state){
        return this.findFilesByCriteria(null, state);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("unchecked")
    public List<IntellectualPropertyFile> findIntellectualPropertyFilesByUserId(String userId){
        return this.findFilesByCriteria(userId, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("unchecked")
    public Integer countIntellectualPropertyFilesByUserId(String userId) {
        HibernateCallback hcb = new HibernateCallback() {
            @Override
            public Object doInHibernate(Session session) throws HibernateException {
                Criteria crit = session.createCriteria(IntellectualPropertyFile.class);
                if(StringUtils.isNotEmpty(userId)) {
                    crit.add(Restrictions.eq(USER_ID, userId));
                    crit.add(Restrictions.ne(STATE, IntellectualPropertyFileState.DELETED));
                }
                return crit.setProjection(Projections.rowCount()).uniqueResult();
            }
        };
        return (Integer) getHibernateTemplate().execute(hcb);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("unchecked")
    public List<IntellectualPropertyFile> findIntellectualPropertyFilesByUserIdAndState(String userId, Integer state){
        return this.findFilesByCriteria(userId, state);
    }

    private List<IntellectualPropertyFile> findFilesByCriteria(String userId, Integer state){
        HibernateCallback hcb = new HibernateCallback() {
            @Override
            public Object doInHibernate(Session session) throws HibernateException {
                Criteria crit = session.createCriteria(IntellectualPropertyFile.class);
                if (StringUtils.isNotEmpty(userId)) {
                    crit.add(Restrictions.eq(USER_ID, userId));
                    crit.add(Restrictions.ne(STATE, IntellectualPropertyFileState.DELETED));
                }
                if (state != null) {
                    crit.add(Restrictions.eq(STATE, state));
                }
                return crit.list().stream().distinct().collect(Collectors.toList());//filtering duplicates due to fetch join/eager
            }
        };
        return (List) getHibernateTemplate().execute(hcb);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("unchecked")
    public IntellectualPropertyFile findIntellectualPropertyFileById(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("Null id passed to getIntellectualPropertyFile");
        }
        return this.findIntellectualPropertyFileByCriteria(id, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("unchecked")
    public boolean existsIntellectualPropertyFileById(Long id) {
        HibernateCallback hcb = new HibernateCallback() {
            @Override
            public Object doInHibernate(Session session) throws HibernateException {
                Criteria crit = session.createCriteria(IntellectualPropertyFile.class);
                crit.add(Restrictions.eq(ID, id));
                return crit.setProjection(Projections.rowCount()).uniqueResult();
            }
        };
        Integer result = (Integer) getHibernateTemplate().execute(hcb);
        return (result > 0);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("unchecked")
    public IntellectualPropertyFile findIntellectualPropertyFileByFileId(String fileId) {
        if (fileId == null) {
            throw new IllegalArgumentException("Null id passed to findIntellectualPropertyFileByFileId");
        }
        return this.findIntellectualPropertyFileByCriteria(null, fileId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("unchecked")
    public List<IntellectualPropertyFile> getPendingFiles(int page, int count, String search) {
        HibernateCallback hcb = new HibernateCallback() {
            @Override
            public Object doInHibernate(Session session) throws HibernateException {
                Criteria crit = session.createCriteria(IntellectualPropertyFile.class);
                crit.setFirstResult((count*page)-count);
                crit.setMaxResults(count);
                crit.add(Restrictions.eq(STATE, IntellectualPropertyFileState.GT10PERM));
                if (StringUtils.isNotEmpty(search)) {
                    crit.add(Restrictions.or(Restrictions.like(AUTHOR, search, MatchMode.ANYWHERE), Restrictions.like(TITLE, search, MatchMode.ANYWHERE)));
                }
                return crit.list().stream().distinct().collect(Collectors.toList());
            }
        };
        return (List) getHibernateTemplate().execute(hcb);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("unchecked")
    public Integer countPendingFiles(String search) {
        HibernateCallback hcb = new HibernateCallback() {
            @Override
            public Object doInHibernate(Session session) throws HibernateException {
                Criteria crit = session.createCriteria(IntellectualPropertyFile.class);
                if (StringUtils.isNotEmpty(search)) {
                    crit.add(Restrictions.or(Restrictions.like(AUTHOR, search, MatchMode.ANYWHERE), Restrictions.like(TITLE, search, MatchMode.ANYWHERE)));
                }
                crit.add(Restrictions.eq(STATE, IntellectualPropertyFileState.GT10PERM));
                return crit.setProjection(Projections.rowCount()).uniqueResult();
            }
        };
        return (Integer) getHibernateTemplate().execute(hcb);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("unchecked")
    public List<IntellectualPropertyFile> getAuditableFiles(int page, int count) {
        HibernateCallback hcb = new HibernateCallback() {
            public Object doInHibernate(Session session) throws HibernateException {
                Criteria crit = session.createCriteria(IntellectualPropertyFile.class);
                crit.setFirstResult((count*page)-count);
                crit.setMaxResults(count);
                crit.add(Restrictions.ne(STATE, IntellectualPropertyFileState.NONE));
                crit.add(Restrictions.ne(STATE, IntellectualPropertyFileState.DELETED));
                crit.add(Restrictions.ne(STATE, IntellectualPropertyFileState.DENIED));
                crit.add(Restrictions.ne(PROPERTY, IntellectualPropertyFileProperty.ADMINISTRATIVE));
                crit.add(Restrictions.ne(PROPERTY, IntellectualPropertyFileProperty.FULL));
                return crit.list().stream().distinct().collect(Collectors.toList());
            }
        };
        return (List) getHibernateTemplate().execute(hcb);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("unchecked")
    public Integer countAuditableFiles() {
        HibernateCallback hcb = new HibernateCallback() {
            @Override
            public Object doInHibernate(Session session) throws HibernateException {
                Criteria crit = session.createCriteria(IntellectualPropertyFile.class);
                crit.add(Restrictions.ne(STATE, IntellectualPropertyFileState.NONE));
                crit.add(Restrictions.ne(STATE, IntellectualPropertyFileState.DELETED));
                crit.add(Restrictions.ne(STATE, IntellectualPropertyFileState.DENIED));
                crit.add(Restrictions.ne(PROPERTY, IntellectualPropertyFileProperty.ADMINISTRATIVE));
                crit.add(Restrictions.ne(PROPERTY, IntellectualPropertyFileProperty.FULL));
                return crit.setProjection(Projections.rowCount()).uniqueResult();
            }
        };
        return (Integer) getHibernateTemplate().execute(hcb);
    }

    private IntellectualPropertyFile findIntellectualPropertyFileByCriteria(Long id, String fileId) {
        HibernateCallback hcb = new HibernateCallback() {
            @Override
            public Object doInHibernate(Session session) throws HibernateException {
                Criteria crit = session.createCriteria(IntellectualPropertyFile.class);
                if(id != null) {
                    crit.add(Restrictions.eq(ID, id));
                }
                if(StringUtils.isNotEmpty(fileId)) {
                    crit.add(Restrictions.eq(FILE_ID, fileId));
                    crit.add(Restrictions.ne(STATE, IntellectualPropertyFileState.DELETED));
                }
                IntellectualPropertyFile ipFile = (IntellectualPropertyFile)crit.uniqueResult();
                return ipFile;
            }
        };
        return (IntellectualPropertyFile) getHibernateTemplate().execute(hcb);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("unchecked")
    public boolean saveIntellectualPropertyFile(IntellectualPropertyFile l) throws IllegalArgumentException {
        boolean success = false;
        if (l == null) {
            throw new IllegalArgumentException("Null Argument");
        } else {
            try {
                getHibernateTemplate().saveOrUpdate(l);
                success = true;
            } catch(DataAccessException e){
                log.error("Error while trying to save IntellectualPropertyFile element " + e.getMessage());
            }
        }
        return success;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("unchecked")
        public boolean saveIntellectualPropertyFileDoubt(IntellectualPropertyFileDoubt t) {
        boolean success = false;
        if (t == null) {
            throw new IllegalArgumentException("Null Argument");
        } else {
            try {
                getHibernateTemplate().saveOrUpdate(t);
                success = true;
            } catch(DataAccessException e){
                log.error("Error while trying to save IntellectualPropertyFileDoubt element " + e.getMessage());
            }
        }
        return success;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("unchecked")
    public void removeIntellectualPropertyFile(IntellectualPropertyFile l) {
        if (l != null) {
            for(IntellectualPropertyFileDoubt lfd : l.getDoubts()){
                deleteIntellectualPropertyFileDoubt(lfd);
            }
            HibernateCallback hcb = new HibernateCallback() {
                @Override
                public Object doInHibernate(Session session) throws HibernateException {
                    session.delete(l);
                    return null;
                }
            };
            getHibernateTemplate().execute(hcb);
        }
    }

    private void deleteIntellectualPropertyFileDoubt(IntellectualPropertyFileDoubt lfd) {
        if (lfd != null) {
            HibernateCallback hcb = new HibernateCallback() {
                @Override
                public Object doInHibernate(Session session) throws HibernateException {
                    session.delete(lfd);
                    return null;
                }
            };
            getHibernateTemplate().execute(hcb);
        }
    }
}
