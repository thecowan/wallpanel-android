package com.jjoe64.motiondetection.motiondetection;

public class State {

    private int[] map = null;
    private int width;
    private int height;
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
