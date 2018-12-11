/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.sakaiproject.s2u.copyright.tool.panels;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormChoiceComponentUpdatingBehavior;
import org.apache.wicket.ajax.form.AjaxFormSubmitBehavior;
import org.apache.wicket.event.Broadcast;
import org.apache.wicket.event.IEventSource;
import org.apache.wicket.feedback.FeedbackMessage;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.Radio;
import org.apache.wicket.markup.html.form.RadioGroup;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.image.ContextImage;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.util.string.StringValue;

import org.sakaiproject.s2u.copyright.logic.CopyrightCheckerService;
import org.sakaiproject.s2u.copyright.logic.SakaiProxy;
import org.sakaiproject.s2u.copyright.model.IntellectualPropertyFile;
import org.sakaiproject.s2u.copyright.model.IntellectualPropertyFileProperty;
import org.sakaiproject.s2u.copyright.model.IntellectualPropertyFileState;
import org.sakaiproject.s2u.copyright.model.IntellectualPropertyFileStatus;
import org.sakaiproject.s2u.copyright.model.IntellectualPropertyFileType;
import org.sakaiproject.s2u.copyright.tool.components.PdfViewer;
import org.sakaiproject.s2u.copyright.tool.dataproviders.DetachableIpfModel;
import org.sakaiproject.s2u.copyright.tool.events.RowUpdateEvent;
import org.sakaiproject.s2u.copyright.tool.validators.IntellectualPropertyFileValidator;

@Getter @Setter
@Slf4j
public final class ManageIpfPanel extends Panel implements IEventSource{

    @SpringBean(name = "org.sakaiproject.s2u.copyright.logic.CopyrightCheckerService")
    protected transient CopyrightCheckerService copyrightCheckerService;

    @SpringBean(name = "org.sakaiproject.s2u.copyright.logic.SakaiProxy")
    protected transient SakaiProxy sakaiProxy;

    private IModel<DetachableIpfModel> data = new Model();

    private List<String> licenseTypes = new ArrayList(8);

    private final Label fileNameLbl;
    private final Label contextLbl;
    private final Label stateLbl;
    private final PdfViewer docViewer;
    private final RadioGroup radioChoices;
    private ListView<String> choices;
    private final Label choiceDescription;
    private final WebMarkupContainer modalForm;
    private final TextField licenseEnd;
    private final CheckBox perpetualLicense;
    private final TextField title;
    private final TextField<String> identification;
    private final TextField<String> author;
    private final TextField<String> license;
    private final TextField<String> publisher;
    private final TextField<String> pages;
    private final TextField<String> totalPages;
    private final TextArea<String> comments;
    private final WebMarkupContainer halfRight;
    private final FeedbackPanel managePanelFeedback;
    private final WebMarkupContainer datePickerContainer;
    private final WebMarkupContainer extendedForm;
    private final WebMarkupContainer perpetualContainer;
    private final Form licenseForm;
    private final Button manageSubmitBtn;
    private final WebMarkupContainer licenseContainer;

    private final String successMsg;
    private final WebMarkupContainer notSupported;
    private final WebMarkupContainer htmlDisplay;
    private final ContextImage unavaliableIcon;
    private final ExternalLink downloadFileButton;
    private final Label previewHtml;

    private int originalState;

    private static final DateTimeFormatter isoFormatter = DateTimeFormatter.ISO_ZONED_DATE_TIME;

