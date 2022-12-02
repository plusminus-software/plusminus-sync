package software.plusminus.sync.service.fetcher;

import org.springframework.stereotype.Component;
import software.plusminus.json.model.ApiObject;
import software.plusminus.sync.annotation.Uuid;
import software.plusminus.util.FieldUtils;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.ParameterExpression;
import javax.persistence.criteria.Root;

@Component
public class ByUuidFinder implements Finder {
    
    @PersistenceContext
    private EntityManager entityManager;
    
    @Override
    public <T extends ApiObject> Optional<T> find(T object) {
        Class<T> type = (Class<T>) object.getClass();
        Optional<Field> uuidField = FieldUtils.findFirstWithAnnotation(type, Uuid.class);
        if (!uuidField.isPresent()) {
            return Optional.empty();
        }
        Object uuid = FieldUtils.read(object, uuidField.get());
        
        TypedQuery<T> query = buildQuery(type, uuidField.get());
        query.setParameter(uuidField.get().getName(), uuid);
        List<T> fetched =  query.getResultList();
        if (fetched.size() != 1) {
            return Optional.empty();
        }
        return Optional.of(fetched.get(0));
    }
    
    private <T extends ApiObject> TypedQuery<T> buildQuery(Class<T> type, Field uuidField) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        
        CriteriaQuery<T> criteriaQuery = cb.createQuery(type);
        Root<T> root = criteriaQuery.from(type);
        ParameterExpression<?> uuidParameter = cb.parameter(
                uuidField.getType(), uuidField.getName());
        criteriaQuery.select(root)
                .where(cb.equal(root.get(uuidField.getName()), uuidParameter));
        return entityManager.createQuery(criteriaQuery);
    }
    
}
