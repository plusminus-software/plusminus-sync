package software.plusminus.sync.service;

import org.springframework.data.domain.Sort;
import software.plusminus.json.model.ApiObject;
import software.plusminus.sync.dto.Sync;

import java.util.List;

@SuppressWarnings("squid:S1452")
public interface SyncService {

    List<Sync<? extends ApiObject>> read(List<String> types, boolean excludeCurrentDevice,
                                         Long offset, Integer size,
                                         Sort.Direction direction);

    List<? extends ApiObject> write(List<Sync<? extends ApiObject>> actions);

}