package org.sakaiproject.s2u.copyright.tool.validators;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.ResourceModel;

import org.sakaiproject.s2u.copyright.model.IntellectualPropertyFileProperty;

@AllArgsConstructor
@RequiredArgsConstructor
@Slf4j
public class IntellectualPropertyFileValidator {

    private final AjaxRequestTarget target;
    private final FeedbackPanel feedback;
    private final IModel property;
    private final IModel identification;
    private final String licenseISO;
    private final String licenseFieldText;
    private final Boolean perpetualLicense;

    private boolean valid = true;
    private static  DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_ZONED_DATE_TIME;

    public boolean validate() {
        switch ((int) property.getObject()) {
            case IntellectualPropertyFileProperty.MINE:
                checkIdentification();
                checkLicenseEnd(licenseISO, true);
                break;
            case IntellectualPropertyFileProperty.FRAGMENT:
            case IntellectualPropertyFileProperty.FULL:
                checkIdentification();
                checkLicenseEnd(licenseISO, false);
                break;
            default:
                break;
        }
        return this.valid;
    }

    public Date parseISODate(final String inputDate) {
        Date convertedDate = null;

        try {
            LocalDateTime ldt = LocalDateTime.parse(inputDate, ISO_FORMATTER);
            convertedDate = Date.from(ldt.atZone(ZoneId.systemDefault()).toInstant());
        } catch (Exception e) {
            log.error("Cannot parse date");
        }
        return convertedDate;
    }

    /**
     * Check if the identification is null
     * @return true if identification is valid
     */
    public boolean checkIdentification() {
        if (StringUtils.isBlank((String) identification.getObject())) {
            feedback.error(new ResourceModel("validation.identification").getObject());
            target.appendJavaScript("markInputError('identification');");
            this.valid = false;
        }
        return this.valid;
    }

    /**
     * Check if the license is not null, and optionally, if is not in the past
     * @param licenseISO license in iso date
     * @param checkBefore check if date shouldn't be in the past
     * @return true the license is valid
     */
    public boolean checkLicenseEnd(String licenseISO, boolean checkBefore) {
        Date licenseDate;
        if(perpetualLicense != null && perpetualLicense == false){            
            if(StringUtils.isNotEmpty(licenseFieldText) && StringUtils.isNotEmpty(licenseISO)){
                licenseDate = parseISODate(licenseISO);
                if(checkBefore && licenseDate != null && licenseDate.before(new Date())) {
                    feedback.error(new ResourceModel("validation.licenseEnd").getObject());
                    target.appendJavaScript("markInputError('datePickerContainer');");
                    this.valid = false;
                }
            }else {
                feedback.error(new ResourceModel("validation.licenseEnd.null").getObject());
                target.appendJavaScript("markInputError('datePickerContainer');");
                this.valid = false;
            }
        }
        return this.valid;
    }
}
