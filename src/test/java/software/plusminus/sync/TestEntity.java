package software.plusminus.sync;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;
import software.plusminus.audit.annotation.Auditable;
import software.plusminus.json.model.ApiObject;
import software.plusminus.sync.annotation.Syncable;
import software.plusminus.tenant.annotation.Tenant;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.Version;

@Data
@Auditable
@Syncable
@Entity
@FilterDef(name = "tenantFilter", parameters = @ParamDef(name = "tenant", type = "string"))
@Filter(name = "tenantFilter", condition = "tenant = :tenant")
@FilterDef(name = "deletedFilter")
@Filter(name = "deletedFilter", condition = "deleted = false")
public class TestEntity implements ApiObject {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String myField;
    @Version
    private Long version;
    @Tenant
    private String tenant;
    @JsonInclude(JsonInclude.Include.NON_DEFAULT)
    private Boolean deleted = Boolean.FALSE;
    @OneToOne
    @PrimaryKeyJoinColumn
    private InnerEntity innerEntity;

}