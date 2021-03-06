/*
 * Copyright (C) 2018 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.optaplanner.openshift.employeerostering.gwtui.client.pages.employeeroster;

import java.time.LocalDateTime;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.inject.Named;

import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import elemental2.dom.HTMLAnchorElement;
import elemental2.dom.HTMLButtonElement;
import elemental2.dom.HTMLDivElement;
import elemental2.dom.HTMLElement;
import elemental2.dom.MouseEvent;
import elemental2.promise.Promise;
import org.jboss.errai.ui.shared.api.annotations.DataField;
import org.jboss.errai.ui.shared.api.annotations.EventHandler;
import org.jboss.errai.ui.shared.api.annotations.ForEvent;
import org.jboss.errai.ui.shared.api.annotations.Templated;
import org.optaplanner.openshift.employeerostering.gwtui.client.app.spinner.LoadingSpinner;
import org.optaplanner.openshift.employeerostering.gwtui.client.header.HeaderView;
import org.optaplanner.openshift.employeerostering.gwtui.client.pages.Page;
import org.optaplanner.openshift.employeerostering.gwtui.client.rostergrid.powers.BlobPopover;
import org.optaplanner.openshift.employeerostering.gwtui.client.rostergrid.view.ViewportView;
import org.optaplanner.openshift.employeerostering.gwtui.client.tenant.TenantStore;
import org.optaplanner.openshift.employeerostering.gwtui.client.util.PromiseUtils;
import org.optaplanner.openshift.employeerostering.shared.employee.Employee;
import org.optaplanner.openshift.employeerostering.shared.employee.EmployeeRestServiceBuilder;
import org.optaplanner.openshift.employeerostering.shared.roster.Pagination;
import org.optaplanner.openshift.employeerostering.shared.roster.RosterRestServiceBuilder;
import org.optaplanner.openshift.employeerostering.shared.roster.view.EmployeeRosterView;

import static org.optaplanner.openshift.employeerostering.gwtui.client.common.FailureShownRestCallback.onSuccess;

@Templated
@ApplicationScoped
public class EmployeeRosterPage implements Page {

    @Inject
    @DataField("toolbar")
    private HTMLDivElement toolbar;

    @Inject
    @DataField("refresh-button")
    private HTMLButtonElement refreshButton;

    @Inject
    @DataField("viewport")
    private ViewportView<LocalDateTime> viewportView;

    @Inject
    @DataField("next-page-button")
    private HTMLAnchorElement nextPageButton;

    @Inject
    @DataField("previous-page-button")
    private HTMLAnchorElement previousPageButton;

    @Inject
    @DataField("availability-blob-popover")
    private BlobPopover availabilityBlobPopover;

    @Inject
    @DataField("current-pagination-range")
    @Named("span")
    private HTMLElement currentRange;

    @Inject
    @DataField("num-of-employees")
    @Named("span")
    private HTMLElement rowCount;

    @Inject
    private EmployeeAvailabilityBlobPopoverContent availabilityBlobPopoverContent;

    @Inject
    private EmployeeRosterViewportFactory employeeRosterViewportFactory;

    @Inject
    private HeaderView headerView;

    @Inject
    private TenantStore tenantStore;

    @Inject
    private LoadingSpinner loadingSpinner;

    @Inject
    private PromiseUtils promiseUtils;

    private EmployeeRosterViewport viewport;
    private Pagination employeePagination = Pagination.of(0, 10);
    private EmployeeRosterView currentEmployeeRosterView;

    @PostConstruct
    public void init() {
        availabilityBlobPopover.init(this, availabilityBlobPopoverContent);
    }

    public BlobPopover getBlobPopover() {
        return availabilityBlobPopover;
    }

    public EmployeeRosterView getCurrentEmployeeRosterView() {
        return currentEmployeeRosterView;
    }

    @Override
    public Promise<Void> beforeOpen() {
        return refreshWithLoadingSpinner();
    }

    @Override
    public Promise<Void> onOpen() {
        headerView.addStickyElement(() -> toolbar);
        return promiseUtils.resolve();
    }

    public void onTenantChanged(@Observes final TenantStore.TenantChange tenant) {
        refreshWithLoadingSpinner();
    }

    private Promise<Void> refreshWithoutLoadingSpinner() {
        return getEmployeeList().then(employeeList -> {
            return fetchEmployeeRosterView().then(employeeRosterView -> {
                if (employeeRosterView.getEmployeeList().isEmpty()) {
                    employeePagination = employeePagination.previousPage();
                } else {
                    viewport = employeeRosterViewportFactory.getViewport(employeeRosterView);
                    viewportView.setViewport(viewport);
                }
                rowCount.innerHTML = Integer.toString(employeeList.size());
                currentRange.innerHTML = new SafeHtmlBuilder().append(employeePagination.getFirstResultIndex() + 1)
                        .append('-').append(Math.min(employeePagination.getFirstResultIndex() + employeePagination.getNumberOfItemsPerPage(),
                                employeeList.size()))
                        .toSafeHtml().asString();
                return promiseUtils.resolve();
            });
        });
    }

    private Promise<Void> refreshWithLoadingSpinner() {

        loadingSpinner.showFor("refresh-employee-roster");

        return refreshWithoutLoadingSpinner().then(i -> {
            loadingSpinner.hideFor("refresh-employee-roster");
            return promiseUtils.resolve();
        }).catch_(e -> {
            loadingSpinner.hideFor("refresh-employee-roster");
            return promiseUtils.resolve();
        });
    }

    //Events
    @EventHandler("refresh-button")
    public void onRefreshButtonClicked(@ForEvent("click") final MouseEvent e) {
        refreshWithLoadingSpinner();
    }

    @EventHandler("previous-page-button")
    public void onPreviousPageButtonClicked(@ForEvent("click") final MouseEvent e) {

        if (employeePagination.isOnFirstPage()) {
            return;
        }

        employeePagination = employeePagination.previousPage();
        refreshWithLoadingSpinner();
    }

    @EventHandler("next-page-button")
    public void onNextPageButtonClicked(@ForEvent("click") final MouseEvent e) {
        employeePagination = employeePagination.nextPage();
        refreshWithLoadingSpinner();
    }

    //API calls
    private Promise<EmployeeRosterView> fetchEmployeeRosterView() {
        return new Promise<>((resolve, reject) -> {
            RosterRestServiceBuilder.getCurrentEmployeeRosterView(tenantStore.getCurrentTenantId(), employeePagination.getPageNumber(), employeePagination.getNumberOfItemsPerPage(),
                    onSuccess(v -> {
                        currentEmployeeRosterView = v;
                        resolve.onInvoke(v);
                    }));
        });
    }

    private Promise<List<Employee>> getEmployeeList() {
        return promiseUtils.promise((resolve, reject) -> {
            EmployeeRestServiceBuilder.getEmployeeList(tenantStore.getCurrentTenantId(),
                    onSuccess(resolve::onInvoke));
        });
    }
}
