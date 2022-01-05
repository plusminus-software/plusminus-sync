package software.plusminus.sync.service;

import org.springframework.data.domain.Sort;
import software.plusminus.json.model.Classable;
import software.plusminus.sync.dto.Sync;

import java.util.List;

@SuppressWarnings("squid:S1452")
public interface SyncService {

    List<Sync<? extends Classable>> read(List<String> types, boolean excludeCurrentDevice,
                                         Long offset, Integer size,
                                         Sort.Direction direction);

    <T extends Classable> List<? extends T> write(List<Sync<? extends T>> actions);

}