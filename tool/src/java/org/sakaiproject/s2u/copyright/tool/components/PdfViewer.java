package org.sakaiproject.s2u.copyright.tool.components;

import lombok.Getter;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.WebMarkupContainer;

public class PdfViewer extends WebMarkupContainer{

    private static final String ROOT_URL = "/copyright-checker-tool/webjars/viewerjs/ViewerJS/index.html#";

    @Getter private String fileUrl;

    public PdfViewer(String id) {
        super(id);
        this.add(new AttributeModifier("src", "about:blank"));
    }

    public void setFileUrl(String fileUrl){
        this.fileUrl = fileUrl;
        this.add(new AttributeModifier("src",  ROOT_URL + this.fileUrl));
    }
}
