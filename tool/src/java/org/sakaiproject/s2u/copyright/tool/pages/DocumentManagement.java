package org.sakaiproject.s2u.copyright.tool.pages;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.event.IEvent;
import org.apache.wicket.extensions.ajax.markup.html.repeater.data.table.AjaxNavigationToolbar;
import org.apache.wicket.extensions.markup.html.form.select.SelectOption;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractToolbar;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DefaultDataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.NavigationToolbar;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.export.CSVDataExporter;
import org.apache.wicket.extensions.markup.html.repeater.data.table.export.ExportToolbar;
import org.apache.wicket.extensions.markup.html.repeater.data.table.filter.ChoiceFilter;
import org.apache.wicket.extensions.markup.html.repeater.data.table.filter.ChoiceFilteredPropertyColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.filter.FilterForm;
import org.apache.wicket.extensions.markup.html.repeater.data.table.filter.FilterToolbar;
import org.apache.wicket.extensions.markup.html.repeater.data.table.filter.FilteredAbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.filter.TextFilter;
import org.apache.wicket.extensions.markup.html.repeater.data.table.filter.TextFilteredPropertyColumn;
import org.apache.wicket.extensions.model.AbstractCheckBoxModel;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.navigation.paging.PagingNavigator;
import org.apache.wicket.markup.repeater.ReuseIfModelsEqualStrategy;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import org.sakaiproject.s2u.copyright.model.IntellectualPropertyFile;
import org.sakaiproject.s2u.copyright.tool.cells.ButtonsCell;
import org.sakaiproject.s2u.copyright.tool.components.CheckBoxColumn;
import org.sakaiproject.s2u.copyright.tool.components.FilterActionsHeader;
import org.sakaiproject.s2u.copyright.tool.cells.FileNameCell;
import org.sakaiproject.s2u.copyright.tool.components.TablePagingNavigator;
import org.sakaiproject.s2u.copyright.tool.events.RowUpdateEvent;
import org.sakaiproject.s2u.copyright.tool.dataproviders.DetachableIpfModel;
import org.sakaiproject.s2u.copyright.tool.dataproviders.SortableIpfDataProvider;
import org.sakaiproject.s2u.copyright.tool.panels.HelpIpfPanel;
import org.sakaiproject.s2u.copyright.tool.panels.ManageIpfPanel;

public class DocumentManagement extends BasePage {

    private static final int DEFAULT_TABLE_ROWS_NUMBER = 10;

    private static final Integer[] tableRowsOptions = {10, 20, 50, 100};

    private final ManageIpfPanel modalManagePanel;
    private final HelpIpfPanel modalHelpPanel;
    private final SortableIpfDataProvider dataProvider;
    private final List visibilityFilterList;
    private final List stateFilterList;

    private int tableRowsNumber = DEFAULT_TABLE_ROWS_NUMBER;
    private int dropdownSelected = 0;
    private Set<DetachableIpfModel> selected = new HashSet();

