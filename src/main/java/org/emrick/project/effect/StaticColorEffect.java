package org.emrick.project.effect;

import org.emrick.project.Performer;
import org.emrick.project.actions.EffectPerformerMap;

import java.awt.*;
import java.time.Duration;
import java.util.ArrayList;

public class StaticColorEffect implements GeneratedEffect {

    private long startTime;
    private long endTime;
    private Color staticColor;
    private Duration duration;
    private int id;

    public StaticColorEffect(long startTime, long endTime, Color staticColor, Duration duration, int id) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.staticColor = staticColor;
        this.duration = duration;
        this.id = id;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    public Color getStaticColor() {
        return staticColor;
    }

    public void setStaticColor(Color staticColor) {
        this.staticColor = staticColor;
    }


    public Duration getDuration() {
        return duration;
    }

    public void setDuration(Duration duration) {
        this.duration = duration;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @Override
    public EffectList getEffectType() {
        return EffectList.STATIC_COLOR;
    }

    @Override
    public Effect generateEffectObj() {
        Effect effect = new Effect(this.getStartTime());
        effect.setEndTimeMSec(this.getEndTime());
        effect.setStartColor(this.getStaticColor());
        effect.setDelay(this.getDuration());
        effect.setDO_DELAY(true);
        effect.setTIME_GRADIENT(false);
        effect.setEffectType(EffectList.STATIC_COLOR);
        effect.setId(this.getId());
        return effect;
    }

    @Override
    public ArrayList<EffectPerformerMap> generateEffects(ArrayList<Performer> performers, Effect effect) {
        ArrayList<EffectPerformerMap> map = new ArrayList<>();
        for (Performer p : performers) {
            map.add(new EffectPerformerMap(effect, p));
        }
        return map;
    }
}
