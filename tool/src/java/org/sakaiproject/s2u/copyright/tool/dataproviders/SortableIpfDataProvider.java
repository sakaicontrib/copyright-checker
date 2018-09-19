/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.sakaiproject.s2u.copyright.tool.dataproviders;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import lombok.Getter;

import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.data.table.filter.IFilterStateLocator;
import org.apache.wicket.extensions.markup.html.repeater.util.SortableDataProvider;
import org.apache.wicket.injection.Injector;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

import org.sakaiproject.s2u.copyright.logic.CopyrightCheckerService;
import org.sakaiproject.s2u.copyright.logic.SakaiProxy;
import org.sakaiproject.s2u.copyright.model.IntellectualPropertyFileStatus;

public class SortableIpfDataProvider extends SortableDataProvider<DetachableIpfModel, String> implements IFilterStateLocator<IpfFilter> {

    @SpringBean(name = "org.sakaiproject.s2u.copyright.logic.CopyrightCheckerService")
    protected CopyrightCheckerService copyrightCheckerService;

    @SpringBean(name = "org.sakaiproject.s2u.copyright.logic.SakaiProxy")
    protected SakaiProxy sakaiProxy;

    @Getter transient private List<DetachableIpfModel> filteredList;

    private IpfFilter lpiFilterState = new IpfFilter();

    /**
     * constructor
     */
    public SortableIpfDataProvider() {
        // set default sort
        Injector.get().inject(this);
        setSort("created", SortOrder.DESCENDING);
    }

    @Override
    public Iterator<DetachableIpfModel> iterator(long first, long count) {
        Collections.sort(filteredList, new Comparator<DetachableIpfModel>() {
            @Override
            public int compare(DetachableIpfModel o1, DetachableIpfModel o2) {
                int order = getSort().isAscending() ? 1 : -1;
                switch (getSort().getProperty()) {
                    case "modified":
                        return order * (o1.getObject().getModified().compareTo(o2.getObject().getModified()));
                    case "fileName":
                        return order * (o1.getFileName().compareTo(o2.getFileName()));
                    case "context":
                        String o1SiteName = sakaiProxy.getFileContextName(o1.getObject().getContext());
                        String o2SiteName = sakaiProxy.getFileContextName(o2.getObject().getContext());
                        return order * (o1SiteName.compareTo(o2SiteName));
                    case "displayStatus":
                        String o1State = new ResourceModel("lpi.file.state." +
                                copyrightCheckerService.getFileState(
                                        o1.getObject().getProperty(), o1.getObject().getState(), o1.isDeleted())).getObject();
                        String o2State = new ResourceModel("lpi.file.state." +
                                copyrightCheckerService.getFileState(
                                        o2.getObject().getProperty(), o2.getObject().getState(), o2.isDeleted())).getObject();
                        return order * (o1State.compareTo(o2State));
                    case "visibility":
                        String o1Visible = (!sakaiProxy.fileIsHidden(o1.getObject().getFileId())) ? new ResourceModel("lpi.yes").getObject() : new ResourceModel("lpi.no").getObject();
                        String o2Visible = (!sakaiProxy.fileIsHidden(o2.getObject().getFileId())) ? new ResourceModel("lpi.yes").getObject() : new ResourceModel("lpi.no").getObject();
                        return order * (o1Visible.compareTo(o2Visible));
                    default:
                        return 0;
                }
            }
        });
        return filteredList.subList((int) first, (int) (first + count)).iterator();
    }

    private List<DetachableIpfModel> filterLpiFiles(List<DetachableIpfModel> lpiFilesFound) {
        ArrayList<DetachableIpfModel> result = new ArrayList<>();

        String contextFilter = lpiFilterState.getContext();
        String fileNameFilter = lpiFilterState.getFileName();

        Integer visibilityFilter = null;
        Integer displayStatusFilter = null;


        if(lpiFilterState.getVisibility() != null){
            visibilityFilter = Integer.parseInt(lpiFilterState.getVisibility().getId());
        }
        if(lpiFilterState.getDisplayStatus() != null){
            displayStatusFilter = Integer.parseInt(lpiFilterState.getDisplayStatus().getId());
        }

        for (DetachableIpfModel lpifile : lpiFilesFound) {
            Boolean itemVisibility = lpifile.isHidden();
            Integer itemStatus = lpifile.getDisplayStatus();
            if(lpifile.isDeleted()){
                continue;
            }
            // FILTER BY VISIBILITY
            if (visibilityFilter != null) {
                switch (visibilityFilter) {
                    case 1:
                        if (itemVisibility) {
                            continue;
                        }
                        break;
                    case 2:
                        if (!itemVisibility) {
                            continue;
                        }
                        break;
                    default:
                        break;
                }
            }

            // FILTER BY STATUS
            if (displayStatusFilter != null) {
                switch (displayStatusFilter) {
                    case (IntellectualPropertyFileStatus.NOT_PROCESSED):
                        if (IntellectualPropertyFileStatus.NOT_PROCESSED != itemStatus) {
                            continue;
                        }
                        break;
                    case (IntellectualPropertyFileStatus.WAITING_LICENSE):
                        if (IntellectualPropertyFileStatus.WAITING_LICENSE != itemStatus) {
                            continue;
                        }
                        break;
                    case (IntellectualPropertyFileStatus.NOT_AUTHORIZED):
                        if (IntellectualPropertyFileStatus.NOT_AUTHORIZED != itemStatus) {
                            continue;
                        }
                        break;
                    case (IntellectualPropertyFileStatus.AUTHORIZED):
                        if (IntellectualPropertyFileStatus.AUTHORIZED != itemStatus) {
                            continue;
                        }
                        break;
                    case (IntellectualPropertyFileStatus.NOT_PRINTABLE):
                        if (IntellectualPropertyFileStatus.NOT_PRINTABLE != itemStatus) {
                            continue;
                        }
                        break;
                    case (IntellectualPropertyFileStatus.DELETED_STATUS):
                        if (IntellectualPropertyFileStatus.DELETED_STATUS != itemStatus) {
                            continue;
                        }
                        break;
                    default:
                        break;
                }
            }

            // FILTER BY CONTEXT
            if(contextFilter != null && !lpifile.getDisplayContext().toLowerCase().contains(contextFilter.toLowerCase())){
                continue;
            }

            // FILTER BY Filename
            if(fileNameFilter != null && !lpifile.getFileName().toLowerCase().contains(fileNameFilter.toLowerCase())){
                continue;
            }

            result.add(lpifile);
        }
        return result;
    }

    @Override
    public long size() {
        List<DetachableIpfModel> data = DetachableIpfModel.fromList(copyrightCheckerService.findIntellectualPropertyFilesByUserId(sakaiProxy.getCurrentUserId()));
        filteredList = filterLpiFiles(data);
        return filteredList.size();
    }

    @Override
    public IModel<DetachableIpfModel> model(DetachableIpfModel object) {
        return Model.of(object);
    }

    @Override
    public IpfFilter getFilterState() {
        return lpiFilterState;
    }

    @Override
    public void setFilterState(IpfFilter state) {
        lpiFilterState = state;
    }
}
