package com.jjoe64.motiondetection.motiondetection;

public interface IMotionDetection {

    /**
     * Get the previous image in integer array format
     * 
     * @return int array of previous image.
     */
    public int[] getPrevious();

    /**
     * Detect motion.
     * 
     * @param data
     *            integer array representing an image.
     * @param width
     *            Width of the image.
     * @param height
     *            Height of the image.
     * @return boolean True is there is motion.
     * @throws NullPointerException
     *             if data integer array is NULL.
     */
    public boolean detect(int[] data, int width, int height);
}
