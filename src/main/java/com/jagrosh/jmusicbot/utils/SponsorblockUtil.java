package com.jagrosh.jmusicbot.utils;

import de.erdbeerbaerlp.jsponsorblock.Category;
import de.erdbeerbaerlp.jsponsorblock.Segment;

import java.util.ArrayList;

public class SponsorblockUtil {
    public static Segment[] filterSegments(Segment[] segs, Category... categories){
        final ArrayList<Segment> s = new ArrayList<>();
        for (Segment seg : segs) {
            for (Category category : categories) {
                if(category.equals(seg.getCategory())){
                    s.add(seg);
                }
            }
        }
        return s.toArray(new Segment[0]);
    }
}