    public ManageIpfPanel(String id, FeedbackPanel feedbackPanel) {
        super(id);
        this.setOutputMarkupId(true);

        successMsg = new StringResourceModel("modal.manage.success").getObject();

        for (int i = 0; i < 8; i++) {
            licenseTypes.add(new ResourceModel("modal.radio.choice.description." + i).getObject());
        }

        halfRight = new WebMarkupContainer("halfRight");
        halfRight.setOutputMarkupId(true);
        add(halfRight);

        choices = new ListView("choices", licenseTypes) {
            @Override
            protected void populateItem(ListItem li) {
                li.add(new Radio("radio", new Model(li.getIndex())));
                li.add(new Label("type", new ResourceModel("modal.radio.choice." + li.getIndex())));
            }
        };

        stateLbl = new Label("stateLbl", Model.of());
        add(stateLbl);

        docViewer = new PdfViewer("docViewer");
        docViewer.setOutputMarkupId(true);
        add(docViewer);

        notSupported = new WebMarkupContainer("notSupported");
        notSupported.setOutputMarkupId(true);
        add(notSupported);

        unavaliableIcon = new ContextImage("unavaliableIcon","images/unavaliable_icon.png");
        notSupported.add(unavaliableIcon);

        downloadFileButton = new ExternalLink("downloadFileButton",Model.of(""));
        notSupported.add(downloadFileButton);

        htmlDisplay = new WebMarkupContainer("htmlDisplay");

        previewHtml = new Label("previewHtml");
        previewHtml.setDefaultModel(Model.of(""));

        add(htmlDisplay);
        htmlDisplay.add(previewHtml);

        fileNameLbl = new Label("fileNameLbl", Model.of());
        halfRight.add(fileNameLbl);

        contextLbl = new Label("contextLbl", data.getObject() != null ? Model.of(data.getObject().getFileName()) : "");
        contextLbl.setOutputMarkupId(true);
        halfRight.add(contextLbl);

        licenseForm = new Form("licenseForm", new Model());
        licenseForm.setOutputMarkupId(true);
        halfRight.add(licenseForm);

        // Add a FeedbackPanel for displaying our messages
        managePanelFeedback = new FeedbackPanel("managePanelFeedback"){
            @Override
            protected Component newMessageDisplayComponent(final String id, final FeedbackMessage message) {
                final Component newMessageDisplayComponent = super.newMessageDisplayComponent(id, message);

                if(message.getLevel() == FeedbackMessage.ERROR ||
                    message.getLevel() == FeedbackMessage.DEBUG ||
                    message.getLevel() == FeedbackMessage.FATAL ||
                    message.getLevel() == FeedbackMessage.WARNING){
                    add(AttributeModifier.replace("class", "alertMessage"));
                } else if(message.getLevel() == FeedbackMessage.INFO){
                    add(AttributeModifier.replace("class", "success"));
                }

                return newMessageDisplayComponent;
            }
        };
        managePanelFeedback.setOutputMarkupId(true);
        licenseForm.add(managePanelFeedback);

        modalForm = new WebMarkupContainer("modalForm");
        modalForm.setOutputMarkupId(true);
        modalForm.setOutputMarkupPlaceholderTag(true);
        licenseForm.add(modalForm);

        radioChoices = new RadioGroup("radioChoices", Model.of());
        radioChoices.add(choices);
        radioChoices.add(new AjaxFormChoiceComponentUpdatingBehavior() {
            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                if (!choiceDescription.isVisible()) {
                    choiceDescription.setVisible(true);
                }
                showAppropiateFiels(target);
                target.add(choiceDescription);
            }
        });
        licenseForm.add(radioChoices);

        choiceDescription = new Label("choiceDescription", new ResourceModel("modal.radio.choice.description.0"));
        choiceDescription.setOutputMarkupId(true);
        choiceDescription.setOutputMarkupPlaceholderTag(true);
        licenseForm.add(choiceDescription);

        datePickerContainer = new WebMarkupContainer("datePickerContainer");
        datePickerContainer.setOutputMarkupId(true);
        datePickerContainer.setOutputMarkupPlaceholderTag(true);
        modalForm.add(datePickerContainer);

        perpetualContainer = new WebMarkupContainer("perpetualContainer");
        perpetualContainer.setOutputMarkupId(true);
        perpetualContainer.setOutputMarkupPlaceholderTag(true);
        modalForm.add(perpetualContainer);

