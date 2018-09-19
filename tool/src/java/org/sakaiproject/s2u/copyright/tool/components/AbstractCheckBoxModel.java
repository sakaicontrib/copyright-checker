package org.sakaiproject.s2u.copyright.tool.components;

import org.apache.wicket.model.IModel;

public abstract class AbstractCheckBoxModel implements IModel {
    public abstract boolean isSelected();
    public abstract void select();
    public abstract void unselect();
}
