/*
 *  Licensed to GraphHopper GmbH under one or more contributor
 *  license agreements. See the NOTICE file distributed with this work for
 *  additional information regarding copyright ownership.
 *
 *  GraphHopper GmbH licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except in
 *  compliance with the License. You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.graphhopper.routing.util.parsers;

import com.graphhopper.reader.ReaderWay;
import com.graphhopper.routing.profiles.DecimalEncodedValue;
import com.graphhopper.routing.profiles.EncodedValue;
import com.graphhopper.routing.profiles.EncodedValueLookup;
import com.graphhopper.routing.profiles.MaxWeight;
import com.graphhopper.routing.util.EncodingManager;
import com.graphhopper.routing.util.parsers.helpers.OSMValueExtractor;
import com.graphhopper.storage.IntsRef;

import java.util.Arrays;
import java.util.List;

public class OSMMaxWeightParser implements TagParser {

    private DecimalEncodedValue weightEncoder;
    private boolean enableLog;

    public OSMMaxWeightParser() {
        this(MaxWeight.create(), false);
    }

    public OSMMaxWeightParser(DecimalEncodedValue weightEncoder, boolean enableLog) {
        this.weightEncoder = weightEncoder;
        this.enableLog = enableLog;
    }

    @Override
    public void createEncodedValues(EncodedValueLookup lookup, List<EncodedValue> registerNewEncodedValue) {
        registerNewEncodedValue.add(weightEncoder);
    }

    @Override
    public IntsRef handleWayTags(IntsRef edgeFlags, ReaderWay way, EncodingManager.Access access, long relationFlags) {
        // do not include OSM tag "height" here as it has completely different meaning (height of peak)
        List<String> weightTags = Arrays.asList("maxweight", "maxgcweight");
        OSMValueExtractor.extractTons(edgeFlags, way, weightEncoder, weightTags, enableLog);
        return edgeFlags;
    }
}
