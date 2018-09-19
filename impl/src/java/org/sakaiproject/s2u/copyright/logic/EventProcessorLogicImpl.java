package org.sakaiproject.s2u.copyright.logic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.StringUtils;

import org.sakaiproject.authz.api.SecurityAdvisor;
import org.sakaiproject.component.cover.ComponentManager;
import org.sakaiproject.event.api.Event;
import org.sakaiproject.s2u.copyright.logic.listener.EventProcessor;
import org.sakaiproject.tool.api.Session;
import org.sakaiproject.tool.api.SessionManager;

@Slf4j
public class EventProcessorLogicImpl implements Observer, EventProcessorLogic {

    protected EventProcessingThread eventProcessingThread = new EventProcessingThread();
    protected Queue<EventCopy> eventQueue = new ConcurrentLinkedQueue<>();
    protected Object eventQueueLock = new Object();

    protected static long EventProcessorThreadId = 0L;

    protected String serverId = null;
    protected boolean loopTimerEnabled = false; //for debugging purposes

    protected Map<String,List<EventProcessor>> eventProcessors = new HashMap<>();

    /************************************************************************
     * Spring-injected classes
     ************************************************************************/
    @Setter protected SakaiProxyImpl sakaiProxy;
    @Setter protected SessionManager sessionManager;

    /************************************************************************
     * init() and destroy()
     ************************************************************************/

    public void init() {
        log.info("init()");

        if(serverId == null) {
            serverId = sakaiProxy.getServerId();
        }

        if (!sakaiProxy.isEventProcessingThreadDisabled()) {
            if(this.eventProcessingThread == null) {
                this.eventProcessingThread = new EventProcessingThread();
            }
            this.eventProcessingThread.start();
            this.sakaiProxy.addLocalEventListener(this);
        }
    }

    public void destroy() {
        log.info("destroy()");

        synchronized(eventQueueLock) {
            if(this.eventQueue != null) {
                // empty the event queue
                this.eventQueue.clear();

                // shut down daemon once it's done processing events
                if(this.eventProcessingThread != null) {
                    this.eventProcessingThread.close();
                    this.eventProcessingThread = null;
                }

                // destroy the event queue
                this.eventQueue = null;
            }
        }
    }

    /************************************************************************
     * Observer method
     * @param arg0
     * @param obj
     ************************************************************************/
    @Override
    public void update(Observable arg0, Object obj) {
        if(obj instanceof Event) {
            Event event = (Event) obj;
            if(getEventProcessors(event.getEvent()) != null) {
                if(log.isDebugEnabled()) {
                    log.debug("adding event to queue: " + event.getEvent());
                }
                synchronized(this.eventQueueLock) {
                    if(this.eventQueue != null) {
                        this.eventQueue.add(new EventCopy(event));
                    }
                }
                if((this.eventProcessingThread == null || ! this.eventProcessingThread.isAlive()) && eventQueue != null) {
                    // the update() method gets called if and only if EventProcessorLogic is registered as an observer.
                    // EventProcessorLogic is registered as an observer if and only if event processing is enabled.
                    // So if the eventProcessingThread is null or disabled in some way, we should restart it,
                    // unless the eventQueue is null, which should happen if and only if we are shutting down.
                    this.eventProcessingThread = null;
                    this.eventProcessingThread = new EventProcessingThread();
                    this.eventProcessingThread.start();
                }
            }
        }
    }

    /************************************************************************
     * Event processing : processors
     ************************************************************************/

    @Override
    public void registerEventProcessor(EventProcessor eventProcessor) {
        registerEventProcessor(null, eventProcessor);
    }

    @Override
    public void registerEventProcessor(String eventId, EventProcessor eventProcessor) {
        log.debug("Registering : "+eventId+" -> "+eventProcessor.getClass().getSimpleName()+" => "+sakaiProxy.isEventProcessingListenerEnabled(eventProcessor));
        String eventIdentifier = eventId;
        if(sakaiProxy.isEventProcessingListenerEnabled(eventProcessor)) {
            if(StringUtils.isBlank(eventIdentifier)){
                eventIdentifier = eventProcessor.getEventIdentifer();
            }
            if(eventProcessor != null && eventIdentifier != null) {
                List processorsList = this.eventProcessors.get(eventIdentifier);
                if(processorsList == null) {
                    processorsList = new ArrayList<>();
                }
                processorsList.add(eventProcessor);
                this.eventProcessors.put(eventIdentifier, processorsList);
            }
        }
    }

    @Override
    public List<EventProcessor> getEventProcessors(String eventIdentifier) {
        return this.eventProcessors.get(eventIdentifier);
    }

