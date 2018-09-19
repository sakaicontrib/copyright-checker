package org.sakaiproject.s2u.copyright.tool.pages;

import javax.servlet.http.HttpServletRequest;

import lombok.extern.slf4j.Slf4j;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.feedback.FeedbackMessage;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.head.OnLoadHeaderItem;
import org.apache.wicket.markup.head.StringHeaderItem;
import org.apache.wicket.markup.html.IHeaderContributor;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.spring.injection.annot.SpringBean;

import org.sakaiproject.s2u.copyright.logic.CopyrightCheckerService;
import org.sakaiproject.s2u.copyright.logic.SakaiProxy;

/**
 * This is our base page for our Sakai app. It sets up the containing markup and top navigation.
 * All top level pages should extend from this page so as to keep the same navigation. The content for those pages will
 * be rendered in the main area below the top nav.
 * <p>It also allows us to setup the API injection and any other common methods, which are then made available in the other pages.
 */
@Slf4j
public class BasePage extends WebPage implements IHeaderContributor {

    @SpringBean(name="org.sakaiproject.s2u.copyright.logic.SakaiProxy")
    protected SakaiProxy sakaiProxy;

    @SpringBean(name="org.sakaiproject.s2u.copyright.logic.CopyrightCheckerService")
    protected CopyrightCheckerService copyrightCheckerService;

    public FeedbackPanel feedbackPanel;

    public BasePage() {
        log.debug("BasePage()");

        // Add a FeedbackPanel for displaying our messages
        feedbackPanel = new FeedbackPanel("feedback"){
            @Override
            protected Component newMessageDisplayComponent(final String id, final FeedbackMessage message) {
                final Component newMessageDisplayComponent = super.newMessageDisplayComponent(id, message);

                switch (message.getLevel()) {
                    case FeedbackMessage.ERROR:
                    case FeedbackMessage.DEBUG:
                    case FeedbackMessage.FATAL:
                    case FeedbackMessage.WARNING:
                    default:
                        add(AttributeModifier.replace("class", "alertMessage"));
                        break;
                    case FeedbackMessage.INFO:
                        add(AttributeModifier.replace("class", "information"));
                        break;
                    case FeedbackMessage.SUCCESS:
                        add(AttributeModifier.replace("class", "messageSuccess"));
                        break;
                }

                return newMessageDisplayComponent;
            }
        };
        feedbackPanel.setOutputMarkupId(true);
        add(feedbackPanel);
    }

    /**
     * Helper to clear the feedbackpanel display.
     * @param f FeedBackPanel
     */
    public void clearFeedback(FeedbackPanel f) {
        if(!f.hasFeedbackMessage()) {
            f.add(AttributeModifier.replace("class", ""));
        }
    }

    /**
     * This block adds the required wrapper markup to style it like a Sakai tool.
     * Add to this any additional CSS or JS references that you need.
     * @param response
     */
    @Override
    public void renderHead(IHeaderResponse response) {
        //get the Sakai skin header fragment from the request attribute
        HttpServletRequest request = (HttpServletRequest)getRequest().getContainerRequest();

        response.render(StringHeaderItem.forString((String)request.getAttribute("sakai.html.head")));
        response.render(OnLoadHeaderItem.forScript("setMainFrameHeight( window.name )"));
        response.render(JavaScriptHeaderItem.forUrl("/copyright-checker-tool/webjars/featherlight/release/featherlight.min.js"));
        response.render(CssHeaderItem.forUrl("/copyright-checker-tool/webjars/featherlight/release/featherlight.min.css"));

        //Tool additions (at end so we can override if required)
        response.render(StringHeaderItem.forString("<meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\" />"));
    }
}
