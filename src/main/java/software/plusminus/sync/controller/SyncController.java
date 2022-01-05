package software.plusminus.sync.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.data.domain.Sort;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import software.plusminus.json.model.Classable;
import software.plusminus.sync.dto.Read;
import software.plusminus.sync.dto.Sync;
import software.plusminus.sync.dto.Write;
import software.plusminus.sync.service.SyncService;

import java.util.List;
import javax.validation.Valid;

@SuppressWarnings("squid:S1452")
@Validated
@RestController
@RequestMapping("/sync")
@ConditionalOnBean(SyncService.class)
public class SyncController {

    @Autowired
    private SyncService syncService;

    @GetMapping
    @Validated(Read.class)
    @Valid
    public List<Sync<? extends Classable>> read(@RequestParam List<String> types,
                                                @RequestParam(defaultValue = "null") String ignoreDevice,
                                                @RequestParam(defaultValue = "0") Long offset,
                                                @RequestParam(defaultValue = "20") Integer size,
                                                @RequestParam(defaultValue = "ASC") Sort.Direction direction) {

        return syncService.read(types, ignoreDevice, offset, size, direction);
    }

    @PostMapping
    @Validated(Write.class)
    public <T extends Classable> List<? extends T> write(
            @Valid @RequestBody List<Sync<? extends T>> actions) {
        return syncService.write(actions);
    }
}