        perpetualLicense = new CheckBox("perpetualLicense", new PropertyModel(data, "perpetual"));
        perpetualContainer.add(perpetualLicense);

        licenseEnd = new TextField("licenseEnd", new Model());
        licenseEnd.setOutputMarkupId(true);
        datePickerContainer.add(licenseEnd);

        extendedForm = new WebMarkupContainer("extendedForm");
        extendedForm.setOutputMarkupId(true);
        extendedForm.setOutputMarkupPlaceholderTag(true);
        licenseForm.add(extendedForm);

        title = new TextField("title", new PropertyModel(data, "title"));
        title.setOutputMarkupId(true);
        title.setOutputMarkupPlaceholderTag(true);
        extendedForm.add(title);

        identification = new TextField("identification", new PropertyModel(data, "identification"));
        identification.setOutputMarkupId(true);
        identification.setOutputMarkupPlaceholderTag(true);
        extendedForm.add(identification);

        author = new TextField("author", new PropertyModel(data, "author"));
        author.setOutputMarkupId(true);
        author.setOutputMarkupPlaceholderTag(true);
        extendedForm.add(author);

        licenseContainer = new WebMarkupContainer("licenseContainer");
        licenseContainer.setOutputMarkupId(true);
        licenseContainer.setOutputMarkupPlaceholderTag(true);
        extendedForm.add(licenseContainer);

        license = new TextField("license", new PropertyModel(data, "license"));
        license.setOutputMarkupId(true);
        license.setOutputMarkupPlaceholderTag(true);
        licenseContainer.add(license);

        publisher = new TextField("publisher", new PropertyModel(data, "publisher"));
        publisher.setOutputMarkupId(true);
        publisher.setOutputMarkupPlaceholderTag(true);
        extendedForm.add(publisher);

        pages = new TextField("pages", new PropertyModel(data, "pages"));
        pages.setOutputMarkupId(true);
        pages.setOutputMarkupPlaceholderTag(true);
        extendedForm.add(pages);

        totalPages = new TextField("totalPages", new PropertyModel(data, "totalPages"));
        totalPages.setOutputMarkupId(true);
        totalPages.setOutputMarkupPlaceholderTag(true);
        extendedForm.add(totalPages);

        comments = new TextArea("comments", new PropertyModel(data, "comments"));
        comments.setOutputMarkupId(true);
        comments.setOutputMarkupPlaceholderTag(true);
        extendedForm.add(comments);

        manageSubmitBtn = new Button("manageSubmitBtn");
        manageSubmitBtn.setOutputMarkupId(true);
        add(manageSubmitBtn);

