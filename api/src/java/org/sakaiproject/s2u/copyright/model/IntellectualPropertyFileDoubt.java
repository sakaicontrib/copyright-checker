package org.sakaiproject.s2u.copyright.model;

import java.io.Serializable;
import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 *
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of="id")
@ToString(exclude="file")
public class IntellectualPropertyFileDoubt implements Serializable, Comparable<IntellectualPropertyFileDoubt> {
    private long id;
    private IntellectualPropertyFile file;
    private String userId;
    private Date created;
    private Date sent;
    private String message;
    private int messageFormat;

    public int compareTo(IntellectualPropertyFileDoubt compareNot) {
        return Long.compare(this.id, compareNot.id);
    }
}
