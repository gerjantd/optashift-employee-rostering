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

package org.optaplanner.openshift.employeerostering.gwtui.client.rostergrid.view;

import java.util.List;

import javax.inject.Inject;

import elemental2.dom.HTMLDivElement;
import elemental2.dom.MouseEvent;
import org.jboss.errai.ui.shared.api.annotations.DataField;
import org.jboss.errai.ui.shared.api.annotations.EventHandler;
import org.jboss.errai.ui.shared.api.annotations.ForEvent;
import org.jboss.errai.ui.shared.api.annotations.Templated;
import org.optaplanner.openshift.employeerostering.gwtui.client.rostergrid.list.ListElementView;
import org.optaplanner.openshift.employeerostering.gwtui.client.rostergrid.list.ListView;
import org.optaplanner.openshift.employeerostering.gwtui.client.rostergrid.model.Blob;
import org.optaplanner.openshift.employeerostering.gwtui.client.rostergrid.model.Lane;
import org.optaplanner.openshift.employeerostering.gwtui.client.rostergrid.model.SubLane;
import org.optaplanner.openshift.employeerostering.gwtui.client.rostergrid.model.Viewport;

import static java.util.stream.Collectors.toList;

@Templated
public class SubLaneView<T> implements ListElementView<SubLane<T>> {

    @Inject
    @DataField("sub-lane")
    public HTMLDivElement root;

    @Inject
    private ListView<Blob<T>> blobs;

    private SubLane<T> subLane;
    private ListView<SubLane<T>> subLaneViews;

    private Viewport<T> viewport;

    private Lane<T> lane;
    private ListView<Lane<T>> lanes;

    private boolean hasMouseMoved = false;

    @Override
    @SuppressWarnings("unchecked")
    public ListElementView<SubLane<T>> setup(final SubLane<T> subLane,
                                             final ListView<SubLane<T>> subLaneViews) {

        this.subLaneViews = subLaneViews;
        this.subLane = subLane;

        //FIXME: Generics issue
        blobs.init(getElement(), subLane.getBlobs(), () -> (BlobView) viewport.newBlobView()
                .withViewport(viewport)
                .withSubLane(subLane));

        return this;
    }

    @Override
    public void destroy() {
        blobs.clear();
    }

    @EventHandler("sub-lane")
    public void onMouseDown(final @ForEvent("mousedown") MouseEvent e) {
        hasMouseMoved = false;
    }

    @EventHandler("sub-lane")
    public void onMouseMove(final @ForEvent("mousemove") MouseEvent e) {
        hasMouseMoved = true;
    }

    @EventHandler("sub-lane")
    public void onMouseUp(final @ForEvent("mouseup") MouseEvent e) {
        if (!hasMouseMoved) {
            onClick(e);
        }
    }

    private void onClick(final MouseEvent e) {

        if (!e.target.equals(e.currentTarget)) {
            return;
        }

        final double offset = viewport.decideBasedOnOrientation(e.offsetY, e.offsetX);
        final Long positionInGridPixels = viewport.toGridPixels(new Double(offset).longValue());
        final T positionInScaleUnits = viewport.getScale().toScaleUnits(positionInGridPixels);

        final List<Blob<T>> newBlobs = viewport.newBlob(lane, positionInScaleUnits).collect(toList());
        final SubLane<T> nextSubLaneWithAvailableSpace = lane.getNextSubLaneWithSpaceForBlobsStartingFrom(subLane, newBlobs);

        subLaneViews.addOrReplace(nextSubLaneWithAvailableSpace, nextSubLaneWithAvailableSpace.withMore(newBlobs));
    }

    public SubLaneView<T> withViewport(final Viewport<T> viewport) {
        this.viewport = viewport;
        return this;
    }

    public ListElementView<SubLane<T>> withParent(final Lane<T> parentLane,
                                                  final ListView<Lane<T>> parentList) {

        this.lane = parentLane;
        this.lanes = parentList;
        return this;
    }
}
