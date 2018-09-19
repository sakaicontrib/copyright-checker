package org.sakaiproject.s2u.copyright.tool.events;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import org.apache.wicket.ajax.AjaxRequestTarget;

import org.sakaiproject.s2u.copyright.model.IntellectualPropertyFile;

@Getter @Setter
@AllArgsConstructor
public class RowUpdateEvent {
    private final AjaxRequestTarget target;
    private IntellectualPropertyFile payload;
    private boolean visible;
}