    public DocumentManagement(PageParameters parameters) {
        List<IColumn<DetachableIpfModel, String>> columns = new ArrayList<>();
        dataProvider = new SortableIpfDataProvider();

        // MODAL - panel that manages the copyright status of a file and shows a preview
        modalManagePanel = new ManageIpfPanel("hiddenManageModalContainer", feedbackPanel);
        modalManagePanel.setOutputMarkupId(true);

        // MODAL - panel used to send doubts about the copyright
        modalHelpPanel = new HelpIpfPanel("hiddenHelpModalContainer", feedbackPanel);

        // COLUMN - the first column of the table, contains the checkboxes for mass actions
        CheckBoxColumn<DetachableIpfModel> bulkColumn = new CheckBoxColumn(Model.of()) {
            @Override
            protected IModel newCheckBoxModel(final IModel rowModel) {
                return new AbstractCheckBoxModel() {
                    @Override
                    public boolean isSelected() {
                        return selected.contains((DetachableIpfModel) rowModel.getObject());
                    }

                    @Override
                    public void unselect() {
                        selected.remove((DetachableIpfModel) rowModel.getObject());
                    }

                    @Override
                    public void select() {
                        selected.add((DetachableIpfModel) rowModel.getObject());
                    }

                    @Override
                    public void detach() {
                        rowModel.detach();
                    }
                };
            }

            @Override
            public String getCssClass() {
                return "text-center";
            }

            @Override
            public void selectAll(boolean selectAll) {
                List<DetachableIpfModel> list = dataProvider.getFilteredList();
                if (selectAll) {
                    if (list != null) {
                        selected.addAll(list);
                    }
                } else {
                    selected.clear();
                }
            }
        };
        columns.add(bulkColumn);

        // COLUMN - the second column of the table, contains the modified date of the lpiFile
        columns.add(new PropertyColumn<DetachableIpfModel, String>(new ResourceModel("lpi.table.column.0"), "modified", "modified") {
            @Override
            public void populateItem(Item<ICellPopulator<DetachableIpfModel>> cellItem, String componentId, IModel<DetachableIpfModel> model) {
                Label aux;
                aux = new Label(componentId, model.getObject().getObject().getModified().toInstant().toString()) {
                    @Override
                    public void onEvent(IEvent<?> event) {
                        if (event.getPayload() instanceof RowUpdateEvent) {
                            RowUpdateEvent eventPayload = (RowUpdateEvent) event.getPayload();
                            Model displayText = Model.of(eventPayload.getPayload().getModified().toInstant());
                            if (model.getObject().getId() == eventPayload.getPayload().getId()) {
                                this.setDefaultModel(displayText);
                                eventPayload.getTarget().add(this);
                                eventPayload.getTarget().appendJavaScript("$('#"+this.getMarkupId()+"').text(moment($('#"+this.getMarkupId()+"').text()).locale(portal.locale).format('LLLL')).removeClass('hidden');");
                            }
                        }
                    }
                };
                aux.add(new AttributeModifier("class", "modifiedDate hidden"));
                aux.setOutputMarkupId(true);
                cellItem.add(aux);
            }

            @Override
            public String getCssClass() {
                return "hidden-xs";
            }
        });

        // COLUMN - the third column of the table, contains the filename from sakai resources
        columns.add(new TextFilteredPropertyColumn<DetachableIpfModel, String, String>(new ResourceModel("lpi.table.column.1"), "fileName", "fileName") {
            @Override
            public void populateItem(Item<ICellPopulator<DetachableIpfModel>> cellItem, String componentId, IModel<DetachableIpfModel> model) {
                cellItem.add(new FileNameCell(componentId, model.getObject().getFileUrl(), model.getObject().getFileName()));
            }

            @Override
            public IModel<?> getDataModel(IModel<DetachableIpfModel> imodel) {
                return Model.of(imodel.getObject().getFileName());
            }

            @Override
            public Component getFilter(String componentId, FilterForm<?> form) {
                TextFilter aux = (TextFilter) super.getFilter(componentId, form);
                aux.getFilter().add(new AttributeModifier("class", "form-control"));
                aux.getFilter().add(new AttributeModifier("placeholder", new ResourceModel("placeholder.search")));
                return aux;
            }

        });

        // COLUMN - the fourth column of the table, contains the sakai site where the file was uploaded
        columns.add(new TextFilteredPropertyColumn<DetachableIpfModel, String, String>(new ResourceModel("lpi.table.column.2"), "context", "context") {
            @Override
            public void populateItem(Item<ICellPopulator<DetachableIpfModel>> cellItem, String componentId, IModel<DetachableIpfModel> model) {
                cellItem.add(new Label((componentId), Model.of(model.getObject().getDisplayContext())));
            }

            @Override
            public String getCssClass() {
                return "hidden-xs";
            }

            @Override
            public Component getFilter(String componentId, FilterForm<?> form) {
                TextFilter aux = (TextFilter) super.getFilter(componentId, form);
                aux.getFilter().add(new AttributeModifier("class", "form-control"));
                aux.getFilter().add(new AttributeModifier("placeholder", new ResourceModel("placeholder.search")));
                return aux;
            }

            @Override
            public IModel<?> getDataModel(IModel<DetachableIpfModel> model) {
                return Model.of(model.getObject().getDisplayContext());
            }

        });

        // COLUMN - the fifth column of the table, contains the copyright status
        stateFilterList = Arrays.asList(new SelectOption[] {
            new SelectOption("0", Model.of(new ResourceModel("lpi.file.state.0").getObject())),
            new SelectOption("1", Model.of(new ResourceModel("lpi.file.state.1").getObject())),
            new SelectOption("2", Model.of(new ResourceModel("lpi.file.state.2").getObject())),
            new SelectOption("3", Model.of(new ResourceModel("lpi.file.state.3").getObject())),
            new SelectOption("4", Model.of(new ResourceModel("lpi.file.state.4").getObject())),
        });
        columns.add(new ChoiceFilteredPropertyColumn<DetachableIpfModel, String, String>(new ResourceModel("lpi.table.column.3"), "displayStatus", "displayStatus", Model.ofList(stateFilterList)) {
            @Override
            public void populateItem(Item<ICellPopulator<DetachableIpfModel>> cellItem, String componentId, IModel<DetachableIpfModel> model) {
                Label aux;
                aux = new Label(componentId, new StringResourceModel("lpi.file.state." + model.getObject().getDisplayStatus(), cellItem, model)) {
                    @Override
                    public void onEvent(IEvent<?> event) {
                        if (event.getPayload() instanceof RowUpdateEvent) {
                            RowUpdateEvent eventPayload = (RowUpdateEvent) event.getPayload();
                            DetachableIpfModel detachableModel = new DetachableIpfModel(eventPayload.getPayload());
                            ResourceModel displayText = new ResourceModel("lpi.file.state." + detachableModel.getDisplayStatus());
                            if (model.getObject().getId() == eventPayload.getPayload().getId()) {
                                this.setDefaultModel(displayText);
                                this.add(new AttributeModifier("title", new ResourceModel("modal.radio.choice." + detachableModel.getObject().getProperty() + ".title")));
                                eventPayload.getTarget().add(this);
                            }
                        }
                    }
                };
                aux.add(new AttributeModifier("title", new ResourceModel("modal.radio.choice." + model.getObject().getObject().getProperty() + ".title")));
                aux.setOutputMarkupId(true);
                cellItem.add(aux);
            }

            @Override
            public String getCssClass() {
                return "hidden-xs text-center";
            }

            @Override
            public Component getFilter(String componentId, FilterForm<?> form) {
                ChoiceFilter filter = new ChoiceFilter(componentId, getFilterModel(form), form,
                        stateFilterList, enableAutoSubmit());
                IChoiceRenderer renderer = new ChoiceRenderer("getDefaultModelObject()");
                filter.getChoice().setChoiceRenderer(renderer);
                return filter;
            }

            @Override
            public IModel<?> getDataModel(IModel<DetachableIpfModel> imodel) {
                int stateValue = imodel.getObject().getDisplayStatus();
                return new StringResourceModel("lpi.file.state." + stateValue);
            }
        });
        // COLUMN - the sixth column of the table, contains the sakai resource visibility state
        visibilityFilterList = Arrays.asList(new SelectOption[]{
            new SelectOption("1", Model.of(new ResourceModel("lpi.visibility.filter.true").getObject())),
            new SelectOption("2", Model.of(new ResourceModel("lpi.visibility.filter.false").getObject())),});
        columns.add(new ChoiceFilteredPropertyColumn<DetachableIpfModel, String, String>(new ResourceModel("lpi.table.column.4"), "visibility", "visibility", Model.ofList(visibilityFilterList)) {
            @Override
            public void populateItem(Item<ICellPopulator<DetachableIpfModel>> cellItem, String componentId, IModel<DetachableIpfModel> model) {
                boolean isVisible = !model.getObject().isHidden();
                boolean isDeleted = model.getObject().isDeleted();
                Label aux;
                ResourceModel auxTextY =new ResourceModel("lpi.yes");
                ResourceModel auxTextN =new ResourceModel("lpi.no");
                aux = new Label(componentId, isVisible && !isDeleted ? auxTextY : auxTextN){
                    @Override
                    public void onEvent(IEvent<?> event) {
                        if (event.getPayload() instanceof RowUpdateEvent) {
                            RowUpdateEvent eventPayload = (RowUpdateEvent) event.getPayload();
                            boolean visible = eventPayload.isVisible();
                            if (model.getObject().getId() == eventPayload.getPayload().getId()) {
                                this.setDefaultModel(visible && !isDeleted ? auxTextY : auxTextN );
                                eventPayload.getTarget().add(this);
                            }
                        }
                    }
                };
                aux.setOutputMarkupId(true);
                aux.setEscapeModelStrings(false);
                cellItem.add(aux);
            }

            @Override
            public String getCssClass() {
                return "hidden-xs text-center";
            }

            @Override
            public Component getFilter(String componentId, FilterForm<?> form) {
                ChoiceFilter filter = new ChoiceFilter(componentId, getFilterModel(form), form,
                        visibilityFilterList, enableAutoSubmit());
                IChoiceRenderer renderer = new ChoiceRenderer("getDefaultModelObject()");
                filter.getChoice().setChoiceRenderer(renderer);
                return filter;
            }

            @Override
            public IModel<?> getDataModel(IModel<DetachableIpfModel> imodel) {
                return Model.of(!imodel.getObject().isHidden());
            }
        });

        // COLUMN - the sixth column of the table, contains the buttons to perform actions on the item
        columns.add(new FilteredAbstractColumn<DetachableIpfModel, String>(new ResourceModel("lpi.table.column.5")) {
            @Override
            public void populateItem(Item<ICellPopulator<DetachableIpfModel>> cellItem, String componentId, IModel<DetachableIpfModel> model) {
                cellItem.add(new ButtonsCell(componentId, model, modalManagePanel, modalHelpPanel));
            }

            @Override
            public String getCssClass() {
                return "col-actions text-center";
            }

            @Override
            public Component getFilter(String componentId, FilterForm<?> ff) {
                return new FilterActionsHeader(componentId);
            }
        });

        WebMarkupContainer tableContainer = new WebMarkupContainer("tableContainer");
        add(tableContainer);

        String rowsNumber = parameters.get("rowsNumber").toString();
        int rowsNumberSelected = DEFAULT_TABLE_ROWS_NUMBER;
        if (rowsNumber != null ) {
            tableRowsNumber = Integer.parseInt(rowsNumber);
            rowsNumberSelected = tableRowsNumber;
        }

        List<Integer> optionsList = Arrays.asList(tableRowsOptions);
        DropDownChoice rowsNumberDropdown = new DropDownChoice("rowsNumberDropdown", Model.of(), optionsList);
        DataTable<DetachableIpfModel, String> dataTable = new DefaultDataTable("documentsTable", columns, dataProvider, tableRowsNumber) {
            @Override
            public void addTopToolbar(AbstractToolbar toolbar) {
                if (toolbar instanceof NavigationToolbar) {
                    super.addTopToolbar(new AjaxNavigationToolbar(this) {
                        @Override
                        protected PagingNavigator newPagingNavigator(String navigatorId, DataTable<?, ?> table) {
                            return new TablePagingNavigator(navigatorId, table);
                        }
                    });

                } else {
                    super.addTopToolbar(toolbar); //To change body of generated methods, choose Tools | Templates.
                }
            }
        };
        rowsNumberDropdown.setDefaultModelObject(rowsNumberSelected);
        tableContainer.add(dataTable);

        dataTable.addBottomToolbar(new ExportToolbar(dataTable).addDataExporter(new CSVDataExporter()));
        dataTable.setItemReuseStrategy(new ReuseIfModelsEqualStrategy());

        FilterForm filterForm = new FilterForm("filterForm", dataProvider);
        filterForm.add(tableContainer);

        // Table rows selector
        AjaxFormComponentUpdatingBehavior rowsNumberDropdownEvent = new AjaxFormComponentUpdatingBehavior("change") {
            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                PageParameters parameters = new PageParameters();
                parameters.set("rowsNumber", rowsNumberDropdown.getModelObject());
                setResponsePage(DocumentManagement.class, parameters);
            }
        };
        rowsNumberDropdown.add(rowsNumberDropdownEvent);
        filterForm.add(rowsNumberDropdown);

