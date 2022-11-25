package software.plusminus.sync.models;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.util.List;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.Index;
import javax.persistence.OneToMany;
import javax.persistence.Table;

@Data
@EqualsAndHashCode(of = "", callSuper = true)
@ToString(of = "", callSuper = true)
@EntityListeners(AuditingEntityListener.class)
@Table(indexes = {
        @Index(columnList = "deleted, tenant")
})
@Entity
public class Product extends UnimarketEntity {

    private String name;

    private String code;
    
    @OneToMany(mappedBy = "product")
    private List<ProductEntry> entries;

}
