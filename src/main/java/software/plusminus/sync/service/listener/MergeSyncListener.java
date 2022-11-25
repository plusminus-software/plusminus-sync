package software.plusminus.sync.service.listener;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.plusminus.data.repository.DataRepository;
import software.plusminus.json.model.ApiObject;
import software.plusminus.sync.dto.Sync;
import software.plusminus.sync.dto.SyncType;
import software.plusminus.sync.service.fetcher.Fetcher;
import software.plusminus.sync.service.fetcher.SyncTransactionService;
import software.plusminus.sync.service.merger.Merger;
import software.plusminus.util.EntityUtils;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@ConditionalOnProperty(value = "plusminus.sync.merge", matchIfMissing = true)
@Component
public class MergeSyncListener implements SyncListener {

    @Autowired
    private List<Merger> mergers;
    @Autowired
    private List<Fetcher> fetchers;
    @Autowired
    private SyncTransactionService transactionService;
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

        if (sync.getType() == SyncType.CREATE) {
            for (Fetcher fetcher : fetchers) {
                Optional<T> fetched = transactionService.newTransaction(() -> fetcher.fetch(sync));
                if (fetched.isPresent()) {
                    T object = fetcher.fetch(sync)
                            .orElseThrow(() -> new IllegalStateException("Entity is not available to fetch"));
                    sync.setObject(object);
                    sync.setType(SyncType.TURN_BACK);
                    return;
                }
            }
            return;
        }
        Class<T> type = (Class<T>) sync.getObject().getClass();
        T current = repository.findById(type, EntityUtils.findId(sync.getObject()));
        foundMergers.forEach(m -> m.process(current, sync));
    }
}
