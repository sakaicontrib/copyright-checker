package org.sakaiproject.s2u.copyright.model;

import java.io.Serializable;
import java.util.Date;
import java.util.Set;
import java.util.TreeSet;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of="id")
@ToString
public class IntellectualPropertyFile implements Serializable {
    private long id;
    private String fileId;//Sakai resourceId
    private String context;//Sakai siteId
    private String userId;//user that authorizes the file
    private Integer property;//mine, public domain, other
    private Integer type;//text, photo, etc.
    private String author;
    private String license;
    private String title;
    private String identification;//ISBNxxx
    private Integer state;//ok, loe10, gt10, dontknow
    private Date created;
    private Date modified;
    private String publisher;//editorial
    private String pages;//in free format (ie: 1-10,24,50-100)
    private String totalPages;
    private String rightsEntity;
    private Date licenseEndTime;
    private Boolean perpetual;
    private String comments;
    private String denyReason;
    private Integer enrollments;//estimation of the students enrolled
    private Date lastDoubtTime;
    private Set<IntellectualPropertyFileDoubt> doubts = new TreeSet<>();
}
