package de.tu.darmstadt.lt.ner.feature.extractor;
/**
 * Copyright (c) 2007-2012, Regents of the University of Colorado
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
 * Neither the name of the University of Colorado at Boulder nor the names of its contributors may be used to endorse or promote products derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

import java.util.Collections;
import java.util.List;

import org.cleartk.ml.Feature;
import org.cleartk.ml.feature.function.FeatureFunction;

/**
 * <br>
 * Copyright (c) 2007-2012, Regents of the University of Colorado <br>
 * All rights reserved.
 *
 *
 * @author Philip Ogren
 *
 */
public class LTCharacterNgramFeatureFunction implements FeatureFunction {

  public enum Orientation {
    RIGHT_TO_LEFT, LEFT_TO_RIGHT
  }

  Orientation orientation;

  int start;

  int end;

  int minimumValueLength;

  boolean lowerCase;

  private String name;

  /**
   * This feature function serves up character n-grams based on StringValued features. For example,
   * if you wanted trigram suffixes (e.g. 'ion' of 'emotion') for words that are of length 7 or more
   * you could call the constructor with the following:
   * CharacterNGramFeatureFunction(Orientation.RIGHT_TO_LEFT, 0, 3, 7, false)
   *
   * @param featureName
   *          a user-specified name for the feature function, to be included in all feature names.
   * @param orientation
   *          must be one of LEFT_TO_RIGHT or RIGHT_TO_LEFT. The orientation determines whether
   *          index 0 corresponds to the first character of the string value or the last. The
   *          orientation does not affect the ordering of the characters in the n-gram which are
   *          always returned in left-to-right order.
   * @param start
   *          the start of the n-gram (typically 0 for both orientations)
   * @param end
   *          the end of the n-gram (typically n for both orientations)
   * @param minimumValueLength
   *          This parameter allows you to skip string values that are too short. It must be greater
   *          than or equal to end.
   * @param lowerCase
   *          if true than the n-gram used as the feature value will be lowercased.
   */
  public LTCharacterNgramFeatureFunction(
      String featureName,
      Orientation orientation,
      int start,
      int end,
      int minimumValueLength,
      boolean lowerCase) {
    name = Feature.createName(
        "NGram",
        orientation == Orientation.RIGHT_TO_LEFT ? "Right" : "Left",
        String.valueOf(start),
        String.valueOf(end),
        String.valueOf(minimumValueLength),
        lowerCase ? "lower" : null,
        featureName);
    if (minimumValueLength < end) {
      throw new IllegalArgumentException(
          "minimumValueLength must be greater than or equal to the parameter end.");
    }
    this.orientation = orientation;
    this.start = start;
    this.end = end;
    this.minimumValueLength = minimumValueLength;
    this.lowerCase = lowerCase;
  }

  public LTCharacterNgramFeatureFunction(
      Orientation orientation,
      int start,
      int end,
      int minimumValueLength,
      boolean lowerCase) {
    this(null, orientation, start, end, minimumValueLength, lowerCase);
  }

  public LTCharacterNgramFeatureFunction(
      String featureName,
      Orientation orientation,
      int start,
      int end) {
    this(featureName, orientation, start, end, end - start, false);
  }

  public LTCharacterNgramFeatureFunction(Orientation orientation, int start, int end) {
    this(null, orientation, start, end);
  }

  /**
   * @return will return an empty list if the value of the feature is not a StringValue or is not as
   *         long as the minimumValueLength.
   */
  @Override
  public List<Feature> apply(Feature feature) {
    String featureName = Feature.createName(name, feature.getName());
    Object featureValue = feature.getValue();
    if (featureValue == null || !(featureValue instanceof String)) {
        return Collections.singletonList(new Feature(featureName, "OUT"));
    }

    String value = featureValue.toString();
    if (value == null || value.length() < minimumValueLength) {
        return Collections.singletonList(new Feature(featureName, "OUT"));
    }

    String ngram;
    if (orientation == Orientation.LEFT_TO_RIGHT) {
      ngram = value.substring(start, end);
    } else {
      ngram = value.substring(value.length() - end, value.length() - start);
    }
    if (lowerCase) {
        ngram = ngram.toLowerCase();
    }

    return Collections.singletonList(new Feature(featureName, ngram));
  }

}