    /************************************************************************
     * Event processing daemon (or thread?)
     ************************************************************************/

    public class EventProcessingThread extends Thread {
        protected static final String EVENT_PROCESSING_THREAD_SHUT_DOWN_MESSAGE = "\n===================================================\n  Event Processing Thread shutting down  \n===================================================";

        protected boolean timeToQuit = false;

        protected long loopTimer = 0L;
        protected String loopActivity = "";

        private final long sleepTime = 2L;

        public EventProcessingThread() {
            super("Event Processing Thread");
            log.info("Created Event Processing Thread");

            this.setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler(){
                public void uncaughtException(Thread arg0, Throwable arg1) {
                    log.error(EVENT_PROCESSING_THREAD_SHUT_DOWN_MESSAGE, arg1);
                }
            });
        }

        public void close() {
            timeToQuit = true;
        }

        @Override
        public void run() {
            // wait till ComponentManager is ready
            ComponentManager.waitTillConfigured();

            try {
                EventProcessorThreadId = Thread.currentThread().getId();
                log.info("Started Event Processing Thread: " + EventProcessorThreadId);

                sakaiProxy.startAdminSession();
                while(! timeToQuit) {
                    if(loopTimerEnabled) {
                        loopTimer = System.currentTimeMillis();
                        loopActivity = "nothing";
                    }

                    log.debug("Event Processing Thread checking event queue: " + eventQueue.size());

                    //pop event from queue
                    EventCopy event = null;
                    synchronized(eventQueueLock) {
                        if(eventQueue != null && ! eventQueue.isEmpty()) {
                            event = eventQueue.poll();
                        }
                    }

                    if(event != null) {
                        if(loopTimerEnabled) {
                            loopActivity = "processingEvents";
                        }
                        if(log.isDebugEnabled()) {
                            log.debug("Event Processing Thread is processing event: " + event.getEvent());
                        }
                        //get list of event processors based on event ID
                        List<EventProcessor> eventProcessorsList = getEventProcessors(event.getEvent());
                        //create new thread to process the event for each listener
                        for(EventProcessor ep : eventProcessorsList) {
                            ListenerProcessingThread listenerProcessingThread = new ListenerProcessingThread(ep, event);
                            listenerProcessingThread.start();
                        }

                        if(loopTimerEnabled) {
                            long elapsedTime = System.currentTimeMillis() - loopTimer;
                            StringBuilder buf = new StringBuilder("EventProcessingThread.activityTimer\t");
                            buf.append(loopTimer);
                            buf.append("\t");
                            buf.append(elapsedTime);
                            buf.append("\t");
                            buf.append(loopActivity);
                            log.info(buf.toString());
                        }
                    }

                    if(eventQueue == null || eventQueue.isEmpty()) {
                        try {
                            Thread.sleep(sleepTime * 1000L);
                        } catch (InterruptedException e) {
                            log.warn("InterruptedException in Event Processing Thread: " + e);
                        }
                    }
                }

                log.warn(EVENT_PROCESSING_THREAD_SHUT_DOWN_MESSAGE);

            } catch(Throwable t) {
                log.error("Unhandled throwable is stopping Event Processing Thread", t);
                return;
            }
        }
    }

    public class ListenerProcessingThread extends Thread {
        private final EventProcessor eventProcessor;
        private final EventCopy event;

        public ListenerProcessingThread(EventProcessor eventProcessor, EventCopy event){
            this.eventProcessor = eventProcessor;
            this.event = event;
        }

        public void run() {
            SecurityAdvisor advisor = new LogicSecurityAdvisor();
            Session sakaiSession = sessionManager.getCurrentSession();
            try {
                sakaiSession.setUserId(ADMIN);
                sakaiSession.setUserEid(ADMIN);

                sakaiProxy.pushSecurityAdvisor(advisor);
                eventProcessor.processEvent(event);
            } catch (Exception e) {
                log.warn("Error processing event: " + event, e);
            } finally {
                sakaiProxy.popSecurityAdvisor(advisor);
                sakaiProxy.clearThreadLocalCache();
                sakaiSession.setUserId(null);
                sakaiSession.setUserEid(null);
            }
        }
    }

    public class LogicSecurityAdvisor implements SecurityAdvisor {

        @Override
        public SecurityAdvice isAllowed(String userId, String function, String reference) {
            long threadId = Thread.currentThread().getId();
            if(threadId == EventProcessorLogicImpl.EventProcessorThreadId) {
                return SecurityAdvice.ALLOWED;
            }
            return SecurityAdvice.PASS;
        }
    }
}
