package site.minnan.robotmanage.entity.enumeration;

public enum Region {

    NA("na"),
    EU("eu")
    ;

    private final String region;

    Region(String region) {
        this.region = region;
    }

    public String getRegion() {
        return region;
    }
}
