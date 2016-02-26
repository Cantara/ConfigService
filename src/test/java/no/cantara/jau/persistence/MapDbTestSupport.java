package no.cantara.jau.persistence;

import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Map;

public class MapDbTestSupport {

    private static final Logger log = LoggerFactory.getLogger(MapDbTestSupport.class);

    public static void cleanAllData() throws Exception {
        File mapDbPathFile = new File("./db/serviceConfig.db");
        log.debug("Cleaning data in MapDB {}", mapDbPathFile.getAbsolutePath());
        mapDbPathFile.getParentFile().mkdirs();
        DB db = DBMaker.newFileDB(mapDbPathFile).make();

        db.getAll().entrySet().stream()
                .filter(e -> e.getValue() instanceof Map)
                .map(e -> (Map) e.getValue())
                .forEach(Map::clear);
        db.commit();
        db.close();
    }

}
