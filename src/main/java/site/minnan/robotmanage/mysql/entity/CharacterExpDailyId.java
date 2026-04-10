package site.minnan.robotmanage.mysql.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;

@Getter
@Setter
@Embeddable
public class CharacterExpDailyId implements Serializable {

    @Column(name = "character_id")
    private Integer characterId;

    @Column(name = "record_date")
    private LocalDate recordDate;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        CharacterExpDailyId that = (CharacterExpDailyId) o;
        return Objects.equals(characterId, that.characterId) && Objects.equals(recordDate, that.recordDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(characterId, recordDate);
    }
}
