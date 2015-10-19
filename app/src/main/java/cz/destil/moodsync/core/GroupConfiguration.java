package cz.destil.moodsync.core;

import java.util.ArrayList;
import java.util.List;

public class GroupConfiguration
{
    public final static ArrayList<String> USEALLLAMPS = new ArrayList<String>();
    private ScreenCapturePlacements screenCapturePlacement = null;

    private String name = "";
    private float brightness = -1;
    private List<String> lamps = null;
    private int previousColor = -1;


    public List<String> getLamps()
    {
        if(lamps == null || lamps.size() == 0)
        {
            return USEALLLAMPS;
        }
        else
            return lamps;

    }

    public String getName()
    {
        if(name.isEmpty())
        {
            return "Unknown";
        }
        else
        {
            return name;
        }

    }

    public float getBrightness()
    {
        if(brightness < 0)
        {
            return Config.LIFX_BRIGHTNESS_Default;
        }
        else
        {
            return brightness;
        }
    }

    public ScreenCapturePlacements getScreenCapturePlacement()
    {
        if(screenCapturePlacement == null)
        {
            return ScreenCapturePlacements.ALLOFTHESCREEN;
        }
        else
        {
            return screenCapturePlacement;
        }
    }

    public GroupConfiguration(String name, float lifxBrightness, ScreenCapturePlacements screenCapturePlacement, ArrayList<String> lamps)
    {
        this.brightness = lifxBrightness;
        this.screenCapturePlacement = screenCapturePlacement;
        this.lamps = lamps;
        this.name = name;
    }

    public int getPreviousColor() {
        return previousColor;
    }

    public void setPreviousColor(int previousColor) {
        this.previousColor = previousColor;
    }
}
