# Copyright Checker

The copyright checker allows users to declare the copyright status of the different resources uploaded to Sakai. Know the copyright status of all the resources is very important for the institutions to negotiate with the IP entities.

Every time an instructor uploads a file, the instructor will be informed via mail/notification that should declare the copyright status of the item, if the instructor doesn't declare the status after some period, the content will be hidden for regular users.

This tool allows the IP entities to make reports about the copyright status of the resources.

## Main features

-  REST API
-  Supports email notifications.
-  Supports popup notifications (PA System).
-  Ability to filter by role.
-  Ability to disable the feature at site level.
-  Ability to filter by tool.
-  Document preview.

## Contribution
This work has been done by [EDF](https://www.edf.global) and contributed by the some members of the Sakai Spanish Users group [S2U](https://confluence.sakaiproject.org/display/SPANISH/S2U+-+Spanish+Sakai+Users)

-  [UPV](http://www.upv.es/index-en.html)
-  [UPNA](http://www.unavarra.es/?languageId=1)
-  [UDL](http://www.udl.es/ca/en/)
-  [UMU](https://www.um.es/en/web/iwp/inicio)

## Contact
If you have any questions please contact the [devs](mailto:it@edf.global).

## Configuration

### Workspaces
The tool should be added to all instructors' workspaces, [follow this guide](https://stackoverflow.com/questions/24205895/how-can-i-add-a-new-tool-to-all-users-my-workspace-sites-in-sakai) to add the tool.

### Skin
You may want to add an icon for the copyright checker tool in your institution's skin.
```
.icon-sakai--sakai-copyright-checker {                    @extend .fa-copyright; }
```

### Disable processing at site level
You can disable the file processing at site level, configure this property in the site.
```
copyright.checker.disable=true
```

### Configuration in the sakai.properties file
```
# (Optional) Enable the PA System integration, show popups to the users about the pending copyright items.
# Default: false
copyright.checker.popup.enable=true

# (Optional) TTL or duration of the PA System popup (in days)
# Default: 2
# copyright.checker.popup.duration = 2

# (Optional) Enable the Email integration, send emails to the users about the pending copyright items.
# Default: false
copyright.checker.email.enable=true

# Support contacts emails, users can send copyright queries to them.
copyright.checker.support.enable=true
copyright.checker.support.contacts.count=2
copyright.checker.support.contacts.1=some@email.com
copyright.checker.support.contacts.2=some2@email.com

# (Optional) Attachment collections to check, it should include the name of the tools in all the locales (Assignments, Tareas, Activitats, Zereginak,...)
copyright.checker.attachment.collections.count=8
copyright.checker.attachment.collections.1=Assignments
copyright.checker.attachment.collections.2=Tareas
copyright.checker.attachment.collections.3=Activitats
copyright.checker.attachment.collections.4=Zereginak
copyright.checker.attachment.collections.5=Forums
copyright.checker.attachment.collections.6=Foros
copyright.checker.attachment.collections.7=Debat
copyright.checker.attachment.collections.8=Foroak

# Instance roles whose files must be checked, mostly your institution instructor roles.
copyright.checker.roles.count=2
copyright.checker.roles.1=Instructor
copyright.checker.roles.2=maintain

# Checked mimetypes, the tool will only process these file mime types, ignoring others.
copyright.checker.checked.mimetypes.count=6
copyright.checker.checked.mimetypes.1=text/plain
copyright.checker.checked.mimetypes.2=application/pdf
copyright.checker.checked.mimetypes.3=application/vnd.openxmlformats-officedocument.wordprocessingml.document
copyright.checker.checked.mimetypes.4=application/xhtml+xml
copyright.checker.checked.mimetypes.5=text/html
copyright.checker.checked.mimetypes.6=application/msword

# (Optional) Disable the event processing, the listener will not process any event.
# DANGER: If you disable the event processor, the tool will rely in the quartz jobs only, the events will be processed depending on the job triggers.
# Default: false
# copyright.checker.eventprocessing.disable=true

# (Optional) List of listeners to be processed, separated by comma.
# DANGER: If you disable the ContentEventProcessor listener, the tool will rely in the quartz jobs only, the events will be processed depending on the job triggers.
# Default: * (Process all listeners)
# copyright.checker.eventprocessing.listeners=ContentEventProcessor,ContentVisibilityEventProcessor
```

## Job to hide files that have not been declared

You may need to add a new job in the jobscheduler tool to your admin workspace.
-  In the jobscheduler tool click on "new job"
-  Select "Copyright Checker - Hide files which intellectual property has not been declared." from the dropdown list
-  Give it a name in the text box (can be anything)
-  Click post
-  Select the triggers link next to the job you just created
-  Create a new trigger. We suggest you set the job to run every 24 hours, which can be done with the following definition
```
0 0 * * * ?
```

## Debugging
Debugging, really useful to debug the entire process

```
log.config.count = 4
log.config.1 = DEBUG.org.sakaiproject.s2u.copyright.listener.ContentEventProcessor
log.config.2 = DEBUG.org.sakaiproject.s2u.copyright.logic.CopyrightCheckerServiceImpl
log.config.3 = DEBUG.org.sakaiproject.s2u.copyright.dao.impl.CopyrightCheckerDaoImpl
log.config.4 = DEBUG.org.sakaiproject.s2u.copyright.logic.SakaiProxyImpl
```
