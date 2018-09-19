package org.sakaiproject.s2u.copyright.tool;

import java.util.Locale;

import org.apache.wicket.Component;
import org.apache.wicket.Session;
import org.apache.wicket.markup.head.ResourceAggregator;
import org.apache.wicket.markup.head.filter.JavaScriptDeferHeaderResponse;
import org.apache.wicket.protocol.http.WebApplication;
import org.apache.wicket.protocol.http.WebSession;
import org.apache.wicket.request.IRequestHandler;
import org.apache.wicket.request.Request;
import org.apache.wicket.request.Response;
import org.apache.wicket.request.cycle.IRequestCycleListener;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.Url;
import org.apache.wicket.resource.loader.IStringResourceLoader;
import org.apache.wicket.spring.injection.annot.SpringComponentInjector;

import org.sakaiproject.s2u.copyright.tool.pages.DocumentManagement;
import org.sakaiproject.util.ResourceLoader;

/**
 * Main application class for the app
 */
public class CopyrightCheckerTool extends WebApplication {

    private static final ResourceLoader messages = new ResourceLoader("CopyrightChecker");

    /**
     * Configure your app here
     */
    @Override
    protected void init() {

        //Configure for Spring injection
        getComponentInstantiationListeners().add(new SpringComponentInjector(this));

        // Custom resource loader since our properties are not in the default location
        getResourceSettings().getStringResourceLoaders().add(new CopyrightCheckerStringResourceLoader());

        //Don't throw an exception if we are missing a property, just fallback
        getResourceSettings().setThrowExceptionOnMissingResource(false);

        //Remove the wicket specific tags from the generated markup
        getMarkupSettings().setStripWicketTags(true);

        // On Wicket session timeout, redirect to main page
        getApplicationSettings().setPageExpiredErrorPage(DocumentManagement.class);
        getApplicationSettings().setAccessDeniedPage(DocumentManagement.class);

        // Use this to render JS's after others
        setHeaderResponseDecorator(response -> new ResourceAggregator(new JavaScriptDeferHeaderResponse(response)));

        getRequestCycleListeners().add(new IRequestCycleListener() {
            public void onBeginRequest() {
                // optionally do something at the beginning of the request
            }

            public void onEndRequest() {
                // optionally do something at the end of the request
            }

            public IRequestHandler onException(RequestCycle cycle, Exception ex) {
                // optionally do something here when there's an exception

                // then, return the appropriate IRequestHandler, or "null"
                // to let another listener handle the exception
                ex.printStackTrace();
                return null;
            }

            @Override
            public void onBeginRequest(RequestCycle arg0) {
                // TODO Auto-generated method stub

            }

            @Override
            public void onDetach(RequestCycle arg0) {
                // TODO Auto-generated method stub

            }

            @Override
            public void onEndRequest(RequestCycle arg0) {
                // TODO Auto-generated method stub

            }

            @Override
            public void onExceptionRequestHandlerResolved(RequestCycle arg0, IRequestHandler arg1, Exception arg2) {
                // TODO Auto-generated method stub
            }

            @Override
            public void onRequestHandlerExecuted(RequestCycle arg0, IRequestHandler arg1) {
                // TODO Auto-generated method stub
            }

            @Override
            public void onRequestHandlerResolved(RequestCycle arg0, IRequestHandler arg1) {
                // TODO Auto-generated method stub
            }

            @Override
            public void onRequestHandlerScheduled(RequestCycle arg0, IRequestHandler arg1) {
                // TODO Auto-generated method stub
            }

            @Override
            public void onUrlMapped(RequestCycle arg0, IRequestHandler arg1, Url arg2) {
                // TODO Auto-generated method stub
            }
        });
    }

    // Custom resource loader
    private static class CopyrightCheckerStringResourceLoader implements IStringResourceLoader {
        @Override
        public String loadStringResource(final Class<?> clazz, final String key,
                final Locale locale, final String style, final String variation) {
            messages.setContextLocale(locale);
            return messages.getString(key, key);
        }

        @Override
        public String loadStringResource(final Component component, final String key,
                final Locale locale, final String style, final String variation) {
            messages.setContextLocale(locale);
            return messages.getString(key, key);
        }
    }

    /**
     * The main page for our app
     * @see org.apache.wicket.Application#getHomePage()
     */
    public Class<DocumentManagement> getHomePage() {
        return DocumentManagement.class;
    }

    @Override
    public Session newSession(Request req, Response res) {
        return new MySession(req);
    }

    //this is your session class
    class MySession extends WebSession {

        public MySession(Request req) {
            super(req);
        }

        @Override
        public Locale getLocale() {
            return messages.getLocale();
        }
    }
}