        FilterToolbar filterToolbar = new FilterToolbar(dataTable, filterForm);
        dataTable.addTopToolbar(filterToolbar);

        List<String> bulkActionsOptions = new ArrayList<>();
        for (int i = 0; i <= 4; i++) {
            bulkActionsOptions.add(new ResourceModel("modal.radio.choice." + i).getObject());
        }
        DropDownChoice bulkActionsDropdown = new DropDownChoice("bulkActionsDropdown", Model.of(), bulkActionsOptions);
        bulkActionsDropdown.setDefaultModelObject(0);
        AjaxFormComponentUpdatingBehavior bulkActionsEvent = new AjaxFormComponentUpdatingBehavior("change") {
            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                dropdownSelected = Integer.parseInt(bulkActionsDropdown.getModelValue());
            }
        };
        bulkActionsDropdown.add(bulkActionsEvent);

        Button bulkActionsSubmit = new Button("bulkActionsSubmit") {
            @Override
            public void onSubmit() {
                int property = dropdownSelected;
                Date modifiedDate = new Date();
                for (DetachableIpfModel detachableFile : selected) {
                    IntellectualPropertyFile file = detachableFile.getObject();
                    file.setProperty(property);
                    file.setModified(modifiedDate);
                    copyrightCheckerService.saveIntellectualPropertyFile(file);
                }
                setResponsePage(DocumentManagement.class);
            }
        };
        bulkActionsSubmit.setDefaultFormProcessing(false);

        filterForm.add(modalManagePanel);
        filterForm.add(modalHelpPanel);
        filterForm.add(bulkActionsDropdown);
        filterForm.add(bulkActionsSubmit);
        add(filterForm);
    }

    @Override
    public void renderHead(IHeaderResponse response) {
        super.renderHead(response);
        response.render(JavaScriptHeaderItem.forUrl("/library/webjars/momentjs/2.11.1/min/moment-with-locales.min.js"));
        response.render(CssHeaderItem.forUrl("/copyright-checker-tool/css/copyright-checker.css"));
    }

    @Override
    protected void onRender() {
        super.onRender();
        selected.clear();
    }

    @Override
    protected void onBeforeRender() {
        super.onBeforeRender();
        feedbackPanel.getFeedbackMessages().clear();
        clearFeedback(feedbackPanel);
    }
}
