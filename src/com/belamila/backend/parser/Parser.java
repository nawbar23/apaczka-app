package com.belamila.backend.parser;

import com.belamila.model.Package;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.lang.reflect.Field;
import java.nio.charset.Charset;
import java.util.List;

/**
 * Created by: Bartosz Nawrot
 * Date: 18.05.2020
 * Description:
 */
public class Parser {

    private static final Logger logger = LoggerFactory.getLogger(Parser.class);

    private final CsvReader csvReader = new CsvReader();

    public List<Package> parse(File file) throws Exception {
        System.setProperty("file.encoding","UTF-8");
        Field charset = Charset.class.getDeclaredField("defaultCharset");
        charset.setAccessible(true);
        charset.set(null, null);

        logger.info("Parsing {}", file.toString());
        List<Package> packages = csvReader.read(file);

        if (packages.isEmpty()) {
            throw new RuntimeException("Nothing to parse :(");
        }

        return packages;
    }
}
