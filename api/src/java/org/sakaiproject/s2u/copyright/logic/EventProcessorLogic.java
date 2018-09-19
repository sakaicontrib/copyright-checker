package org.sakaiproject.s2u.copyright.logic;

import java.util.List;

import org.sakaiproject.s2u.copyright.logic.listener.EventProcessor;

public interface EventProcessorLogic {

    public static final String ADMIN = "admin";

    /************************************************************************
     * Event processing : processors
     ************************************************************************/

    /**
     * Register given event processor listener with the given event id.
     * If event id is null, it will call getEventIdentifer from eventProcessor to
     * get the id
     * @param eventId
     * @param eventProcessor
     */
    public abstract void registerEventProcessor(String eventId, EventProcessor eventProcessor);

    /**
     * Register given event processor listener. It will call getEventIdentifer from
     * eventProcessor to get the id
     * @param eventProcessor
     */
    public abstract void registerEventProcessor(EventProcessor eventProcessor);

    /**
     * Returns a event processor based on the given event id
     * @param eventIdentifier
     * @return
     */
    public abstract List<EventProcessor> getEventProcessors(String eventIdentifier);
}
