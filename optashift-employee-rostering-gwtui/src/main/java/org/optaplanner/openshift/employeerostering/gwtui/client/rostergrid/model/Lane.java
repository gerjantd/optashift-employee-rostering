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

public class Lane<T> {

    private final String title;

    private final List<SubLane<T>> subLanes;

    public Lane(final String title, final List<SubLane<T>> subLanes) {
        this.title = title;
        this.subLanes = subLanes;
    }

    public List<SubLane<T>> getSubLanes() {
        return subLanes;
    }

    public String getTitle() {
        return title;
    }

    public SubLane<T> getNextSubLaneWithSpaceForBlobsStartingFrom(final SubLane<T> subLane,
                                                                  final List<Blob<T>> newBlobs) {

        return getSubLanes().subList(getSubLanes().indexOf(subLane), getSubLanes().size())
                .stream()
                .filter(s -> !s.anyCollide(newBlobs))
                .findFirst().orElseGet(SubLane::new);
    }
}
