package software.plusminus.sync.models;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.math.BigDecimal;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.Entity;
import javax.persistence.Index;
import javax.persistence.ManyToOne;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.Table;

@Data
@EqualsAndHashCode(of = "", callSuper = true)
@ToString(of = "", callSuper = true)
@Table(indexes = @Index(columnList = "product_id"))
@Entity
@DiscriminatorColumn
public abstract class ProductEntry extends UnimarketEntity {

    @ManyToOne
    @PrimaryKeyJoinColumn
    private Product product;

    private BigDecimal quantity;

    private String unit;

}
