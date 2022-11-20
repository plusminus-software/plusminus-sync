package software.plusminus.sync.service.listener;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import software.plusminus.data.repository.DataRepository;
import software.plusminus.json.model.ApiObject;
import software.plusminus.sync.dto.Sync;
import software.plusminus.sync.dto.SyncType;
import software.plusminus.sync.service.fetcher.Fetcher;
import software.plusminus.sync.service.merger.Merger;
import software.plusminus.util.EntityUtils;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class MergeSyncListener implements SyncListener {

    @Autowired
    private List<Merger> mergers;
    @Autowired
    private List<Fetcher> fetchers;
    @Autowired
    private DataRepository repository;

    @Override
    public <T extends ApiObject> void onWrite(Sync<T> sync) {
        List<Merger> foundMergers = mergers.stream()
                .filter(m -> m.supports(sync))
                .collect(Collectors.toList());
        if (foundMergers.isEmpty()) {
            return;
        }

        T current;
        if (sync.getType() == SyncType.CREATE) {
            Optional<T> fetched = fetchers.stream()
                    .map(f -> f.fetch(sync))
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .findFirst();
            if (!fetched.isPresent()) {
                return;
            }
            current = fetched.get();
        } else {
            Class<T> type = (Class<T>) sync.getObject().getClass();
            current = repository.findById(type, EntityUtils.findId(sync.getObject()));
        }
        foundMergers.forEach(m -> m.process(current, sync));
    }
}
