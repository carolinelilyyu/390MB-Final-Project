# -*- coding: utf-8 -*-
"""
Created on Tue Sep 27 13:08:49 2016

@author: cs390mb

This file is used for extracting features over windows of tri-axial accelerometer
data. We recommend using helper functions like _compute_mean_features(window) to
extract individual features.

As a side note, the underscore at the beginning of a function is a Python
convention indicating that the function has private access (although in reality
it is still publicly accessible).

"""

import numpy as np
peaks = 0
threshold = 0
counter = 0
max = 0
min = 15

def _compute_mean_features(window):
    """
    Computes the mean x, y and z acceleration over the given window.
    """
    return np.mean(window, axis=0)

def _compute_Median(window):
    return np.median(np.absolute(window), axis = 0)

def _compute_Local_Min(window):
    return np.amin(window, axis = 0)

def _compute_Local_Max(window):
    return np.amax(window, axis = 0)

def _compute_StandardDev(window):
    return np.std(window, axis = 0)

def _compute_Variance(window):
    return np.var(window, axis = 0)

def _compute_axis_sum(window):
    return np.sum(np.absolute(window), axis = 0)

def _compute_axis_squared(window):
    return np.sum(np.square(window), axis = 0)

def _compute_peaks(window):
    global peaks
    global threshold

    total = _compute_axis_sum(window)/3
    min = _compute_Local_Min(window)[0]
    max = _compute_Local_Max(window)[0]
    if(counter>50)
        difference = max - min
        threshold = max + (difference * 0.5)
        min = 15
        max = 0
        counter = 0
    if(max > threshold and min < threshold):
        peaks = peaks + 1
    counter++
    return peaks

def extract_features(window):
    """
    Here is where you will extract your features from the data over
    the given window. We have given you an example of computing
    the mean and appending it to the feature matrix X.

    Make sure that X is an N x d matrix, where N is the number
    of data points and d is the number of features.

    """
    x = []
    x = np.append(x, np.abs(_compute_mean_features(window)))
    x = np.append(x, _compute_Local_Min(window))
    x = np.append(x, _compute_Local_Max(window))
    x = np.append(x, _compute_Median(window))
    x = np.append(x, _compute_StandardDev(window))
    x = np.append(x, _compute_Variance(window))
    x = np.append(x, np.mean(_compute_axis_squared(window)))
    x = np.append(x, _compute_peaks(window))

    return x
