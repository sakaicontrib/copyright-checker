package org.sakaiproject.s2u.copyright.tool.panels;

import java.util.Date;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import org.apache.wicket.ajax.AjaxPreventSubmitBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormSubmitBehavior;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

import org.sakaiproject.s2u.copyright.logic.CopyrightCheckerService;
import org.sakaiproject.s2u.copyright.logic.SakaiProxy;
import org.sakaiproject.s2u.copyright.model.IntellectualPropertyFile;
import org.sakaiproject.s2u.copyright.model.IntellectualPropertyFileDoubt;

@Getter @Setter
@Slf4j
public class HelpIpfPanel extends Panel {

    @SpringBean(name = "org.sakaiproject.s2u.copyright.logic.SakaiProxy")
    protected transient SakaiProxy sakaiProxy;

    @SpringBean(name="org.sakaiproject.s2u.copyright.logic.CopyrightCheckerService")
    protected transient CopyrightCheckerService copyrightCheckerService;

    private TextField<String> supportFileName;
    private TextArea<String> supportTextArea;
    private Label userEmail;
    private IntellectualPropertyFile file;
    private String filename;

    private final String successMsg;
    private final String errorMsg;

    public HelpIpfPanel(String id, FeedbackPanel feedbackPanel) {
        super(id);
        this.setOutputMarkupId(true);

        Form supportForm = new Form("supportForm");

        String currentUser = sakaiProxy.getCurrentUserId();

        supportFileName = new TextField("fileName", Model.of());
        supportTextArea = new TextArea("helpContent", Model.of());
        userEmail = new Label("userEmail", Model.of(sakaiProxy.getCurrentUserEmail()));

        successMsg = new StringResourceModel("modal.help.success").setParameters(sakaiProxy.getCurrentUserEmail()).getObject();
        errorMsg = new StringResourceModel("modal.help.error").getObject();

        Button submitSupportBtn = new Button("submitSupportBtn");

        submitSupportBtn.add(new AjaxFormSubmitBehavior(supportForm, "click") {
            @Override
            protected void onSubmit(AjaxRequestTarget target) {
                try {
                    Date date = new Date();
                    IntellectualPropertyFileDoubt doubt = new IntellectualPropertyFileDoubt();
                    doubt.setSent(date);
                    doubt.setCreated(date);
                    doubt.setUserId(currentUser);
                    doubt.setFile(file);
                    doubt.setMessage(supportTextArea.getDefaultModelObject().toString());
                    copyrightCheckerService.saveIntellectualPropertyFileDoubt(doubt);
                    sakaiProxy.sendSupportEmail(sakaiProxy.getUserEid(currentUser), doubt.getMessage(), file.getFileId(), file.getContext());
                    log.info("EMAIL SENT -> Content: {}", doubt.getMessage());
                    feedbackPanel.success(successMsg);

                } catch (Exception ex) {
                    log.error("Error at onSubmit() :" + ex);
                    feedbackPanel.error(errorMsg);
                }
                target.add(feedbackPanel);
                target.appendJavaScript("$.featherlight.close()");
            }
        });

        supportForm.add(new AjaxPreventSubmitBehavior());
        supportForm.add(supportFileName);
        supportForm.add(supportTextArea);
        supportForm.add(userEmail);
        supportForm.add(submitSupportBtn);
        add(supportForm);
    }
}
