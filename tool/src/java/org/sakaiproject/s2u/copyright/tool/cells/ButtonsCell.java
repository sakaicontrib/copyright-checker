package org.sakaiproject.s2u.copyright.tool.cells;

import java.util.Calendar;
import java.util.Date;

import org.apache.commons.lang3.ArrayUtils;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;

import org.sakaiproject.s2u.copyright.logic.CopyrightCheckerService;
import org.sakaiproject.s2u.copyright.logic.SakaiProxy;
import org.sakaiproject.s2u.copyright.model.IntellectualPropertyFile;
import org.sakaiproject.s2u.copyright.tool.dataproviders.DetachableIpfModel;
import org.sakaiproject.s2u.copyright.tool.panels.HelpIpfPanel;
import org.sakaiproject.s2u.copyright.tool.panels.ManageIpfPanel;

public class ButtonsCell extends Panel {

    @SpringBean(name = "org.sakaiproject.s2u.copyright.logic.SakaiProxy")
    protected transient SakaiProxy sakaiProxy;

    @SpringBean(name = "org.sakaiproject.s2u.copyright.logic.CopyrightCheckerService")
    protected transient CopyrightCheckerService copyrightCheckerService;

    public ButtonsCell(String id) {
        super(id);
    }

    public ButtonsCell(String id, IModel<DetachableIpfModel> model, ManageIpfPanel modalManage, HelpIpfPanel modalHelp) {
        super(id, model);

        AjaxLink manageBtn = new AjaxLink("manageBtn") {
            @Override
            public void onClick(AjaxRequestTarget target) {
                DetachableIpfModel detachableFile = model.getObject();
                IntellectualPropertyFile file = detachableFile.getObject();

                modalManage.getData().setObject(detachableFile);
                modalManage.getDocViewer().setFileUrl(detachableFile.getFileUrl());

                if (checkIfSupportedPreview(detachableFile.getMimeType())) {
                    modalManage.getDocViewer().setVisible(true);
                    modalManage.getNotSupported().setVisible(false);
                    modalManage.getHtmlDisplay().setVisible(false);
                } else {
                    modalManage.getDocViewer().setVisible(false);
                    if (detachableFile.getMimeType().equals("text/plain") || detachableFile.getMimeType().equals("text/html")) {
                        modalManage.getNotSupported().setVisible(false);
                        modalManage.getHtmlDisplay().setVisible(true);

                        modalManage.getPreviewHtml().setDefaultModelObject(
                                sakaiProxy.getFileContentAsString(file.getFileId()) == null ? "" : sakaiProxy.getFileContentAsString(file.getFileId())
                        );

                    } else {
                        modalManage.getNotSupported().setVisible(true);
                        modalManage.getHtmlDisplay().setVisible(false);
                        modalManage.getDownloadFileButton().setDefaultModel(Model.of(detachableFile.getFileUrl()));
                    }
                }
                modalManage.setOriginalState(detachableFile.getObject().getState());
                modalManage.setExternalValues(detachableFile.getFileName(),
                        sakaiProxy.getFileContextName(file.getContext()),
                        copyrightCheckerService.getFileState(file.getProperty(), file.getState(), detachableFile.isDeleted()),
                        file.getProperty(), file.getPerpetual());
                modalManage.showAppropiateFiels(target);
                modalManage.clearFeedback(modalManage.getManagePanelFeedback());

                target.add(modalManage.getChoiceDescription());
                target.add(modalManage);

                Date licenseEndTime = file.getLicenseEndTime();
                String initDatepickerJs = "";
                if (licenseEndTime != null) {
                    Calendar cal = Calendar.getInstance();
                    cal.setTime(licenseEndTime);
                    initDatepickerJs += "initDatepickerManageModal(" + cal.get(Calendar.YEAR) + "," + (cal.get(Calendar.MONTH) + 1) + "," + cal.get(Calendar.DAY_OF_MONTH) + ")";
                } else {
                    initDatepickerJs += "initDatepickerManageModal()";
                }
                target.appendJavaScript("$.featherlight('#resourceModal',{ variant: 'lpiResourceModal', persist: true, afterClose: clearManageModalForm, afterOpen: " + initDatepickerJs + "});");
            }
        };

        AjaxLink helpBtn = new AjaxLink("helpBtn") {
            @Override
            public void onClick(AjaxRequestTarget target) {
                DetachableIpfModel detachableFile = model.getObject();
                IntellectualPropertyFile file = detachableFile.getObject();

                String filename = sakaiProxy.getFileName(file.getFileId());
                modalHelp.getSupportTextArea().getModel().setObject("");
                modalHelp.getSupportFileName().getModel().setObject(filename);
                modalHelp.getSupportTextArea().getModel().setObject("");
                modalHelp.setFilename(filename);
                modalHelp.setFile(file);
                target.add(modalHelp);
                target.appendJavaScript("$.featherlight('#helpModal',{ variant: 'resourceHelpModal', persist: true });");
            }
        };
        helpBtn.setVisible(sakaiProxy.isSupportEnabled());
        add(manageBtn);
        add(helpBtn);
    }

    public boolean checkIfSupportedPreview(String mimeToCheck) {
        String[] allowedMimeTypes = {"application/pdf",
            "application/vnd.oasis.opendocument.text"};
        return ArrayUtils.contains(allowedMimeTypes, mimeToCheck);
    }
}
