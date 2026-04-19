package site.minnan.robotmanage.entity.enumeration;

public enum World {

    KRONOS(Region.NA, 45, "Kronos"),
    HYPERION(Region.NA, 70, "Hyperion"),
    BERA(Region.NA, 1, "Bera"),
    SCANIA(Region.NA, 19, "Scania"),
    LUNA(Region.EU, 30, "Luna"),
    SOLIS(Region.EU, 46, "Solis")
    ;



    private final Region region;

    private final Integer worldId;

    private final String name;

    World(Region region, Integer worldId, String name) {
        this.region = region;
        this.worldId = worldId;
        this.name = name;
    }

    public Region getRegion() {
        return region;
    }

    public Integer getWorldId() {
        return worldId;
    }

    public String getName() {
        return name;
    }

    public String regionName() {
        return region.name();
    }

    public static World getById(Integer id) {
        for (World w : World.values()) {
            if (w.worldId.equals(id)) {
                return w;
            }
        }
        return null;
    }
}
