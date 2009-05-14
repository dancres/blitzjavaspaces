package org.dancres.blitz.tools.dash;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;

import javax.swing.JPanel;

public class PieChart extends JPanel {
    String title;
    Font font;
    FontMetrics fontMetrics;
    int titleHeight = 15;
    int columns;
    int values[];
    Color colors[];
    String labels[];
    float percent[];
    float angle[];
    int maxLabelWidth = 0;
    int maxValueWidth = 0;
    int max = 0;
    int strWidth = 0;
    boolean showLabel = true;
    boolean showPercent = true;
    int lx = 0, ly = 0;
    int cx = 0, cy = 0;
    public PieChart() {
        font = new java.awt.Font("Sanserif", Font.BOLD, 12);
        fontMetrics = getFontMetrics(font);
        setBackground(Color.white);
        title = "JavaSpace ops";
        columns = 3;
        showLabel = true;
        showPercent = true;
        values = new int[]{0, 0, 0};
        colors = new Color[]{ColorScheme.TAKE,ColorScheme.WRITE,
                             ColorScheme.READ};
        labels = new String[]{"Take", "Write", "Read"};
        percent = new float[columns];
        angle = new float[columns];
        calcValues();
    }
    private void calcValues() {
        float totalValue = 0;
        for (int i = 0; i < columns; i++) {
            totalValue += values[i];
            if (values[i] > max) {
                max = values[i];
            }
            maxLabelWidth = Math.max(fontMetrics
                                     .stringWidth((labels[i])),
                                     maxLabelWidth);
        }
        float multiFactor = 100 / totalValue;
        for (int i = 0; i < columns; i++) {
            percent[i] = values[i] * multiFactor;
            angle[i] = (float) (percent[i] * 3.6);
        }
    }
    void update(int take, int write, int read) {
        values = new int[]{take, write, read};
        calcValues();
        repaint();
    }
    public synchronized void paint(Graphics g) {
        Dimension dim = getSize();
        g.setColor(Color.white);
        g.fillRect(0, 0, dim.width, dim.height);
        int x = 0;
        int y = 0;
        int width = 0, height = 0;
        int ax = 0, ay = 0;
        int px = 0, py = 0;
        int radius = 0;
        width = height = Math.min((getSize().width - 100),
                                  (getSize().height - 100));
        x = y = 50;
        if (getSize().width > width) {
            x = (getSize().width - width) / 2;
        }
        cx = x + width / 2;
        cy = y + height / 2;
        radius = width / 2;
        strWidth = fontMetrics.stringWidth(title);
        Font fnt = new java.awt.Font("Sanserif", Font.BOLD, 16);
        g.setFont(fnt);
        g.setColor(Color.red);
        g.setFont(font);
        int initAngle = 90;
        int sweepAngle = 0;
        int incSweepAngle = 0;
        int incLabelAngle = (int) (angle[0] / 2);
        for (int i = 0; i < columns; i++) {
            sweepAngle = Math.round(angle[i]);
            g.setColor(colors[i]);
            if (i == (columns - 1)) {
                sweepAngle = 360 - incSweepAngle;
                g.fillArc(x, y, width, height, initAngle, (-sweepAngle));
                g.setColor(Color.black);
                g.drawArc(x, y, width, height, initAngle, (-sweepAngle));
                if (showLabel) {
                    lx = (int) (cx + (radius * Math
                                      .cos((incLabelAngle * 3.14f / 180) - 3.14f / 2)));
                    ly = (int) (cy + (radius * Math
                                      .sin((incLabelAngle * 3.14f / 180) - 3.14f / 2)));
                    adjustLabel(i);
                    g.drawString(labels[i], lx, ly);
                }
                if (showPercent) {
                    px = (int) (cx + ((radius * 2.0f / 3) * Math
                                      .cos((incLabelAngle * 3.14f / 180) - 3.14f / 2)));
                    py = (int) (cy + ((radius * 2 / 3) * Math
                                      .sin((incLabelAngle * 3.14f / 180) - 3.14f / 2)));
                    g.drawString(String.valueOf(Math.round(percent[i])) + "%",
                                 px, py);
                }
                break;
            }
            g.fillArc(x, y, width, height, initAngle, (-sweepAngle));
            g.setColor(Color.black);
            g.drawArc(x, y, width, height, initAngle, (-sweepAngle));
            incSweepAngle += sweepAngle;
            ax = (int) (cx + (radius * Math
                              .cos((incSweepAngle * 3.14f / 180) - 3.14f / 2)));
            ay = (int) (cy + (radius * Math
                              .sin((incSweepAngle * 3.14f / 180) - 3.14f / 2)));
            g.drawLine(cx, cy, ax, ay);
            if (showLabel) {
                lx = (int) (cx + (radius * Math
                                  .cos((incLabelAngle * 3.14f / 180) - 3.14f / 2)));
                ly = (int) (cy + (radius * Math
                                  .sin((incLabelAngle * 3.14f / 180) - 3.14f / 2)));
                adjustLabel(i);
                g.drawString(labels[i], lx, ly);
            }
            if (showPercent) {
                px = (int) (cx + ((radius * 2 / 3) * Math
                                  .cos((incLabelAngle * 3.14f / 180) - 3.14f / 2)));
                py = (int) (cy + ((radius * 2 / 3) * Math
                                  .sin((incLabelAngle * 3.14f / 180) - 3.14f / 2)));
                strWidth = fontMetrics
                    .stringWidth(Math.round(percent[i]) + "%");
                g.drawString(String.valueOf(Math.round(percent[i])) + "%",
                             (px - strWidth / 2), py);
            }
            incLabelAngle = incLabelAngle
                + (int) (angle[i] / 2 + angle[i + 1] / 2);
            initAngle += (-sweepAngle);
        }
        g.setColor(Color.black);
        g.drawLine(cx, cy, cx, cy - radius);
    }
    private void adjustLabel(int i) {
        if ((lx > cx) && (ly < cy)) {
            lx += 5;
            ly -= 5;
        }
        if ((lx > cx) && (ly > cy)) {
            lx += 5;
            ly += 10;
        }
        if ((lx < cx) && (ly > cy)) {
            strWidth = fontMetrics.stringWidth(labels[i]);
            lx -= strWidth + 5;
            if (lx < 0)
                lx = 0;
        }
        if ((lx < cx) && (ly < cy)) {
            strWidth = fontMetrics.stringWidth(labels[i]);
            lx -= strWidth + 5;
            if (lx < 0)
                lx = 0;
        }
    }
}
