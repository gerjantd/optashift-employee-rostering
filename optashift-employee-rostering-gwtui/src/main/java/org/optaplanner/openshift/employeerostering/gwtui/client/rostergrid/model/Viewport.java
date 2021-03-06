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

package org.optaplanner.openshift.employeerostering.gwtui.client.rostergrid.model;

import java.util.List;
import java.util.stream.Stream;

import org.jboss.errai.common.client.api.elemental2.IsElement;
import org.optaplanner.openshift.employeerostering.gwtui.client.rostergrid.view.BlobView;

public abstract class Viewport<T> {

    public abstract void drawGridLinesAt(final IsElement target);

    public abstract void drawDateTicksAt(IsElement target);

    public abstract void drawTimeTicksAt(IsElement target);

    public abstract Lane<T> newLane();

    public abstract Stream<Blob<T>> newBlob(final Lane<T> lane, final T positionInScaleUnits);

    public abstract BlobView<T, ?> newBlobView();

    public abstract List<Lane<T>> getLanes();

    public abstract Long getGridPixelSizeInScreenPixels();

    public abstract Orientation getOrientation();

    public abstract LinearScale<T> getScale();

    public <Y> Y decideBasedOnOrientation(final Y verticalOption, final Y horizontalOption) {
        return getOrientation().equals(Orientation.VERTICAL) ? verticalOption : horizontalOption;
    }

    public Long getSizeInGridPixels(final IsElement element) {
        return toGridPixels(getOrientation().getSize(element));
    }

    public Long toGridPixels(final Long screenPixels) {
        return screenPixels / getGridPixelSizeInScreenPixels();
    }

    public Long toScreenPixels(final Number gridPixels) {
        return Math.round(gridPixels.doubleValue() * getGridPixelSizeInScreenPixels());
    }

    public void setSizeInScreenPixels(final IsElement element, final Number sizeInGridPixels, final Long offsetInScreenPixels) {
        getOrientation().scale(element, sizeInGridPixels.doubleValue(), this, offsetInScreenPixels);
    }

    public void setPositionInScreenPixels(final IsElement element, final Number positionInGridPixels, final Long offsetInScreenPixels) {
        getOrientation().position(element, positionInGridPixels.doubleValue(), this, offsetInScreenPixels);
    }

    public double getSizeInGridPixels() {
        return getScale().getEndInGridPixels();
    }
}
