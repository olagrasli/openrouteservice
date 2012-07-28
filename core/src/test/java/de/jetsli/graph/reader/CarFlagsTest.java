/*
 *  Copyright 2012 Peter Karich info@jetsli.de
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package de.jetsli.graph.reader;

import java.util.HashMap;
import java.util.Map;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Peter Karich
 */
public class CarFlagsTest {

    @Test
    public void testBasics() {
        CarFlags fl = new CarFlags(CarFlags.create(true));
        assertTrue(fl.isForward());
        assertTrue(fl.isBackward());

        fl = new CarFlags(CarFlags.create(false));
        assertTrue(fl.isForward());
        assertFalse(fl.isBackward());
        assertEquals(CarFlags.DEFAULT_SPEED, fl.getSpeedPart());
    }

    @Test
    public void testSwapDir() {
        CarFlags fl = new CarFlags(CarFlags.swapDirection(CarFlags.create(true)));
        assertTrue(fl.isForward());
        assertTrue(fl.isBackward());
        assertEquals(CarFlags.DEFAULT_SPEED, fl.getSpeedPart());
        
        fl = new CarFlags(CarFlags.swapDirection(CarFlags.create(false)));
        assertFalse(fl.isForward());
        assertTrue(fl.isBackward());
        assertEquals(CarFlags.DEFAULT_SPEED, fl.getSpeedPart());
    }
    
    @Test
    public void testService() {
        Map<String, Object> p = new HashMap<String, Object>();
        p.put("car", CarFlags.CAR_SPEED.get("service"));
        CarFlags fl = new CarFlags(CarFlags.create(p));
        assertTrue(fl.isForward());
        assertTrue(fl.isBackward());
        assertTrue(fl.isService());
    }
}