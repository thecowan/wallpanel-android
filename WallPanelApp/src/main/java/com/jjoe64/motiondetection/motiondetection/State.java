/*
 * Copyright (c) 2018 ThanksMister LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.jjoe64.motiondetection.motiondetection;

public class State {

    private int[] map = null;
    private int width;
    private int height;
    @SuppressWarnings("FieldCanBeLocal")
    private int average;

    public State(int[] data, int width, int height) {
        if (data == null) throw new NullPointerException();

        this.map = data.clone();
        this.width = width;
        this.height = height;

        // build map and stats
        this.average = 0;
        for (int y = 0, xy = 0; y < this.height; y++) {
            for (int x = 0; x < this.width; x++, xy++) {
                this.average += data[xy];
            }
        }
        this.average = (this.average / (this.width * this.height));
    }

    /**
     * Get Map of the State.
     * 
     * @return integer array of the State.
     */
    public int[] getMap() {
        return map;
    }

    /**
     * Get the width of the State.
     * 
     * @return integer representing the width of the state.
     */
    public int getWidth() {
        return width;
    }

    /**
     * Get the height of the State.
     * 
     * @return integer representing the height of the state.
     */
    public int getHeight() {
        return height;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        StringBuilder output = new StringBuilder();
        output.append("h=" + height + " w=" + width + "\n");
        for (int y = 0, xy = 0; y < height; y++) {
            output.append('|');
            for (int x = 0; x < width; x++, xy++) {
                output.append(map[xy]);
                output.append('|');
            }
            output.append("\n");
        }
        return output.toString();
    }
}
