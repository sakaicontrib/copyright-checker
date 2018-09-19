package org.sakaiproject.s2u.copyright.tool.cells;

import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.html.panel.Panel;

public class FileNameCell extends Panel {

    public FileNameCell(String id, String fileUrl, String fileName) {
        super(id);
        ExternalLink fileLink = new ExternalLink("fileLink", fileUrl, fileName);
        add(fileLink);
    }
}
