package org.dancres.blitz.tools.dash;

import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.FontUIResource;
import javax.swing.plaf.metal.DefaultMetalTheme;

public class BlitzTheme extends DefaultMetalTheme {
    
    public BlitzTheme() {
        
    }
    
    public String getName() {
        return "Blitz";
    }
    
    public FontUIResource getControlTextFont() {
        return controlFont;
    }
    
    public FontUIResource getSystemTextFont() {
        return systemFont;
    }
    
    public FontUIResource getUserTextFont() {
        return userFont;
    }
    
    public FontUIResource getMenuTextFont() {
        return controlFont;
    }
    
    public FontUIResource getWindowTitleFont() {
        return controlFont;
    }
    
    public FontUIResource getSubTextFont() {
        return smallFont;
    }
    
    protected ColorUIResource getPrimary2() {
        return primary2;
    }
    
    protected ColorUIResource getPrimary3() {
        return primary3;
    }
    
    private final FontUIResource controlFont = new FontUIResource("Dialog", 0, 11);
    private final FontUIResource systemFont = new FontUIResource("Dialog", 0, 11);
    private final FontUIResource userFont = new FontUIResource("SansSerif", 0, 11);
    private final FontUIResource smallFont = new FontUIResource("Dialog", 0, 10);
    private final ColorUIResource primary2 = new ColorUIResource(68, 126, 183);
    private final ColorUIResource primary3 = new ColorUIResource(153, 180, 255);
    
    
}
