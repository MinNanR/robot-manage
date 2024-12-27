package site.minnan.robotmanage.entity.enumeration;

/**
 * 权限码枚举类
 *
 * @author Minnan on 2024/12/16
 */
public enum AuthEnum implements Authentic {

    ADD_QUESTION(1),
    QUERY_QUESTION(1 << 1),
    FUZZY_QUERY_QUESTION(1 << 2),
    DELETE_QUESTION(1 << 3),
    DELETE_ANSWER(1 << 4),
    COPY_BOSS(1 << 5),
    AVAILABLE(1 << 6) {
        @Override
        public boolean isAuthorized(int authNumber) {
            return ((((authNumber >> 6) & 1) ^ 1)) != 0;
        }
    },
    TRIGGER_MAINTAIN_DETECT(1 << 7),
    AUTH_OPERATE(1 << 8)
    ;

    protected int authorizedNumber;


    AuthEnum(Integer authorizedNumber) {
        this.authorizedNumber = authorizedNumber;
    }


    @Override
    public boolean isAuthorized(int authNumber) {
        return (this.authorizedNumber & authNumber) != 0;
    }
}
