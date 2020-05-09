package net.dirtydeeds.discordsoundboard.util;

import net.dirtydeeds.discordsoundboard.beans.Clip;

import java.util.Comparator;

/**
 * Class used to sort SoundFile Object
 *
 * Created by Dave on 4/2/2016.
 */
public class SortIgnoreCase implements Comparator<Clip> {
    public int compare(Clip o1, Clip o2) {
        String s1 = o1.getClipId();
        String s2 = o2.getClipId();
        return s1.toLowerCase().compareTo(s2.toLowerCase());
    }
}
