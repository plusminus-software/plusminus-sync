package software.plusminus.sync.service.merger;

import software.plusminus.json.model.ApiObject;
import software.plusminus.sync.dto.Sync;

public interface TypedMerger<T extends ApiObject> extends Merger {

    Class<T> type();

    void merge(T current, Sync<T> sync);

    @Override
    default <O extends ApiObject> boolean supports(Sync<O> sync) {
        return sync.getObject().getClass() == type();
    }
    
    @Override
    default <O extends ApiObject> void process(O current, Sync<O> sync) {
        merge(type().cast(current), (Sync<T>) sync);
    }
    
}
