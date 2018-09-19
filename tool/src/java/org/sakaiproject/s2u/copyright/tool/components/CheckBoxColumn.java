package org.sakaiproject.s2u.copyright.tool.components;

import java.util.UUID;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxCheckBox;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

public abstract class CheckBoxColumn<T> extends AbstractColumn<T, String> {

    private final String uuid = UUID.randomUUID().toString().replace("-", "");

    public CheckBoxColumn(IModel<String> displayModel) {
        super(displayModel);
    }

    @Override
    public void populateItem(Item<ICellPopulator<T>> cellItem, String componentId, IModel<T> rowModel) {
        cellItem.add(new CheckPanel(componentId, newCheckBoxModel(rowModel)));
    }

    protected AjaxCheckBox newCheckBox(String id, IModel<Boolean> checkModel) {
        return new AjaxCheckBox("check", checkModel) {
            @Override
            protected void onComponentTag(ComponentTag tag) {
                super.onComponentTag(tag);
                tag.append("data-col-uuid", uuid, " ");
            }

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                //This is an empty method.
            }
        };
    }

    protected abstract IModel<Boolean> newCheckBoxModel(IModel<T> rowModel);

    @Override
    public Component getHeader(String componentId) {
        CheckPanel panel = new CheckPanel(componentId, new Model<>(),uuid);
        return panel;
    }

    private class CheckPanel extends Panel {
        public CheckPanel(String wicketId, IModel<Boolean> checkModel) {
            super(wicketId);
            add(newCheckBox("check", checkModel));
        }

        public CheckPanel(String wicketId, IModel<Boolean> checkModel,String id) {
            super(wicketId);
            AjaxCheckBox aux = new AjaxCheckBox("check", checkModel) {
                @Override
                protected void onComponentTag(ComponentTag tag) {
                    super.onComponentTag(tag);
                    tag.append("data-col-uuid", uuid, " ");
                }

                @Override
                protected void onUpdate(AjaxRequestTarget target) {
                    boolean selectAll = Boolean.parseBoolean(getDefaultModelObject().toString());
                    selectAll(selectAll);
                }
            };
            aux.add(new AttributeModifier("data-header-id", id));
            add(aux);
        }
    }

    public abstract void selectAll(boolean selectAll);
}
