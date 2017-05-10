package md2k.mcerebrum.cstress.features;

/*
 * Copyright (c) 2015, The University of Memphis, MD2K Center 
 * - Timothy Hnat <twhnat@memphis.edu>
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 * 
 * * Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

import md2k.mcerebrum.cstress.StreamConstants;
import md2k.mcerebrum.cstress.autosense.AUTOSENSE;
import md2k.mcerebrum.cstress.library.Time;
import md2k.mcerebrum.cstress.library.dataquality.autosense.ECGQualityCalculation;
import md2k.mcerebrum.cstress.library.datastream.DataPointStream;
import md2k.mcerebrum.cstress.library.datastream.DataStreams;
import md2k.mcerebrum.cstress.library.structs.DataPoint;

import java.util.ArrayList;
import java.util.List;

/**
 * Wrapper class for ECGQualityCalculation
 */
public class ECGDataQuality {

    /**
     * Constructor
     * @param datastreams Global datastream object
     * @param qualityThreshold Input quality threshold
     */
    public ECGDataQuality(DataStreams datastreams, double qualityThreshold) {
        DataPointStream ecg = datastreams.getDataPointStream(StreamConstants.ORG_MD2K_CSTRESS_DATA_ECG);
        DataPointStream ecgQuality = datastreams.getDataPointStream(StreamConstants.ORG_MD2K_CSTRESS_DATA_ECG_QUALITY);

        ECGQualityCalculation ecgComputation = new ECGQualityCalculation(3, 50, 4500, 20, 2, 47);

        envelBuff = new int[bufferLength];
        classBuff = new int[bufferLength];
        for (int i = 0; i < bufferLength; i++) {
            envelBuff[i] = 2 * ecgThresholdBandLoose;
            classBuff[i] = 0;
        }
        envelHead = 0;
        classHead = 0;

        List<DataPoint> quality = computeQuality(ecg.data, 5000); //0.67

        double count = 0;
        for (DataPoint dp : quality) {
            ecgQuality.add(dp);
            if (dp.value == AUTOSENSE.QUALITY_GOOD) {
                count++;
            }
        }

        DataPointStream ecgWindowQuality = datastreams.getDataPointStream(StreamConstants.ORG_MD2K_CSTRESS_DATA_ECG_WINDOW_QUALITY);

        if ((count / quality.size()) > qualityThreshold)
            ecgWindowQuality.add(new DataPoint(quality.get(0).timestamp, AUTOSENSE.QUALITY_GOOD));
        else
            ecgWindowQuality.add(new DataPoint(quality.get(0).timestamp, AUTOSENSE.QUALITY_BAD));


    }

    private List<DataPoint> computeQuality(List<DataPoint> ecg, long windowSize) {

        List<DataPoint[]> windowedECG = Time.window(ecg, windowSize);
        List<DataPoint> result = new ArrayList<DataPoint>();

        for (DataPoint[] dpA : windowedECG) {
            int[] data = new int[dpA.length];
            int i = 0;
            for (DataPoint s : dpA) {
                data[i++] = (int) s.value;
            }
            if (data.length > 0) {
                result.add(new DataPoint(dpA[0].timestamp, currentQuality(data)));
            }
        }


        return result;

    }
    private int currentQuality(int[] data) {
        classifyDataPoints(data);
        classifySegment(data);

        classBuff[(classHead++) % classBuff.length] = segment_class;
        envelBuff[(envelHead++) % envelBuff.length] = max_value - min_value;
        classifyBuffer();

        if (segment_class = AUTOSENSE.SEGMENT_BAD) {
            return AUTOSENSE.QUALITY_BAND_OFF;
        } else if (2 * amplitude_small > envelBuff.length) {
            return AUTOSENSE.QUALITY_BAND_LOOSE;
        }else if(max_value - min_value <= ecgThresholdBandLoose){
            return AUTOSENSE.QUALITY_BAND_LOOSE;;
        return AUTOSENSE.QUALITY_GOOD;
    }
}