        manageSubmitBtn.add(new AjaxFormSubmitBehavior(licenseForm, "click") {
            @Override
            protected void onSubmit(AjaxRequestTarget target) {
                IntellectualPropertyFile file = data.getObject().getObject();
                StringValue aux = getRequest().getRequestParameters().getParameterValue("hiddenManageModalContainer:halfRight:licenseForm:modalForm:datePickerContainer:licenseEnd");
                String dateFieldValue = aux.toString("");
                IntellectualPropertyFileValidator validator = new IntellectualPropertyFileValidator(
                        target,
                        managePanelFeedback,
                        radioChoices.getDefaultModel(),
                        identification.getDefaultModel(),
                        pages.getDefaultModel(),
                        totalPages.getDefaultModel(),
                        getRequest().getRequestParameters().getParameterValue("date_ISO8601").toString(""),
                        dateFieldValue,
                        file.getPerpetual()
                );
                
                if (validator.validate()) {
                    Date modifiedDate = new Date();
                    file.setProperty((int) radioChoices.getDefaultModelObject());

                if (file.getPerpetual() !=  null && !file.getPerpetual()) {
                    String dateUnformatted = RequestCycle.get().getRequest().getPostParameters().getParameterValue("date_ISO8601").toString();
                    Date dateFormatted = parseISODate(dateUnformatted);
                    file.setLicenseEndTime(dateFormatted);
                }
                
                    switch (file.getProperty()) {
                        case IntellectualPropertyFileProperty.NONE:
                            file.setState(IntellectualPropertyFileState.OK);
                            file.setType(IntellectualPropertyFileType.NOT_PRINTED_OR_PRINTABLE);
                            break;
                        case IntellectualPropertyFileProperty.ADMINISTRATIVE:
                            file.setState(IntellectualPropertyFileState.OK);
                            file.setType(IntellectualPropertyFileType.ADMINISTRATIVE);
                            break;
                        case IntellectualPropertyFileProperty.TEACHERS:
                        case IntellectualPropertyFileProperty.UNIVERSITY:
                        case IntellectualPropertyFileProperty.PUBLIC_DOMAIN:
                            file.setState(IntellectualPropertyFileState.OK);
                            file.setType(IntellectualPropertyFileType.PRINTED_OR_PRINTABLE);
                            break;
                        case IntellectualPropertyFileProperty.FRAGMENT:
                            file.setState(IntellectualPropertyFileState.GT10);
                            break;
                        case IntellectualPropertyFileProperty.FULL:
                            file.setState(IntellectualPropertyFileState.GT10PERM);
                            break;
                        default:
                            break;
                    }
                    file.setModified(modifiedDate);
                    if(file.getProperty() != IntellectualPropertyFileProperty.MINE){
                        file.setPerpetual(null);
                    }

                    copyrightCheckerService.saveIntellectualPropertyFile(file);

                    if(originalState == IntellectualPropertyFileStatus.NOT_AUTHORIZED &&
                        file.getState() != IntellectualPropertyFileStatus.NOT_AUTHORIZED && 
                        file.getState() != IntellectualPropertyFileState.GT10PERM &&
                        sakaiProxy.setContentResourceVisibility(file.getFileId(), true)){
                            data.getObject().setHidden(false);
                    }

                    send(getPage(), Broadcast.BREADTH, new RowUpdateEvent(target, file, !data.getObject().isHidden()));
                    target.appendJavaScript("$.featherlight.close()");

                    clearFeedback(managePanelFeedback);
                    feedbackPanel.success(successMsg);
                    target.add(feedbackPanel);

                } else {
                    target.add(managePanelFeedback);
                }
            }
        });

