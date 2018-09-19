package org.sakaiproject.s2u.copyright.tool.components;

import org.apache.wicket.markup.html.navigation.paging.IPageable;
import org.apache.wicket.markup.html.navigation.paging.IPagingLabelProvider;
import org.apache.wicket.markup.html.navigation.paging.PagingNavigator;

public class TablePagingNavigator extends PagingNavigator {

    public TablePagingNavigator(String id, IPageable pageable) {
        super(id, pageable);
    }

    public TablePagingNavigator(String id, IPageable pageable, IPagingLabelProvider labelProvider) {
        super(id, pageable, labelProvider);
    }
}
