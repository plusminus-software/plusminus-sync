package software.plusminus.sync.dehydration;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import software.plusminus.sync.models.BusinessEntity;

import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.PrimaryKeyJoinColumn;

@Data
@EqualsAndHashCode(of = "", callSuper = true)
@ToString(of = "", callSuper = true)
@Entity
public class B extends BusinessEntity {

    private String name;

    @ManyToOne
    @PrimaryKeyJoinColumn
    private A entityA;

}
