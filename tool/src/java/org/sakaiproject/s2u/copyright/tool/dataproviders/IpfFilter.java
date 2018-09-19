package org.sakaiproject.s2u.copyright.tool.dataproviders;

import java.io.Serializable;
import java.util.Date;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import org.apache.wicket.extensions.markup.html.form.select.SelectOption;

@Getter @Setter
@ToString
public class IpfFilter implements Serializable {
    private Date dateFrom;
    private Date dateTo;
    private SelectOption visibility;
    private String fileName;
    private SelectOption displayStatus;
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
    private Boolean perpetual;
    private int enrollments;//estimation of the students enrolled
    private Date lastDoubtTime;
}
