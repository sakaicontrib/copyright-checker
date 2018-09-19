package org.sakaiproject.s2u.copyright.logic.listener;

import org.sakaiproject.event.api.Event;

public interface EventProcessor {

    /**
     * Get the unique identifier for the events that will be handled
     * by this processor
     * @return the unique identifier for the events
     */
    public String getEventIdentifer();

    /**
     * Deal with one event -- adding, updating or deleting
     * events as appropriate.
     * @param event The event to be processed.
     */
    public void processEvent(Event event);
}
