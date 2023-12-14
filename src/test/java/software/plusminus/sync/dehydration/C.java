package software.plusminus.sync.dehydration;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import software.plusminus.sync.models.BusinessEntity;

import java.util.List;
import javax.persistence.Entity;
import javax.persistence.OneToMany;

@Data
@EqualsAndHashCode(of = "", callSuper = true)
@ToString(of = "", callSuper = true)
@Entity
public class C extends BusinessEntity {

    private String name;

    @OneToMany(mappedBy = "entityC")
    private List<A> as;

}
