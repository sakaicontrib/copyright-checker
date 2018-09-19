package org.sakaiproject.s2u.copyright.logic;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Date;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import org.sakaiproject.event.api.Event;
import org.sakaiproject.event.api.LearningResourceStoreService.LRS_Statement;

/************************************************************************
 * Making copies of events
 ************************************************************************/
@Slf4j
public class EventCopy implements Event {

    @Getter protected String context;
    protected String eventIdentifier;
    @Getter protected Date eventTime;
    protected boolean modify;
    @Getter protected int priority;
    protected String entityReference;
    @Getter protected String sessionId;
    @Getter protected String userId;
    @Getter protected LRS_Statement lrsStatement;

    public EventCopy() {
        super();
    }

    public EventCopy(Date eventTime, String eventIdentifier, String entityReference, String context, String userId, String sessionId, char eventCode, int priority) {
        super();
        this.eventTime= eventTime;
        this.eventIdentifier = eventIdentifier;
        this.entityReference = entityReference;
        this.context = context;
        this.userId = userId;
        this.sessionId = sessionId;
        this.priority = priority;
        this.modify = ('m' == eventCode);
    }

    public EventCopy(Event original) {
        super();
        this.context = original.getContext();
        this.eventIdentifier = original.getEvent();

        try {
            // this.eventTime = original.getEventTime();
            // the getEventTime() method did not exist before kernel 1.2
            // so we use reflection
            Method getEventTimeMethod = original.getClass().getMethod("getEventTime", null);
            this.eventTime = (Date) getEventTimeMethod.invoke(original, null);
        } catch (SecurityException | NoSuchMethodException | IllegalArgumentException |
            IllegalAccessException | InvocationTargetException e) {
            log.warn("Exception trying to get event time: " + e);
        }
        if(this.eventTime == null) {
            // If we couldn't get eventTime from event, just use NOW.  That's close enough.
            this.eventTime = new Date();
        }

        this.modify = original.getModify();
        this.priority = original.getPriority();
        this.entityReference = original.getResource();
        this.sessionId = original.getSessionId();
        this.userId = original.getUserId();
        //Uncomment this for 12.x or master
        //this.lrsStatement = original.getLrsStatement();
    }

    @Override
    public String getEvent() {
        return eventIdentifier;
    }

    @Override
    public boolean getModify() {
        return modify;
    }

    @Override
    public String getResource() {
        return entityReference;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("EventCopy [context=");
        builder.append(context);
        builder.append(", eventIdentifier=");
        builder.append(eventIdentifier);
        builder.append(", eventTime=");
        builder.append(eventTime);
        builder.append(", modify=");
        builder.append(modify);
        builder.append(", priority=");
        builder.append(priority);
        builder.append(", entityReference=");
        builder.append(entityReference);
        builder.append(", sessionId=");
        builder.append(sessionId);
        builder.append(", userId=");
        builder.append(userId);
        builder.append(", lrsStatement=");
        builder.append(lrsStatement);
        builder.append("]");
        return builder.toString();
    }
}
