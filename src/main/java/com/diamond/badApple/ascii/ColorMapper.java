package com.diamond.badApple.ascii;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import lombok.experimental.UtilityClass;

@UtilityClass
public class ColorMapper {

  static {
    List<Color> colors = new ArrayList<>();

    int rgb = 255;
    int step = 60;
    int rounds = rgb / step;

    for (int i = 0; i < rounds; i++) {
      for (int j = 0; j < rounds; j++) {
        for (int k = 0; k < rounds; k++) {
          colors.add(new Color(i * step, j * step, k * step));
        }
      }
    }

    ColorMapper.colors = colors.toArray(new Color[0]);
  }

  private static Color[] colors;

  public static Color getNearestConstant(Color color) {
    Color nearest = null;
    double minDistance = Double.MAX_VALUE;

    for (Color c : colors) {
      double distance = getDistanceBetweenColors(c, color);
      if (distance < minDistance) {
        minDistance = distance;
        nearest = c;
      }
    }

    return nearest;
  }

  private static double getDistanceBetweenColors(Color c1, Color c2) {
    double rDif = c1.getRed() - c2.getRed();
    double gDif = c1.getGreen() - c2.getGreen();
    double bDif = c1.getBlue() - c2.getBlue();

    double rSquare = Math.pow(rDif, 2);
    double gSquare = Math.pow(gDif, 2);
    double bSquare = Math.pow(bDif, 2);

    return Math.sqrt(rSquare + gSquare + bSquare);
  }
}