        hideAllFields(null);
    }

    @Override
    public void renderHead(IHeaderResponse response) {
        super.renderHead(response);
        response.render(JavaScriptHeaderItem.forUrl("/copyright-checker-tool/js/documentManagement.js"));
        response.render(JavaScriptHeaderItem.forUrl("/copyright-checker-tool/js/manageLpiPanel.js"));
        response.render(JavaScriptHeaderItem.forUrl("/library/webjars/jquery-ui/1.11.3/jquery-ui.min.js"));
        response.render(JavaScriptHeaderItem.forUrl("/library/js/lang-datepicker/lang-datepicker.js"));
    }

    public void showAppropiateFiels(AjaxRequestTarget target) {
        if (radioChoices.getDefaultModelObject() != null) {

            int selectedIndex = Integer.parseInt(radioChoices.getDefaultModelObject().toString());
            choiceDescription.setDefaultModel(new ResourceModel("modal.radio.choice.description." + selectedIndex));
            switch (selectedIndex) {
                case IntellectualPropertyFileProperty.NONE:
                case IntellectualPropertyFileProperty.ADMINISTRATIVE:
                case IntellectualPropertyFileProperty.TEACHERS:
                case IntellectualPropertyFileProperty.UNIVERSITY:
                case IntellectualPropertyFileProperty.PUBLIC_DOMAIN:
                    hideAllFields(target);
                    break;
                case IntellectualPropertyFileProperty.MINE:
                    showAllFieldsForAuthoredLicense(target);
                    break;
                case IntellectualPropertyFileProperty.FRAGMENT:
                    showAllFieldsForLoe10(target);
                    break;
                case IntellectualPropertyFileProperty.FULL:
                    showAllFieldsForGt10(target);
                    break;
                default:
                    break;
            }
        }
    }

    public void showAllFieldsForAuthoredLicense(AjaxRequestTarget target) {
        perpetualContainer.setVisible(true);
        extendedForm.setVisible(true);
        licenseContainer.setVisible(true);
        target.add(perpetualContainer, extendedForm, licenseContainer);
        showDatepicker(target);
    }

    private void showDatepicker(AjaxRequestTarget target) {
        Date licenseEndTime = data.getObject().getObject().getLicenseEndTime();
        String initDatepickerJs = "";
        if (licenseEndTime != null) {
            Calendar cal = Calendar.getInstance();
            cal.setTime(licenseEndTime);
            initDatepickerJs += "initDatepickerManageModal(" + cal.get(Calendar.YEAR) + "," + (cal.get(Calendar.MONTH) + 1) + "," + cal.get(Calendar.DAY_OF_MONTH) + ");";
        } else {
            initDatepickerJs += "initDatepickerManageModal();";
        }
        target.appendJavaScript(initDatepickerJs);
        boolean licenseEnabled = true;
        boolean radioMine = (int) radioChoices.getDefaultModelObject() == IntellectualPropertyFileProperty.MINE;
        boolean perpertual = data.getObject().getObject().getPerpetual() != null && data.getObject().getObject().getPerpetual();
        if(radioMine && perpertual){
            licenseEnabled = false;
        }
        licenseEnd.setEnabled(licenseEnabled);
        datePickerContainer.setVisible(true);
        target.add(datePickerContainer, licenseEnd);
    }

    public void showAllFieldsForLoe10(AjaxRequestTarget target) {
        perpetualContainer.setVisible(false);
        extendedForm.setVisible(true);
        licenseContainer.setVisible(false);
        showDatepicker(target);
        target.add(perpetualContainer, extendedForm, licenseContainer);
    }

    public void showAllFieldsForGt10(AjaxRequestTarget target) {
        perpetualContainer.setVisible(false);
        extendedForm.setVisible(true);
        licenseContainer.setVisible(false);
        showDatepicker(target);
        target.add(perpetualContainer, extendedForm, licenseContainer);
    }

    public void hideAllFields(AjaxRequestTarget target) {
        datePickerContainer.setVisible(false);
        perpetualContainer.setVisible(false);
        extendedForm.setVisible(false);
        licenseContainer.setVisible(false);
        if (target != null) {
            target.add(datePickerContainer, perpetualContainer, extendedForm, licenseContainer);
        }
    }

    public void setExternalValues(String fileName, String context, int state, int property, Boolean perpetual) {
        fileNameLbl.setDefaultModel(Model.of(fileName));
        contextLbl.setDefaultModel(Model.of(context));
        stateLbl.setDefaultModel(new StringResourceModel("lpi.file.state." + state));

        // If no property (-1), set it to 0
        int newProperty = (property == IntellectualPropertyFileProperty.NOT_PROCESSED) ? IntellectualPropertyFileProperty.NONE : property;
        radioChoices.setDefaultModelObject(newProperty);

        if (perpetual != null && newProperty == IntellectualPropertyFileProperty.MINE) licenseEnd.setEnabled(!perpetual);
    }

    public Date parseISODate(final String inputDate) {
        Date convertedDate = null;

        try {
            LocalDateTime ldt = LocalDateTime.parse(inputDate, isoFormatter);
            convertedDate = Date.from(ldt.atZone(ZoneId.systemDefault()).toInstant());
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }

        return convertedDate;
    }

    public void clearFeedback(FeedbackPanel f) {
        if(!f.hasFeedbackMessage()) {
            f.add(AttributeModifier.replace("class", ""));
        }
    }
}
