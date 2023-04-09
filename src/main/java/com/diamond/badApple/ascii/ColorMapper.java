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

  private static final double[] kl = {1.0, 1.0, 1.0};
  private static final double[] kc = {1.0, 1.0, 1.0};
  private static final double[] kh = {1.0, 1.0, 1.0};

  private static double getCie2000Distance(Color c1, Color c2) {
    double[] lab1 = rgbToLab(c1);
    double[] lab2 = rgbToLab(c2);

    double dl = lab2[0] - lab1[0];
    double dc = lab2[1] - lab1[1];
    double dh = lab2[2] - lab1[2];

    double sl = 1.0;
    double sc = 1.0 + 0.045 * lab1[1];
    double sh = 1.0 + 0.015 * lab1[1];

    double dl2 = dl * dl;
    double dc2 = dc * dc;
    double dh2 = dh * dh;

    double firstTerm = dl2 / (kl[0] * sl);
    double secondTerm = dc2 / (kc[0] * sc);
    double thirdTerm = dh2 / (kh[0] * sh);

    return Math.sqrt(firstTerm + secondTerm + thirdTerm);
  }

  private static double[] rgbToLab(Color color) {
    double[] lab = new double[3];
    double[] xyz = rgbToXyz(color);
    double x = xyz[0];
    double y = xyz[1];
    double z = xyz[2];
    double fx, fy, fz;
    if (x > 0.008856) {
      fx = Math.pow(x, 0.333333333333333);
    } else {
      fx = 7.787 * x + 16 / 116.0;
    }
    if (y > 0.008856) {
      fy = Math.pow(y, 0.333333333333333);
    } else {
      fy = 7.787 * y + 16 / 116.0;
    }
    if (z > 0.008856) {
      fz = Math.pow(z, 0.333333333333333);
    } else {
      fz = 7.787 * z + 16 / 116.0;
    }
    lab[0] = 116 * fy - 16;
    lab[1] = 500 * (fx - fy);
    lab[2] = 200 * (fy - fz);
    return lab;
  }

  private static double[] rgbToXyz(Color color) {
    double[] xyz = new double[3];
    double r = color.getRed() / 255.0;
    double g = color.getGreen() / 255.0;
    double b = color.getBlue() / 255.0;
    r = (r > 0.04045) ? Math.pow((r + 0.055) / 1.055, 2.4) : r / 12.92;
    g = (g > 0.04045) ? Math.pow((g + 0.055) / 1.055, 2.4) : g / 12.92;
    b = (b > 0.04045) ? Math.pow((b + 0.055) / 1.055, 2.4) : b / 12.92;
    xyz[0] = (r * 0.4124 + g * 0.3576 + b * 0.1805) / 0.95047;
    xyz[1] = (r * 0.2126 + g * 0.7152 + b * 0.0722) / 1.00000;
    xyz[2] = (r * 0.0193 + g * 0.1192 + b * 0.9505) / 1.08883;
    return xyz;
  }
}
