package org.sakaiproject.s2u.copyright.tool.components;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.markup.html.panel.Panel;

import org.sakaiproject.s2u.copyright.tool.pages.DocumentManagement;

public class FilterActionsHeader extends Panel{

    public FilterActionsHeader(String id) {
        super(id);
        AjaxButton cleanFilterBtn = new AjaxButton("cleanFilterBtn"){
            @Override
            protected void onSubmit(AjaxRequestTarget target) {
                setResponsePage(DocumentManagement.class);
            }
        };
        add(cleanFilterBtn);
    }
}
