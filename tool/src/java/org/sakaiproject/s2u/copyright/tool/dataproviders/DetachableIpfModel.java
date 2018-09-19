package org.sakaiproject.s2u.copyright.tool.dataproviders;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import org.apache.commons.lang3.StringUtils;

import org.apache.wicket.injection.Injector;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

import org.sakaiproject.s2u.copyright.logic.CopyrightCheckerService;
import org.sakaiproject.s2u.copyright.logic.SakaiProxy;
import org.sakaiproject.s2u.copyright.model.IntellectualPropertyFile;

@Getter
@ToString
public class DetachableIpfModel extends LoadableDetachableModel<IntellectualPropertyFile> {

    @SpringBean(name = "org.sakaiproject.s2u.copyright.logic.CopyrightCheckerService")
    protected transient CopyrightCheckerService copyrightCheckerService;

    @SpringBean(name = "org.sakaiproject.s2u.copyright.logic.SakaiProxy")
    protected transient SakaiProxy sakaiProxy;

    private final long id;
    private final String displayContext;
    private final String fileName;
    private final String fileUrl;
    private final int displayStatus;
    @Setter
    private boolean isHidden;
    private final boolean isDeleted;
    private final String mimeType;

    /**
     * @param c
     */
    public DetachableIpfModel(IntellectualPropertyFile c) {
        Injector.get().inject(this);
        String fileId = c.getFileId();
        this.id = c.getId();
        this.displayContext = sakaiProxy.getFileContextName(c.getContext());
        this.fileName = sakaiProxy.getFileName(fileId);
        this.fileUrl = sakaiProxy.getFileUrl(fileId);
        this.isDeleted = (StringUtils.isBlank(this.fileName));
        this.displayStatus = copyrightCheckerService.getFileState(c.getProperty(), c.getState(), this.isDeleted);
        this.isHidden = sakaiProxy.fileIsHidden(fileId);
        this.mimeType = sakaiProxy.getContentResource(fileId) == null ? "null" : sakaiProxy.getContentResource(fileId).getContentType();
    }

    @Override
    public int hashCode() {
        return Long.valueOf(id).hashCode();
    }

    /**
     * used for dataview with ReuseIfModelsEqualStrategy item reuse strategy
     * @return boolean
     * @see org.apache.wicket.markup.repeater.ReuseIfModelsEqualStrategy
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        } else if (obj == null) {
            return false;
        } else if (obj instanceof DetachableIpfModel) {
            DetachableIpfModel other = (DetachableIpfModel) obj;
            return other.id == id;
        }
        return false;
    }

    /**
     * @see org.apache.wicket.model.LoadableDetachableModel#load()
     * @return
     */
    @Override
    protected IntellectualPropertyFile load() {
        Injector.get().inject(this);
        return copyrightCheckerService.findIntellectualPropertyFileById(id);
    }

    public static List<DetachableIpfModel> fromList(List<IntellectualPropertyFile> list) {
        List<DetachableIpfModel> aux = new ArrayList<>();
        list.forEach(item -> {
            aux.add(new DetachableIpfModel(item));
        });
        return aux;
    }

    public static DetachableIpfModel of(IntellectualPropertyFile c) {
        return new DetachableIpfModel(c);
    }
}
