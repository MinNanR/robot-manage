package site.minnan.robotmanage.service.impl;

import cn.hutool.core.lang.Console;
import cn.hutool.core.map.MapBuilder;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

public class StatsServiceImpl {

    private record Star(int stats, int att) {
    }

    private record Flame(int stat, int att, int all) {
    }

    private record Equipment(int stat, int att, Flame flame, Star star) {

        int indicate() {
            int flameIndicate = flame.stat + flame.att * 3 + flame.all * 9;
            int starIndicate = star.stats + star.att * 3;
            return stat + att * 3 + flameIndicate + starIndicate;
        }
    }

    private final static Map<Integer, Star> eternalStats;

    static {
        eternalStats = MapBuilder.create(new HashMap<Integer, Star>())
                .put(17, new Star(74, 29))
                .put(18, new Star(91, 45))
                .put(19, new Star(102, 68))
                .put(20, new Star(125, 80))
                .put(21, new Star(142, 99))
                .put(22, new Star(159, 120))
                .build();
    }

    static int craSet = 20 + 50 * 3;

    static int eternalSet = 50 + 120 * 3;

    static Star cra22 = new Star(117, 85);

    static Flame standardCraFlame = new Flame(8 * 5 + 4 * 8, 0, 5);

    static Flame standardEternalFlame = new Flame(13 * 5 + 7 * 8, 0, 5);

    private static Equipment craHat(Flame flame) {
        return new Equipment(40, 2, flame, cra22);
    }

    private static Equipment craTop(Flame flame) {
        return new Equipment(30, 2, flame, cra22);
    }

    private static Equipment craBottom(Flame flame) {
        return new Equipment(30, 2, flame, cra22);
    }

    private static Equipment eternalHat(Flame flame, Star star) {
        return new Equipment(80, 10, flame, star);
    }

    private static Equipment eternalTop(Flame flame, Star star) {
        return new Equipment(50, 6, flame, star);
    }

    private static Equipment eternalBottom(Flame flame, Star star) {
        return new Equipment(50, 6, flame, star);
    }

    public static void main(String[] args) {
        //民难
//        Equipment craHat = craHat(new Flame(92, 0, 4));
//        Equipment craTop = craTop(new Flame(84, 0, 4));
//        Equipment craBottom = craBottom(new Flame(80, 0, 5));
        //sakuya
//        Equipment craHat = craHat(new Flame(52, 4, 5));
//        Equipment craTop = craTop(new Flame(68, 3, 5));
//        Equipment craBottom = craBottom(new Flame(48, 6, 5));
        //橘子
//        Equipment craHat = craHat(new Flame(64, 0, 6));
//        Equipment craTop = craTop(new Flame(64, 0, 5));
//        Equipment craBottom = craBottom(new Flame(68, 0, 5));
        Equipment craHat = craHat(new Flame(64, 0, 6));
        Equipment craTop = craTop(new Flame(72, 0, 5));
        Equipment craBottom = craBottom(new Flame(64, 4, 5));

        int craIndicate = Stream.of(craHat, craTop, craBottom).mapToInt(Equipment::indicate).sum() + craSet;


        int hat = 17, top = 17, bottom = 17;
        Flame hatFlame = new Flame(123, 0, 6);
        Equipment eternalHat = eternalHat(hatFlame, eternalStats.get(17));
        Equipment eternalTop = eternalTop(hatFlame, eternalStats.get(17));
        Equipment eternalBottom = eternalBottom(hatFlame, eternalStats.get(17));
        int eternalIndicate = 0;
        while (hat <= 22 && top <= 22 && bottom <= 22) {
            eternalIndicate = Stream.of(eternalHat, eternalTop, eternalBottom).mapToInt(Equipment::indicate).sum() + eternalSet;
            if (eternalIndicate >= craIndicate) {
                break;
            }
            hat++;
            eternalHat = eternalHat(hatFlame, eternalStats.get(hat));
            eternalIndicate = Stream.of(eternalHat, eternalTop, eternalBottom).mapToInt(Equipment::indicate).sum() + eternalSet;
            if (eternalIndicate >= craIndicate) {
                break;
            }
//            hat++;
//            eternalHat = eternalHat(hatFlame, eternalStats.get(hat));
//            eternalIndicate = Stream.of(eternalHat, eternalTop, eternalBottom).mapToInt(Equipment::indicate).sum() + eternalSet;
//            if (eternalIndicate >= craIndicate) {
//                break;
//            }
            top++;
            eternalTop = eternalTop(hatFlame, eternalStats.get(top));
            eternalIndicate = Stream.of(eternalHat, eternalTop, eternalBottom).mapToInt(Equipment::indicate).sum() + eternalSet;
            if (eternalIndicate >= craIndicate) {
                break;
            }
//            top++;
//            eternalTop = eternalTop(hatFlame, eternalStats.get(top));
//            eternalIndicate = Stream.of(eternalHat, eternalTop, eternalBottom).mapToInt(Equipment::indicate).sum() + eternalSet;
//            if (eternalIndicate >= craIndicate) {
//                break;
//            }
            bottom++;
            eternalBottom = eternalBottom(hatFlame, eternalStats.get(bottom));
            eternalIndicate = Stream.of(eternalHat, eternalTop, eternalBottom).mapToInt(Equipment::indicate).sum() + eternalSet;
            if (eternalIndicate >= craIndicate) {
                break;
            }
//            bottom++;
//            eternalBottom = eternalBottom(standardEternalFlame, eternalStats.get(bottom));
//            eternalIndicate = Stream.of(eternalHat, eternalTop, eternalBottom).mapToInt(Equipment::indicate).sum() + eternalSet;
//            if (eternalIndicate >= craIndicate) {
//                break;
//            }
        }


//        Equipment eternalHat = eternalHat(standardEternalFlame, eternalStats.get(22));
//        Equipment eternalTop = eternalTop(standardEternalFlame, eternalStats.get(17));
//        Equipment eternalBottom = eternalBottom(standardEternalFlame, eternalStats.get(17));
//        int eternalIndicate = Stream.of(eternalHat, eternalTop, eternalBottom).mapToInt(Equipment::indicate).sum() + eternalSet;

        Console.log("cra" , craIndicate);
        Console.log(hat, top, bottom);
        Console.log("eternal" , eternalIndicate);
    }
}
